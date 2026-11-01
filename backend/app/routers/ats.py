import os
import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.config import settings
from app.models.user import User, UserRole
from app.models.job import Job
from app.models.resume import Resume, ResumeAnalysis
from app.schemas.ats import ResumeOut, ResumeListOut, AtsMatchRequest, AtsReportOut, AtsHistoryListOut
from app.services.ats_engine import (
    extract_text_from_pdf,
    extract_text_from_docx,
    analyze_general_resume,
    analyze_compatibility
)
from app.core.deps import require_role

router = APIRouter(prefix="/ats", tags=["ATS Resume Analyzer"])

ALLOWED_EXTENSIONS = {".pdf", ".docx"}

@router.post("/upload-resume", response_model=ResumeOut, status_code=status.HTTP_201_CREATED)
async def upload_resume(
    file: UploadFile = File(...),
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """
    Candidate: Upload a resume (PDF or DOCX).
    Validates file extension, size (<= 5MB), and parses text content safely.
    """
    candidate = current_user.candidate_profile
    if not candidate:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Candidate profile required")

    file_name = file.filename or "resume.pdf"
    _, ext = os.path.splitext(file_name)
    ext_lower = ext.lower()

    if ext_lower not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file format '{ext}'. Allowed formats: PDF (.pdf), Word (.docx)"
        )

    # Read bytes and check size
    file_bytes = await file.read()
    if len(file_bytes) > settings.MAX_FILE_SIZE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File exceeds maximum allowed size of {settings.MAX_FILE_SIZE_BYTES // (1024 * 1024)} MB."
        )

    if len(file_bytes) == 0:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Uploaded file is empty.")

    # Extract text safely
    raw_text = ""
    try:
        if ext_lower == ".pdf":
            raw_text = extract_text_from_pdf(file_bytes)
        elif ext_lower == ".docx":
            raw_text = extract_text_from_docx(file_bytes)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Failed to read document contents. Ensure file is not password-protected or corrupted. Error: {str(e)}"
        )

    if not raw_text.strip():
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="No readable text found in document. Please upload a standard text-based PDF or DOCX file."
        )

    # Save physical file outside public roots with UUID
    os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
    saved_filename = f"{uuid.uuid4()}{ext_lower}"
    file_path = os.path.join(settings.UPLOAD_DIR, saved_filename)
    with open(file_path, "wb") as f:
        f.write(file_bytes)

    resume = Resume(
        candidate_id=candidate.id,
        file_name=file_name,
        file_path=file_path,
        file_mime_type=file.content_type or "application/octet-stream",
        file_size_bytes=len(file_bytes),
        raw_extracted_text=raw_text
    )
    db.add(resume)
    db.commit()
    db.refresh(resume)

    return ResumeOut.model_validate(resume)

