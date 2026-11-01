import os
import sys
import pytest
from decimal import Decimal
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

current_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from app.database import Base
from app.models.user import User, UserRole
from app.models.job import Job, JobStatus
from app.models.resume import Resume, ResumeAnalysis
from app.schemas.auth import UserRegister
from app.schemas.job import JobCreate
from app.schemas.ats import AtsMatchRequest
from app.routers.auth import register
from app.routers.jobs import create_job
from app.routers.ats import analyze_general, analyze_job_match, get_ats_history, delete_ats_report
from app.services.ats_engine import (
    extract_skills,
    extract_experience_years,
    extract_education,
    analyze_general_resume,
    analyze_compatibility
)

TEST_DB_FILE = "./test_hiregrid_ats.db"
SQLALCHEMY_DATABASE_URL = f"sqlite:///{TEST_DB_FILE}"

engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)
    if os.path.exists(TEST_DB_FILE):
        try:
            os.remove(TEST_DB_FILE)
        except Exception:
            pass

@pytest.fixture
def db():
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()

SAMPLE_RESUME_TEXT = """
Alex River
Senior Android Engineer
Email: alex.river@example.com | Phone: +1 555-0199 | Remote

Summary:
Innovative Software Engineer with 5+ years of experience specializing in native Android application development.
Skilled in Kotlin, Jetpack Compose, Coroutines, StateFlow, MVVM, Room Database, and Retrofit.
Passionate about building responsive, high-performance mobile architectures.

Experience:
Senior Android Developer — MobileCraft Ltd (2021 - Present)
- Architected and delivered 3 mission-critical Android apps using Jetpack Compose and Clean Architecture.
- Integrated REST APIs with Retrofit and managed persistent offline storage using Room database.
- Implemented state management using Kotlin StateFlow and Coroutines, reducing app crashes by 45%.
- Mentored junior engineers and led code reviews following SOLID principles.

Android Engineer — AppNova Inc (2019 - 2021)
- Built native features in Kotlin and Java with MVVM pattern.
- Wrote extensive Unit Testing suites using JUnit and Espresso.
- Automated release workflows using Git, GitHub Actions, and CI/CD.

Education:
Bachelor of Science in Computer Science — State University (2015 - 2019)

Skills:
Kotlin, Java, Jetpack Compose, Android SDK, Coroutines, StateFlow, Retrofit, Room, MVVM, Git, Docker, REST APIs, CI/CD, Agile, Teamwork.
"""

def test_nlp_skill_extraction():
    skills = extract_skills(SAMPLE_RESUME_TEXT)
    assert "Kotlin" in skills
    assert "Jetpack Compose" in skills
    assert "Coroutines" in skills
    assert "StateFlow" in skills
    assert "Room" in skills
    assert "Retrofit" in skills
    assert "MVVM" in skills
    assert "Git" in skills
    assert "Agile" in skills
    assert len(skills) >= 10

def test_nlp_experience_extraction():
    exp = extract_experience_years(SAMPLE_RESUME_TEXT)
    assert exp == 5.0

def test_nlp_education_extraction():
    edu = extract_education(SAMPLE_RESUME_TEXT)
    assert "Bachelor's Degree" in edu

def test_general_resume_analysis_engine():
    res = analyze_general_resume(SAMPLE_RESUME_TEXT)
    assert res["overall_score"] >= 80.0
    assert len(res["extracted_skills"]) >= 10
    assert len(res["strengths"]) >= 3
    assert res["extracted_experience_years"] == 5.0

def test_compatibility_matching_engine():
    job_desc = """
    We are seeking a Senior Android Developer with 4+ years of experience.
    Must have strong hands-on experience in Kotlin, Jetpack Compose, MVVM, Room, and GraphQL.
    """
    match_res = analyze_compatibility(
        resume_text=SAMPLE_RESUME_TEXT,
        job_description=job_desc,
        job_required_skills=["Kotlin", "Jetpack Compose", "MVVM", "Room", "GraphQL"],
        min_experience_years=4
    )
    assert match_res["overall_score"] >= 75.0
    assert "Kotlin" in match_res["matched_skills"]
    assert "Jetpack Compose" in match_res["matched_skills"]
    assert "Room" in match_res["matched_skills"]
    assert "GraphQL" in match_res["missing_skills"]
    assert match_res["experience_score"] == 100.0  # 5 yrs candidate vs 4 yrs required

def test_ats_endpoints_and_history(db):
    # 1. Create candidate user
    cand_in = UserRegister(
        email="ats.candidate@example.com",
        password="Password123!",
        role="candidate",
        full_name="Ats Candidate"
    )
    register(cand_in, db)
    user = db.query(User).filter(User.email == "ats.candidate@example.com").first()

    # 2. Add Resume record
    resume = Resume(
        candidate_id=user.candidate_profile.id,
        file_name="Alex_River_Resume.pdf",
        file_path="/uploads/test_resume.pdf",
        file_mime_type="application/pdf",
        file_size_bytes=102400,
        raw_extracted_text=SAMPLE_RESUME_TEXT
    )
    db.add(resume)
    db.commit()
    db.refresh(resume)

    # 3. Call analyze_general endpoint
    general_report = analyze_general(resume.id, user, db)
    assert general_report.id is not None
    assert general_report.overall_score >= 80
    assert "Kotlin" in general_report.extracted_skills

    # 4. Call analyze_job_match endpoint
    match_req = AtsMatchRequest(
        resume_id=resume.id,
        job_title="Android Tech Lead",
        job_description="Seeking Android Developer with Kotlin, Flutter, and Docker.",
        min_experience_years=3
    )
    match_report = analyze_job_match(match_req, user, db)
    assert match_report.id is not None
    assert match_report.overall_score > 50
    assert "Kotlin" in match_report.matched_skills

    # 5. Retrieve history
    history = get_ats_history(user, db)
    assert history.total >= 2

    # 6. Delete a report
    del_res = delete_ats_report(general_report.id, user, db)
    assert "deleted successfully" in del_res["message"]

    history_after = get_ats_history(user, db)
    assert history_after.total == history.total - 1
