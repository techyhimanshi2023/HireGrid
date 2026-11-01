from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, ConfigDict
from app.schemas.job import JobOut

class ApplicationCreate(BaseModel):
    job_id: str
    resume_id: Optional[str] = None
    cover_note: Optional[str] = None

class ApplicationStatusUpdate(BaseModel):
    status: str = Field(..., description="under_review, shortlisted, interview, rejected, selected")
    notes: Optional[str] = None

class StatusHistoryItem(BaseModel):
    id: str
    from_status: Optional[str] = None
    to_status: str
    notes: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class CandidateBriefOut(BaseModel):
    id: str
    full_name: str
    headline: Optional[str] = None
    location: Optional[str] = None
    years_of_experience: int = 0
    education_level: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class ApplicationOut(BaseModel):
    id: str
    job_id: str
    candidate_id: str
    resume_id: Optional[str] = None
    status: str
    cover_note: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    job: Optional[JobOut] = None
    candidate: Optional[CandidateBriefOut] = None
    status_history: List[StatusHistoryItem] = Field(default_factory=list)

    model_config = ConfigDict(from_attributes=True)

class ApplicationListOut(BaseModel):
    total: int
    items: List[ApplicationOut]
