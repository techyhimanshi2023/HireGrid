import os
import sys
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from fastapi import HTTPException

current_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from app.database import Base
from app.models.user import User, UserRole, CandidateProfile, EmployerProfile, Company
from app.models.job import Job, JobStatus, JobSkill, SavedJob
from app.models.application import Application, ApplicationStatus, ApplicationStatusHistory
from app.schemas.auth import UserRegister
from app.schemas.job import JobCreate, JobUpdate
from app.schemas.application import ApplicationCreate, ApplicationStatusUpdate
from app.routers.auth import register
from app.routers.jobs import create_job, list_jobs, get_job_by_id, update_job, change_job_status
from app.routers.applications import apply_to_job, get_my_applications, withdraw_application, get_job_applicants, update_application_status
from app.routers.candidates import toggle_save_job, get_saved_jobs, get_candidate_dashboard
from app.routers.employers import get_employer_dashboard

TEST_DB_FILE = "./test_hiregrid_jobs.db"
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

@pytest.fixture(scope="module")
def test_users():
    """Seed an employer and a candidate once for this test module."""
    db_session = TestingSessionLocal()
    try:
        # Employer
        emp_in = UserRegister(
            email="recruiter.sarah@globaltech.com",
            password="EmployerPassword123!",
            role="employer",
            full_name="Sarah Connor",
            company_name="GlobalTech Solutions"
        )
        register(emp_in, db_session)
        employer_user = db_session.query(User).filter(User.email == "recruiter.sarah@globaltech.com").first()

        # Candidate
        cand_in = UserRegister(
            email="developer.alex@example.com",
            password="CandidatePassword123!",
            role="candidate",
            full_name="Alex River"
        )
        register(cand_in, db_session)
        candidate_user = db_session.query(User).filter(User.email == "developer.alex@example.com").first()

        return {"employer_id": employer_user.id, "candidate_id": candidate_user.id}
    finally:
        db_session.close()

def test_employer_creates_job(db, test_users):
    employer = db.query(User).filter(User.id == test_users["employer_id"]).first()
    job_in = JobCreate(
        title="Senior Android Engineer",
        description="Looking for an experienced Android Developer with Kotlin and Jetpack Compose skills.",
        responsibilities="Build scalable mobile applications.",
        requirements="5+ years in Android, Kotlin, M3, MVVM.",
        job_type="full_time",
        workplace_type="remote",
        location="Remote, Worldwide",
        min_experience_years=5,
        min_salary=120000,
        max_salary=160000,
        currency="USD",
        vacancies=2,
        required_skills=["Kotlin", "Jetpack Compose", "Coroutines", "MVVM"]
    )
    job_out = create_job(job_in, employer, db)
    assert job_out.id is not None
    assert job_out.title == "Senior Android Engineer"
    assert "Kotlin" in job_out.skills
    assert "Jetpack Compose" in job_out.skills
    assert job_out.company.name == "GlobalTech Solutions"
    assert job_out.status == JobStatus.ACTIVE.value

def test_list_and_search_jobs(db):
    # Query all active jobs
    jobs_list = list_jobs(db=db)
    assert jobs_list.total >= 1
    assert any("Android" in j.title for j in jobs_list.items)

    # Search with keyword "Android"
    search_res = list_jobs(q="Android", db=db)
    assert search_res.total >= 1

    # Search with non-matching keyword
    empty_res = list_jobs(q="NonExistentAstronomerJob", db=db)
    assert empty_res.total == 0

def test_candidate_apply_to_job(db, test_users):
    candidate = db.query(User).filter(User.id == test_users["candidate_id"]).first()
    job = db.query(Job).filter(Job.title == "Senior Android Engineer").first()
    assert job is not None

    app_in = ApplicationCreate(
        job_id=job.id,
        cover_note="Excited to apply! I have 6 years of Kotlin experience."
    )
    app_out = apply_to_job(app_in, candidate, db)
    assert app_out.id is not None
    assert app_out.status == ApplicationStatus.APPLIED.value
    assert app_out.job.title == "Senior Android Engineer"
    assert len(app_out.status_history) == 1
    assert app_out.status_history[0].to_status == ApplicationStatus.APPLIED.value

