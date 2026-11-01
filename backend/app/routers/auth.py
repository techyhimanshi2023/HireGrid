from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User, UserRole, CandidateProfile, EmployerProfile, Company
from app.schemas.auth import UserRegister, UserLogin, Token, UserOut
from app.core.security import get_password_hash, verify_password, create_access_token
from app.core.deps import get_current_user

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register", response_model=Token, status_code=status.HTTP_201_CREATED)
def register(user_in: UserRegister, db: Session = Depends(get_db)):
    """Register a new Job Seeker (candidate) or Recruiter (employer)."""
    # 1. Validate role
    role_str = user_in.role.lower().strip()
    if role_str not in [UserRole.CANDIDATE.value, UserRole.EMPLOYER.value]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid role '{user_in.role}'. Must be '{UserRole.CANDIDATE.value}' or '{UserRole.EMPLOYER.value}'."
        )

    # 2. Check if email already registered
    existing_user = db.query(User).filter(User.email == user_in.email.lower()).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email address already exists."
        )

    # 3. Hash password and create User
    hashed_pw = get_password_hash(user_in.password)
    user = User(
        email=user_in.email.lower(),
        hashed_password=hashed_pw,
        role=role_str,
        is_active=True,
        is_verified=True  # Default active for development
    )
    db.add(user)
    db.flush()  # Flush so user.id is generated

    # 4. Create corresponding profile based on role
    if role_str == UserRole.CANDIDATE.value:
        candidate_profile = CandidateProfile(
            user_id=user.id,
            full_name=user_in.full_name
        )
        db.add(candidate_profile)
    elif role_str == UserRole.EMPLOYER.value:
        company_id = None
        if user_in.company_name:
            # Check or create company
            company = db.query(Company).filter(Company.name == user_in.company_name.strip()).first()
            if not company:
                company = Company(name=user_in.company_name.strip())
                db.add(company)
                db.flush()
            company_id = company.id

        employer_profile = EmployerProfile(
            user_id=user.id,
            company_id=company_id,
            full_name=user_in.full_name,
            contact_email=user_in.email.lower()
        )
        db.add(employer_profile)

    db.commit()
    db.refresh(user)

    # 5. Generate Access Token
    access_token = create_access_token(subject=user.id, role=user.role)

    return Token(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id,
        email=user.email,
        role=user.role,
        full_name=user_in.full_name
    )

@router.post("/login", response_model=Token)
def login(login_in: UserLogin, db: Session = Depends(get_db)):
    """Authenticate with email and password, returning JWT access token and user role."""
    user = db.query(User).filter(User.email == login_in.email.lower()).first()
    if not user or not verify_password(login_in.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account is deactivated"
        )

    # Determine full_name from profile
    full_name = "User"
    if user.role == UserRole.CANDIDATE.value and user.candidate_profile:
        full_name = user.candidate_profile.full_name
    elif user.role == UserRole.EMPLOYER.value and user.employer_profile:
        full_name = user.employer_profile.full_name

    access_token = create_access_token(subject=user.id, role=user.role)

    return Token(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id,
        email=user.email,
        role=user.role,
        full_name=full_name
    )
