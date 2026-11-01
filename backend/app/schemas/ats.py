from datetime import datetime
from typing import Optional, List, Any
from pydantic import BaseModel, Field, ConfigDict
from decimal import Decimal

class ResumeOut(BaseModel):
    id: str
    candidate_id: str
    file_name: str
    file_mime_type: str
    file_size_bytes: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class ResumeListOut(BaseModel):
    total: int
    items: List[ResumeOut]

class AtsMatchRequest(BaseModel):
    resume_id: str
    job_id: Optional[str] = None
    job_title: Optional[str] = None
    job_description: Optional[str] = None
    min_experience_years: Optional[int] = 0

class AtsReportOut(BaseModel):
    id: str
    resume_id: str
    job_id: Optional[str] = None
    overall_score: Decimal
    skill_match_score: Optional[Decimal] = None
    experience_score: Optional[Decimal] = None
    keyword_score: Optional[Decimal] = None
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    extracted_skills: List[str] = Field(default_factory=list)
    extracted_experience_years: Optional[Decimal] = None
    improvement_suggestions: List[str] = Field(default_factory=list)
    strengths: List[str] = Field(default_factory=list)
    weaknesses: List[str] = Field(default_factory=list)
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class AtsHistoryListOut(BaseModel):
    total: int
    items: List[AtsReportOut]
