from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.auth import UserOut, CandidateProfileOut, EmployerProfileOut
from app.core.deps import get_current_user

router = APIRouter(prefix="/users", tags=["Users"])

@router.get("/me", response_model=UserOut)
def get_current_user_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Retrieve the current logged-in user details and profile."""
    candidate_out = None
    if current_user.candidate_profile:
        candidate_out = CandidateProfileOut.model_validate(current_user.candidate_profile)

    employer_out = None
    if current_user.employer_profile:
        emp = current_user.employer_profile
        company_name = emp.company.name if emp.company else None
        employer_out = EmployerProfileOut(
            id=emp.id,
            full_name=emp.full_name,
            designation=emp.designation,
            contact_email=emp.contact_email,
            company_id=emp.company_id,
            company_name=company_name
        )

    return UserOut(
        id=current_user.id,
        email=current_user.email,
        role=current_user.role,
        is_active=current_user.is_active,
        is_verified=current_user.is_verified,
        candidate_profile=candidate_out,
        employer_profile=employer_out
    )

@router.delete("/me", status_code=status.HTTP_200_OK)
def delete_my_account(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Google Play Compliance: Self-service account and personal data deletion.
    Cascades and deletes user profile, applications, and uploaded data.
    """
    db.delete(current_user)
    db.commit()
    return {"message": "Account and associated personal data deleted successfully."}
