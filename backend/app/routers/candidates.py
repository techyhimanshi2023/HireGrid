from typing import List, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.user import User, UserRole
from app.models.job import Job, JobStatus, SavedJob
from app.models.application import Application
from app.schemas.job import JobListOut, JobOut
from app.routers.jobs import _format_job_out
from app.core.deps import require_role

router = APIRouter(prefix="/candidates", tags=["Candidate Experience"])

@router.post("/saved-jobs/{job_id}", status_code=status.HTTP_200_OK)
def toggle_save_job(
    job_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Bookmark or unbookmark a job vacancy."""
    candidate = current_user.candidate_profile
    if not candidate:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Profile required")

    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found")

    existing_saved = db.query(SavedJob).filter(
        SavedJob.candidate_id == candidate.id,
        SavedJob.job_id == job_id
    ).first()

    if existing_saved:
        db.delete(existing_saved)
        db.commit()
        return {"job_id": job_id, "is_saved": False, "message": "Job removed from bookmarks."}
    else:
        new_saved = SavedJob(candidate_id=candidate.id, job_id=job_id)
        db.add(new_saved)
        db.commit()
        return {"job_id": job_id, "is_saved": True, "message": "Job saved to bookmarks."}

@router.get("/saved-jobs", response_model=JobListOut)
def get_saved_jobs(
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Retrieve all bookmarked jobs."""
    candidate = current_user.candidate_profile
    if not candidate:
        return JobListOut(total=0, items=[])

    saved_entries = db.query(SavedJob).filter(SavedJob.candidate_id == candidate.id).all()
    job_ids = [s.job_id for s in saved_entries]
    
    if not job_ids:
        return JobListOut(total=0, items=[])

    jobs = db.query(Job).filter(Job.id.in_(job_ids)).all()
    items = [_format_job_out(j, db, current_user.id) for j in jobs]
    return JobListOut(total=len(items), items=items)

@router.get("/dashboard")
def get_candidate_dashboard(
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Candidate: Retrieve summary statistics for the candidate home dashboard."""
    candidate = current_user.candidate_profile
    if not candidate:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Profile required")

    total_applications = db.query(func.count(Application.id)).filter(
        Application.candidate_id == candidate.id
    ).scalar() or 0

    saved_jobs_count = db.query(func.count(SavedJob.id)).filter(
        SavedJob.candidate_id == candidate.id
    ).scalar() or 0

    # Recent active jobs
    recent_jobs = db.query(Job).filter(
        Job.status == JobStatus.ACTIVE.value
    ).order_by(Job.created_at.desc()).limit(5).all()

    formatted_recent_jobs = [_format_job_out(j, db, current_user.id).model_dump() for j in recent_jobs]

    return {
        "candidate_name": candidate.full_name,
        "headline": candidate.headline,
        "total_applications": total_applications,
        "saved_jobs_count": saved_jobs_count,
        "recent_jobs": formatted_recent_jobs
    }
