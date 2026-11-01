from typing import Dict, Any, List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.user import User, UserRole
from app.models.job import Job, JobStatus
from app.models.application import Application, ApplicationStatus
from app.routers.applications import _format_application_out
from app.core.deps import require_role

router = APIRouter(prefix="/employers", tags=["Employer Experience"])

@router.get("/dashboard")
def get_employer_dashboard(
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Employer: Overview dashboard showing recruitment pipeline statistics and recent applicants."""
    employer = current_user.employer_profile
    if not employer or not employer.company_id:
        return {
            "total_jobs": 0,
            "active_jobs": 0,
            "total_applicants": 0,
            "shortlisted": 0,
            "interview": 0,
            "selected": 0,
            "recent_applications": []
        }

    company_id = employer.company_id

    # Jobs counts
    total_jobs = db.query(func.count(Job.id)).filter(Job.company_id == company_id).scalar() or 0
    active_jobs = db.query(func.count(Job.id)).filter(
        Job.company_id == company_id,
        Job.status == JobStatus.ACTIVE.value
    ).scalar() or 0

    # Applications to company jobs
    company_job_ids = [j.id for j in db.query(Job.id).filter(Job.company_id == company_id).all()]

    total_applicants = 0
    shortlisted_count = 0
    interview_count = 0
    selected_count = 0
    recent_apps_out = []

    if company_job_ids:
        total_applicants = db.query(func.count(Application.id)).filter(
            Application.job_id.in_(company_job_ids)
        ).scalar() or 0

        shortlisted_count = db.query(func.count(Application.id)).filter(
            Application.job_id.in_(company_job_ids),
            Application.status == ApplicationStatus.SHORTLISTED.value
        ).scalar() or 0

        interview_count = db.query(func.count(Application.id)).filter(
            Application.job_id.in_(company_job_ids),
            Application.status == ApplicationStatus.INTERVIEW.value
        ).scalar() or 0

        selected_count = db.query(func.count(Application.id)).filter(
            Application.job_id.in_(company_job_ids),
            Application.status == ApplicationStatus.SELECTED.value
        ).scalar() or 0

        recent_applications = db.query(Application).filter(
            Application.job_id.in_(company_job_ids)
        ).order_by(Application.created_at.desc()).limit(5).all()

        recent_apps_out = [_format_application_out(a).model_dump() for a in recent_applications]

    return {
        "company_name": employer.company.name if employer.company else "My Organization",
        "total_jobs": total_jobs,
        "active_jobs": active_jobs,
        "total_applicants": total_applicants,
        "shortlisted": shortlisted_count,
        "interview": interview_count,
        "selected": selected_count,
        "recent_applications": recent_apps_out
    }
