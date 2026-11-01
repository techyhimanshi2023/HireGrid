from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, func
from app.database import get_db
from app.models.user import User, UserRole, Company
from app.models.job import Job, JobStatus, JobSkill, SavedJob
from app.models.application import Application
from app.schemas.job import JobCreate, JobUpdate, JobOut, JobListOut, CompanyBriefOut
from app.core.deps import require_role

router = APIRouter(prefix="/jobs", tags=["Jobs Marketplace"])

def _format_job_out(job: Job, db: Session, current_user_id: Optional[str] = None) -> JobOut:
    skills = [s.skill_name for s in job.job_skills]
    applicant_count = db.query(func.count(Application.id)).filter(Application.job_id == job.id).scalar() or 0
    
    is_saved = False
    if current_user_id:
        user = db.query(User).filter(User.id == current_user_id).first()
        if user and user.candidate_profile:
            saved = db.query(SavedJob).filter(
                SavedJob.candidate_id == user.candidate_profile.id,
                SavedJob.job_id == job.id
            ).first()
            is_saved = saved is not None

    company_brief = None
    if job.company:
        company_brief = CompanyBriefOut(
            id=job.company.id,
            name=job.company.name,
            logo_url=job.company.logo_url,
            location=job.company.location,
            website=job.company.website
        )

    return JobOut(
        id=job.id,
        company_id=job.company_id,
        posted_by_user_id=job.posted_by_user_id,
        title=job.title,
        description=job.description,
        responsibilities=job.responsibilities,
        requirements=job.requirements,
        job_type=job.job_type,
        workplace_type=job.workplace_type,
        location=job.location,
        min_experience_years=job.min_experience_years,
        max_experience_years=job.max_experience_years,
        min_salary=job.min_salary,
        max_salary=job.max_salary,
        currency=job.currency,
        vacancies=job.vacancies,
        application_deadline=job.application_deadline,
        status=job.status,
        created_at=job.created_at,
        updated_at=job.updated_at,
        skills=skills,
        company=company_brief,
        applicant_count=applicant_count,
        is_saved=is_saved
    )