def test_duplicate_application_prevented(db, test_users):
    candidate = db.query(User).filter(User.id == test_users["candidate_id"]).first()
    job = db.query(Job).filter(Job.title == "Senior Android Engineer").first()

    app_in = ApplicationCreate(
        job_id=job.id,
        cover_note="Attempting to apply again."
    )
    with pytest.raises(HTTPException) as exc_info:
        apply_to_job(app_in, candidate, db)
    assert exc_info.value.status_code == 400
    assert "already submitted an application" in exc_info.value.detail

def test_candidate_views_applications(db, test_users):
    candidate = db.query(User).filter(User.id == test_users["candidate_id"]).first()
    my_apps = get_my_applications(candidate, db)
    assert my_apps.total >= 1
    first_app = my_apps.items[0]
    assert first_app.job.title == "Senior Android Engineer"
    assert first_app.status == ApplicationStatus.APPLIED.value

def test_candidate_saved_jobs_toggle(db, test_users):
    candidate = db.query(User).filter(User.id == test_users["candidate_id"]).first()
    job = db.query(Job).filter(Job.title == "Senior Android Engineer").first()

    # 1. Bookmark
    res1 = toggle_save_job(job.id, candidate, db)
    assert res1["is_saved"] is True

    # Check saved list
    saved_list = get_saved_jobs(candidate, db)
    assert saved_list.total == 1
    assert saved_list.items[0].id == job.id

    # 2. Unbookmark
    res2 = toggle_save_job(job.id, candidate, db)
    assert res2["is_saved"] is False

    # Check saved list again
    saved_list_after = get_saved_jobs(candidate, db)
    assert saved_list_after.total == 0

def test_employer_views_applicants_and_updates_status(db, test_users):
    employer = db.query(User).filter(User.id == test_users["employer_id"]).first()
    job = db.query(Job).filter(Job.title == "Senior Android Engineer").first()

    # Employer reviews applicants for their job
    applicants = get_job_applicants(job.id, employer, db)
    assert applicants.total >= 1
    application = applicants.items[0]
    assert application.candidate.full_name == "Alex River"

    # Advance stage: applied -> under_review
    stage1 = update_application_status(
        application.id,
        ApplicationStatusUpdate(status="under_review", notes="Reviewing portfolio"),
        employer,
        db
    )
    assert stage1.status == "under_review"

    # Advance stage: under_review -> shortlisted
    stage2 = update_application_status(
        application.id,
        ApplicationStatusUpdate(status="shortlisted", notes="Skills match requirements"),
        employer,
        db
    )
    assert stage2.status == "shortlisted"

    # Advance stage: shortlisted -> interview
    stage3 = update_application_status(
        application.id,
        ApplicationStatusUpdate(status="interview", notes="Technical interview scheduled"),
        employer,
        db
    )
    assert stage3.status == "interview"

    # Final stage: interview -> selected
    stage4 = update_application_status(
        application.id,
        ApplicationStatusUpdate(status="selected", notes="Offer letter accepted"),
        employer,
        db
    )
    assert stage4.status == "selected"
    assert len(stage4.status_history) == 5  # applied, under_review, shortlisted, interview, selected

def test_candidate_and_employer_dashboards(db, test_users):
    candidate = db.query(User).filter(User.id == test_users["candidate_id"]).first()
    employer = db.query(User).filter(User.id == test_users["employer_id"]).first()

    cand_dash = get_candidate_dashboard(candidate, db)
    assert cand_dash["candidate_name"] == "Alex River"
    assert cand_dash["total_applications"] >= 1

    emp_dash = get_employer_dashboard(employer, db)
    assert emp_dash["company_name"] == "GlobalTech Solutions"
    assert emp_dash["total_jobs"] >= 1
    assert emp_dash["selected"] >= 1
