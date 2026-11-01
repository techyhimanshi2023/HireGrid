import os
import sys
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Ensure backend directory is in path
current_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from app.database import Base
from app.models.user import User, UserRole, CandidateProfile, EmployerProfile, Company
from app.schemas.auth import UserRegister, UserLogin, UserOut
from app.routers.auth import register, login
from app.routers.users import get_current_user_profile, delete_my_account
from app.core.security import get_password_hash, verify_password, create_access_token, decode_token
from fastapi import HTTPException

TEST_DB_FILE = "./test_hiregrid_unit.db"
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

def test_password_hashing_and_verification():
    raw_password = "SecurePassword123!"
    hashed = get_password_hash(raw_password)
    assert hashed != raw_password
    assert verify_password(raw_password, hashed) is True
    assert verify_password("WrongPassword!", hashed) is False

def test_token_creation_and_decoding():
    user_id = "test-uuid-1234"
    role = UserRole.CANDIDATE.value
    token = create_access_token(subject=user_id, role=role)
    payload = decode_token(token)
    assert payload is not None
    assert payload["sub"] == user_id
    assert payload["role"] == role
    assert payload["type"] == "access"

def test_candidate_registration(db):
    user_in = UserRegister(
        email="candidate.jane@example.com",
        password="CandidatePass123!",
        role="candidate",
        full_name="Jane Doe"
    )
    token_resp = register(user_in, db)
    assert token_resp.access_token is not None
    assert token_resp.role == "candidate"
    assert token_resp.email == "candidate.jane@example.com"
    assert token_resp.full_name == "Jane Doe"

    # Verify candidate profile in database
    user = db.query(User).filter(User.email == "candidate.jane@example.com").first()
    assert user is not None
    assert user.role == UserRole.CANDIDATE.value
    assert user.candidate_profile is not None
    assert user.candidate_profile.full_name == "Jane Doe"

def test_employer_registration(db):
    user_in = UserRegister(
        email="recruiter.bob@apextech.com",
        password="EmployerPass123!",
        role="employer",
        full_name="Bob Smith",
        company_name="Apex Tech Corp"
    )
    token_resp = register(user_in, db)
    assert token_resp.access_token is not None
    assert token_resp.role == "employer"
    assert token_resp.email == "recruiter.bob@apextech.com"
    assert token_resp.full_name == "Bob Smith"

    # Verify employer profile and company in database
    user = db.query(User).filter(User.email == "recruiter.bob@apextech.com").first()
    assert user is not None
    assert user.role == UserRole.EMPLOYER.value
    assert user.employer_profile is not None
    assert user.employer_profile.company is not None
    assert user.employer_profile.company.name == "Apex Tech Corp"

def test_duplicate_registration_fails(db):
    user_in = UserRegister(
        email="candidate.jane@example.com",
        password="AnotherPassword123!",
        role="candidate",
        full_name="Duplicate Jane"
    )
    with pytest.raises(HTTPException) as exc_info:
        register(user_in, db)
    assert exc_info.value.status_code == 400
    assert "already exists" in exc_info.value.detail

def test_invalid_role_registration_fails(db):
    user_in = UserRegister(
        email="invalid.role@example.com",
        password="Password123!",
        role="super_hacker",
        full_name="Invalid Role"
    )
    with pytest.raises(HTTPException) as exc_info:
        register(user_in, db)
    assert exc_info.value.status_code == 400
    assert "Invalid role" in exc_info.value.detail

def test_candidate_login_success(db):
    login_in = UserLogin(
        email="candidate.jane@example.com",
        password="CandidatePass123!"
    )
    token_resp = login(login_in, db)
    assert token_resp.access_token is not None
    assert token_resp.role == "candidate"
    assert token_resp.full_name == "Jane Doe"

def test_login_invalid_password_fails(db):
    login_in = UserLogin(
        email="candidate.jane@example.com",
        password="WrongPassword!"
    )
    with pytest.raises(HTTPException) as exc_info:
        login(login_in, db)
    assert exc_info.value.status_code == 401
    assert "Invalid email or password" in exc_info.value.detail

def test_get_current_user_profile(db):
    user = db.query(User).filter(User.email == "candidate.jane@example.com").first()
    profile_out = get_current_user_profile(current_user=user, db=db)
    assert profile_out.email == "candidate.jane@example.com"
    assert profile_out.role == "candidate"
    assert profile_out.candidate_profile.full_name == "Jane Doe"

def test_account_deletion_compliance(db):
    # Register temporary user
    temp_user_in = UserRegister(
        email="to.delete@example.com",
        password="Password123!",
        role="candidate",
        full_name="Delete Me"
    )
    register(temp_user_in, db)
    user = db.query(User).filter(User.email == "to.delete@example.com").first()
    assert user is not None

    # Call delete account endpoint
    resp = delete_my_account(current_user=user, db=db)
    assert "deleted successfully" in resp["message"]

    # Verify user and candidate profile are completely removed
    deleted_user = db.query(User).filter(User.email == "to.delete@example.com").first()
    assert deleted_user is None