@router.get("/resumes", response_model=ResumeListOut)
def list_my_resumes(
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: List all uploaded resumes."""
    candidate = current_user.candidate_profile
    if not candidate:
        return ResumeListOut(total=0, items=[])

    resumes = db.query(Resume).filter(
        Resume.candidate_id == candidate.id
    ).order_by(Resume.created_at.desc()).all()

    items = [ResumeOut.model_validate(r) for r in resumes]
    return ResumeListOut(total=len(items), items=items)

@router.delete("/resumes/{resume_id}", status_code=status.HTTP_200_OK)
def delete_resume(
    resume_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Delete a previously uploaded resume and its stored file."""
    candidate = current_user.candidate_profile
    resume = db.query(Resume).filter(
        Resume.id == resume_id,
        Resume.candidate_id == candidate.id
    ).first()

    if not resume:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Resume not found")

    # Remove physical file if exists
    if os.path.exists(resume.file_path):
        try:
            os.remove(resume.file_path)
        except Exception:
            pass

    db.delete(resume)
    db.commit()
    return {"message": "Resume and extracted data deleted successfully."}

@router.post("/analyze-general/{resume_id}", response_model=AtsReportOut)
def analyze_general(
    resume_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """
    Candidate: Run standalone ATS evaluation on resume:
    Overall quality score, extracted skills, formatting feedback, strengths, and weaknesses.
    """
    candidate = current_user.candidate_profile
    resume = db.query(Resume).filter(
        Resume.id == resume_id,
        Resume.candidate_id == candidate.id
    ).first()

    if not resume:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Resume not found")

    analysis_res = analyze_general_resume(resume.raw_extracted_text or "")

    analysis = ResumeAnalysis(
        resume_id=resume.id,
        job_id=None,
        overall_score=analysis_res["overall_score"],
        skill_match_score=None,
        experience_score=None,
        keyword_score=None,
        matched_skills=[],
        missing_skills=[],
        extracted_skills=analysis_res["extracted_skills"],
        extracted_experience_years=analysis_res["extracted_experience_years"],
        improvement_suggestions=analysis_res["improvement_suggestions"],
        strengths=analysis_res["strengths"],
        weaknesses=analysis_res["weaknesses"]
    )
    db.add(analysis)
    db.commit()
    db.refresh(analysis)

    return AtsReportOut.model_validate(analysis)

@router.post("/analyze-match", response_model=AtsReportOut)
def analyze_job_match(
    req: AtsMatchRequest,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """
    Candidate: Compare resume against an active job posting OR a custom pasted job description.
    Generates explainable compatibility score, matched/missing skills, and tailored suggestions.
    """
    candidate = current_user.candidate_profile
    resume = db.query(Resume).filter(
        Resume.id == req.resume_id,
        Resume.candidate_id == candidate.id
    ).first()

    if not resume:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Resume not found")

    job_desc = req.job_description or ""
    req_skills = []
    min_exp = req.min_experience_years or 0
    job_id = None

    if req.job_id:
        job = db.query(Job).filter(Job.id == req.job_id).first()
        if not job:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Job position not found")
        job_id = job.id
        job_desc = f"{job.title}\n{job.description}\n{job.requirements or ''}"
        req_skills = [s.skill_name for s in job.job_skills]
        min_exp = job.min_experience_years

    if not job_desc.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Either a valid 'job_id' or a 'job_description' text must be provided."
        )

    match_res = analyze_compatibility(
        resume_text=resume.raw_extracted_text or "",
        job_description=job_desc,
        job_required_skills=req_skills,
        min_experience_years=min_exp
    )

    analysis = ResumeAnalysis(
        resume_id=resume.id,
        job_id=job_id,
        overall_score=match_res["overall_score"],
        skill_match_score=match_res["skill_match_score"],
        experience_score=match_res["experience_score"],
        keyword_score=match_res["keyword_score"],
        matched_skills=match_res["matched_skills"],
        missing_skills=match_res["missing_skills"],
        extracted_skills=match_res["extracted_skills"],
        extracted_experience_years=match_res["extracted_experience_years"],
        improvement_suggestions=match_res["improvement_suggestions"],
        strengths=match_res["strengths"],
        weaknesses=match_res["weaknesses"]
    )
    db.add(analysis)
    db.commit()
    db.refresh(analysis)

    return AtsReportOut.model_validate(analysis)

@router.get("/history", response_model=AtsHistoryListOut)
def get_ats_history(
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: View past resume analysis reports."""
    candidate = current_user.candidate_profile
    if not candidate:
        return AtsHistoryListOut(total=0, items=[])

    resume_ids = [r.id for r in db.query(Resume.id).filter(Resume.candidate_id == candidate.id).all()]
    if not resume_ids:
        return AtsHistoryListOut(total=0, items=[])

    reports = db.query(ResumeAnalysis).filter(
        ResumeAnalysis.resume_id.in_(resume_ids)
    ).order_by(ResumeAnalysis.created_at.desc()).all()

    items = [AtsReportOut.model_validate(r) for r in reports]
    return AtsHistoryListOut(total=len(items), items=items)

@router.get("/history/{report_id}", response_model=AtsReportOut)
def get_ats_report(
    report_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Retrieve an individual ATS report."""
    report = db.query(ResumeAnalysis).filter(ResumeAnalysis.id == report_id).first()
    if not report or report.resume.candidate_id != current_user.candidate_profile.id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Report not found")

    return AtsReportOut.model_validate(report)

@router.delete("/history/{report_id}", status_code=status.HTTP_200_OK)
def delete_ats_report(
    report_id: str,
    current_user: User = Depends(require_role(UserRole.CANDIDATE.value)),
    db: Session = Depends(get_db)
):
    """Candidate: Delete an individual ATS analysis record."""
    report = db.query(ResumeAnalysis).filter(ResumeAnalysis.id == report_id).first()
    if not report or report.resume.candidate_id != current_user.candidate_profile.id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Report not found")

    db.delete(report)
    db.commit()
    return {"message": "Report deleted successfully."}
