from pydantic_settings import BaseSettings, SettingsConfigDict
import email_validator
import os

# Disable DNS deliverability checks so email validation is instant and works offline
email_validator.CHECK_DELIVERABILITY = False

class Settings(BaseSettings):
    PROJECT_NAME: str = "HireGrid API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Database
    DATABASE_URL: str = "sqlite:///./hiregrid.db"
    
    # JWT & Security
    SECRET_KEY: str = "hiregrid-super-secret-key-change-in-production-2026"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days for development
    
    # File Storage
    UPLOAD_DIR: str = os.path.join(os.getcwd(), "uploads", "resumes")
    MAX_FILE_SIZE_BYTES: int = 5 * 1024 * 1024  # 5 MB
    
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=True, extra="ignore")

settings = Settings()
