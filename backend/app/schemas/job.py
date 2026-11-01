from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, ConfigDict
from decimal import Decimal

class SkillItem(BaseModel):
    skill_name: str
    is_required: bool = True

    model_config = ConfigDict(from_attributes=True)

class JobCreate(BaseModel):
    title: str = Field(..., min_length=3, max_length=255)
    description: str = Field(..., min_length=10)
    responsibilities: Optional[str] = None
    requirements: Optional[str] = None
    job_type: str = Field(default="full_time", description="full_time, part_time, contract, internship")
    workplace_type: str = Field(default="remote", description="remote, hybrid, onsite")
    location: Optional[str] = "Remote"
    min_experience_years: int = Field(default=0, ge=0)
    max_experience_years: Optional[int] = Field(None, ge=0)
    min_salary: Optional[Decimal] = None
    max_salary: Optional[Decimal] = None
    currency: str = "USD"
    vacancies: int = Field(default=1, ge=1)
    application_deadline: Optional[datetime] = None
    required_skills: List[str] = Field(default_factory=list, description="List of required technical & soft skills")

class JobUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    responsibilities: Optional[str] = None
    requirements: Optional[str] = None
    job_type: Optional[str] = None
    workplace_type: Optional[str] = None
    location: Optional[str] = None
    min_experience_years: Optional[int] = None
    max_experience_years: Optional[int] = None
    min_salary: Optional[Decimal] = None
    max_salary: Optional[Decimal] = None
    currency: Optional[str] = None
    vacancies: Optional[int] = None
    application_deadline: Optional[datetime] = None
    status: Optional[str] = None
    required_skills: Optional[List[str]] = None

class CompanyBriefOut(BaseModel):
    id: str
    name: str
    logo_url: Optional[str] = None
    location: Optional[str] = None
    website: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class JobOut(BaseModel):
    id: str
    company_id: str
    posted_by_user_id: str
    title: str
    description: str
    responsibilities: Optional[str] = None
    requirements: Optional[str] = None
    job_type: str
    workplace_type: str
    location: Optional[str] = None
    min_experience_years: int
    max_experience_years: Optional[int] = None
    min_salary: Optional[Decimal] = None
    max_salary: Optional[Decimal] = None
    currency: str
    vacancies: int
    application_deadline: Optional[datetime] = None
    status: str
    created_at: datetime
    updated_at: datetime
    skills: List[str] = Field(default_factory=list)
    company: Optional[CompanyBriefOut] = None
    applicant_count: Optional[int] = 0
    is_saved: Optional[bool] = False

    model_config = ConfigDict(from_attributes=True)

class JobListOut(BaseModel):
    total: int
    items: List[JobOut]
