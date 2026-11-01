from typing import Optional
from pydantic import BaseModel, EmailStr, Field, ConfigDict

class UserRegister(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6, description="Password must be at least 6 characters")
    role: str = Field(..., description="Role must be 'candidate' or 'employer'")
    full_name: str = Field(..., min_length=2, description="Full name of user")
    company_name: Optional[str] = Field(None, description="Company name if registering as employer")

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    email: str
    role: str
    full_name: str

class CandidateProfileOut(BaseModel):
    id: str
    full_name: str
    phone: Optional[str] = None
    headline: Optional[str] = None
    summary: Optional[str] = None
    location: Optional[str] = None
    years_of_experience: int = 0
    education_level: Optional[str] = None
    linkedin_url: Optional[str] = None
    github_url: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class EmployerProfileOut(BaseModel):
    id: str
    full_name: str
    designation: Optional[str] = None
    contact_email: Optional[str] = None
    company_id: Optional[str] = None
    company_name: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class UserOut(BaseModel):
    id: str
    email: str
    role: str
    is_active: bool
    is_verified: bool
    candidate_profile: Optional[CandidateProfileOut] = None
    employer_profile: Optional[EmployerProfileOut] = None

    model_config = ConfigDict(from_attributes=True)
