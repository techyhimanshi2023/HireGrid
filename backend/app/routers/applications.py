from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User, UserRole
from app.models.job import Job, JobStatus
from app.models.application import Application, ApplicationStatus, ApplicationStatusHistory
from app.schemas.application import (
    ApplicationCreate,
    ApplicationStatusUpdate,
    ApplicationOut,
    ApplicationListOut,
    CandidateBriefOut,
    StatusHistoryItem
)
from app.schemas.job import JobOut, CompanyBriefOut
from app.core.deps import require_role

router = APIRouter(prefix="/applications", tags=["Applications"])

def _format_application_out(app_obj: Application) -> ApplicationOut:
    history_items = [
        StatusHistoryItem(
            id=h.id,
            from_status=h.from_status,
            to_status=h.to_status,
            notes=h.notes,
            created_at=h.created_at
        )
        for h in app_obj.status_history
    ]

    candidate_brief = None
    if app_obj.candidate:
        c = app_obj.candidate
        candidate_brief = CandidateBriefOut(
            id=c.id,
            full_name=c.full_name,
            headline=c.headline,
            location=c.location,
            years_of_experience=c.years_of_experience,
            education_level=c.education_level
        )

    job_brief = None
    if app_obj.job:
        j = app_obj.job
        comp_brief = None
        if j.company:
            comp_brief = CompanyBriefOut(
                id=j.company.id,
                name=j.company.name,
                logo_url=j.company.logo_url,
                location=j.company.location,
                website=j.company.website
            )
        job_brief = JobOut(
            id=j.id,
            company_id=j.company_id,
            posted_by_user_id=j.posted_by_user_id,
            title=j.title,
            description=j.description,
            responsibilities=j.responsibilities,
            requirements=j.requirements,
            job_type=j.job_type,
            workplace_type=j.workplace_type,
            location=j.location,
            min_experience_years=j.min_experience_years,
            max_experience_years=j.max_experience_years,
            min_salary=j.min_salary,
            max_salary=j.max_salary,
            currency=j.currency,
            vacancies=j.vacancies,
            application_deadline=j.application_deadline,
            status=j.status,
            created_at=j.created_at,
            updated_at=j.updated_at,
            skills=[s.skill_name for s in j.job_skills],
            company=comp_brief
        )

    return ApplicationOut(
        id=app_obj.id,
        job_id=app_obj.job_id,
        candidate_id=app_obj.candidate_id,
        resume_id=app_obj.resume_id,
        status=app_obj.status,
        cover_note=app_obj.cover_note,
        created_at=app_obj.created_at,
        updated_at=app_obj.updated_at,
        job=job_brief,
        candidate=candidate_brief,
        status_history=history_items
    )

@router.post("/apply", response_model=ApplicationOut, status_code=status.HTTP_201_CREATED)
def apply_to_job(
    app_in: ApplicationCreate,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Submit an application for an active job vacancy."""
    candidate_profile = current_user.candidate_profile
    if not candidate_profile:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Candidate profile does not exist. Please complete your profile first."
        )

    # 1. Verify job existence and active status
    job = db.query(Job).filter(Job.id == app_in.job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job position not found")

    if job.status != JobStatus.ACTIVE.value:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This job position is no longer accepting applications."
        )

    # 2. Check duplicate applications
    existing_application = db.query(Application).filter(
        Application.job_id == app_in.job_id,
        Application.candidate_id == candidate_profile.id
    ).first()

    if existing_application:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You have already submitted an application for this position."
        )

    # 3. Create Application
    application = Application(
        job_id=job.id,
        candidate_id=candidate_profile.id,
        resume_id=app_in.resume_id,
        cover_note=app_in.cover_note,
        status=ApplicationStatus.APPLIED.value
    )
    db.add(application)
    db.flush()

    # 4. Log initial history item
    history_entry = ApplicationStatusHistory(
        application_id=application.id,
        from_status=None,
        to_status=ApplicationStatus.APPLIED.value,
        changed_by_user_id=current_user.id,
        notes="Application submitted by candidate"
    )
    db.add(history_entry)

    db.commit()
    db.refresh(application)

    return _format_application_out(application)

@router.get("/my-applications", response_model=ApplicationListOut)
def get_my_applications(
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: View all submitted applications with live status and history timeline."""
    if not current_user.candidate_profile:
        return ApplicationListOut(total=0, items=[])

    applications = db.query(Application).filter(
        Application.candidate_id == current_user.candidate_profile.id
    ).order_by(Application.created_at.desc()).all()

    items = [_format_application_out(a) for a in applications]
    return ApplicationListOut(total=len(items), items=items)

@router.patch("/{application_id}/withdraw", response_model=ApplicationOut)
def withdraw_application(
    application_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Withdraw application if it is still in an eligible stage."""
    application = db.query(Application).filter(
        Application.id == application_id,
        Application.candidate_id == current_user.candidate_profile.id
    ).first()

    if not application:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found")

    if application.status in [ApplicationStatus.SELECTED.value, ApplicationStatus.WITHDRAWN.value]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot withdraw application in '{application.status}' state."
        )

    prev_status = application.status
    application.status = ApplicationStatus.WITHDRAWN.value

    history_entry = ApplicationStatusHistory(
        application_id=application.id,
        from_status=prev_status,
        to_status=ApplicationStatus.WITHDRAWN.value,
        changed_by_user_id=current_user.id,
        notes="Withdrawn by candidate"
    )
    db.add(history_entry)

    db.commit()
    db.refresh(application)
    return _format_application_out(application)

@router.get("/job/{job_id}", response_model=ApplicationListOut)
def get_job_applicants(
    job_id: str,
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: Review all candidates who applied to a specific company job."""
    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job position not found")

    if job.company_id != current_user.employer_profile.company_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access forbidden. You do not own this job posting."
        )

    applications = db.query(Application).filter(
        Application.job_id == job_id
    ).order_by(Application.created_at.desc()).all()

    items = [_format_application_out(a) for a in applications]
    return ApplicationListOut(total=len(items), items=items)

@router.patch("/{application_id}/status", response_model=ApplicationOut)
def update_application_status(
    application_id: str,
    status_in: ApplicationStatusUpdate,
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """
    Employer: Advance candidate stage along the hiring workflow:
    Applied -> Under Review -> Shortlisted / Rejected -> Interview -> Selected
    """
    application = db.query(Application).filter(Application.id == application_id).first()
    if not application:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Application not found")

    # Verify authorization: Job must belong to employer's company
    job = application.job
    if not job or job.company_id != current_user.employer_profile.company_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access forbidden. You do not own the job posting for this application."
        )

    valid_statuses = [
        ApplicationStatus.UNDER_REVIEW.value,
        ApplicationStatus.SHORTLISTED.value,
        ApplicationStatus.INTERVIEW.value,
        ApplicationStatus.REJECTED.value,
        ApplicationStatus.SELECTED.value
    ]

    target_status = status_in.status.lower().strip()
    if target_status not in valid_statuses:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid status '{status_in.status}'. Allowed: {valid_statuses}"
        )

    prev_status = application.status
    application.status = target_status

    history_entry = ApplicationStatusHistory(
        application_id=application.id,
        from_status=prev_status,
        to_status=target_status,
        changed_by_user_id=current_user.id,
        notes=status_in.notes or f"Status updated to {target_status} by employer"
    )
    db.add(history_entry)

    db.commit()
    db.refresh(application)
    return _format_application_out(application)