@router.post("", response_model=JobOut, status_code=status.HTTP_201_CREATED)
def create_job(
    job_in: JobCreate,
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: Create a new job vacancy for their registered company."""
    if not current_user.employer_profile or not current_user.employer_profile.company_id:
        company = Company(name=f"{current_user.employer_profile.full_name}'s Organization" if current_user.employer_profile else "HireGrid Partner")
        db.add(company)
        db.flush()
        if current_user.employer_profile:
            current_user.employer_profile.company_id = company.id
            db.flush()

    company_id = current_user.employer_profile.company_id

    job = Job(
        company_id=company_id,
        posted_by_user_id=current_user.id,
        title=job_in.title,
        description=job_in.description,
        responsibilities=job_in.responsibilities,
        requirements=job_in.requirements,
        job_type=job_in.job_type,
        workplace_type=job_in.workplace_type,
        location=job_in.location,
        min_experience_years=job_in.min_experience_years,
        max_experience_years=job_in.max_experience_years,
        min_salary=job_in.min_salary,
        max_salary=job_in.max_salary,
        currency=job_in.currency,
        vacancies=job_in.vacancies,
        application_deadline=job_in.application_deadline,
        status=JobStatus.ACTIVE.value
    )
    db.add(job)
    db.flush()

    for skill in job_in.required_skills:
        skill_clean = skill.strip()
        if skill_clean:
            job_skill = JobSkill(job_id=job.id, skill_name=skill_clean, is_required=True)
            db.add(job_skill)

    db.commit()
    db.refresh(job)

    return _format_job_out(job, db, current_user.id)

@router.get("", response_model=JobListOut)
def list_jobs(
    q: Optional[str] = None,
    location: Optional[str] = None,
    job_type: Optional[str] = None,
    workplace_type: Optional[str] = None,
    min_salary: Optional[float] = None,
    max_experience: Optional[int] = None,
    limit: int = 20,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """Browse and filter active job opportunities with keyword search and pagination."""
    query = db.query(Job).filter(Job.status == JobStatus.ACTIVE.value)

    if q and isinstance(q, str):
        search_filter = or_(
            Job.title.ilike(f"%{q}%"),
            Job.description.ilike(f"%{q}%"),
            Job.location.ilike(f"%{q}%")
        )
        query = query.filter(search_filter)

    if location and isinstance(location, str):
        query = query.filter(Job.location.ilike(f"%{location}%"))

    if job_type and isinstance(job_type, str):
        query = query.filter(Job.job_type == job_type)

    if workplace_type and isinstance(workplace_type, str):
        query = query.filter(Job.workplace_type == workplace_type)

    if min_salary is not None and isinstance(min_salary, (int, float)):
        query = query.filter(Job.min_salary >= min_salary)

    if max_experience is not None and isinstance(max_experience, int):
        query = query.filter(Job.min_experience_years <= max_experience)

    total = query.count()
    limit_val = limit if isinstance(limit, int) else 20
    offset_val = offset if isinstance(offset, int) else 0

    jobs = query.order_by(Job.created_at.desc()).offset(offset_val).limit(limit_val).all()

    formatted_items = [_format_job_out(j, db) for j in jobs]
    return JobListOut(total=total, items=formatted_items)

@router.get("/employer/my-jobs", response_model=JobListOut)
def get_employer_jobs(
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: List all jobs published by the current employer's company."""
    if not current_user.employer_profile or not current_user.employer_profile.company_id:
        return JobListOut(total=0, items=[])

    company_id = current_user.employer_profile.company_id
    jobs = db.query(Job).filter(Job.company_id == company_id).order_by(Job.created_at.desc()).all()
    
    formatted_items = [_format_job_out(j, db, current_user.id) for j in jobs]
    return JobListOut(total=len(formatted_items), items=formatted_items)

@router.get("/{job_id}", response_model=JobOut)
def get_job_by_id(
    job_id: str,
    db: Session = Depends(get_db)
):
    """View complete job description, required qualifications, and company details."""
    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job posting not found"
        )
    return _format_job_out(job, db)

@router.put("/{job_id}", response_model=JobOut)
def update_job(
    job_id: str,
    job_in: JobUpdate,
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: Update job details (only allowed for authoring company)."""
    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found")

    if job.company_id != current_user.employer_profile.company_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You do not have permission to modify job postings for another company"
        )

    update_data = job_in.model_dump(exclude_unset=True)
    required_skills = update_data.pop("required_skills", None)

    for field, val in update_data.items():
        setattr(job, field, val)

    if required_skills is not None:
        db.query(JobSkill).filter(JobSkill.job_id == job.id).delete()
        for skill in required_skills:
            if skill.strip():
                db.add(JobSkill(job_id=job.id, skill_name=skill.strip(), is_required=True))

    db.commit()
    db.refresh(job)
    return _format_job_out(job, db, current_user.id)

@router.patch("/{job_id}/status", response_model=JobOut)
def change_job_status(
    job_id: str,
    status_val: str = Query(..., description="active or closed"),
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: Open or close an existing job posting."""
    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found")

    if job.company_id != current_user.employer_profile.company_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Permission denied")

    if status_val not in [JobStatus.ACTIVE.value, JobStatus.CLOSED.value]:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid status")

    job.status = status_val
    db.commit()
    db.refresh(job)
    return _format_job_out(job, db, current_user.id)

@router.delete("/{job_id}", status_code=status.HTTP_200_OK)
def delete_job(
    job_id: str,
    current_user: User = Depends(require_role(UserRole.EMPLOYER.value)),
    db: Session = Depends(get_db)
):
    """Employer: Archive or delete an existing job posting."""
    job = db.query(Job).filter(Job.id == job_id).first()
    if not job:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job not found")

    if job.company_id != current_user.employer_profile.company_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Permission denied")

    db.delete(job)
    db.commit()
    return {"message": "Job deleted successfully"}
