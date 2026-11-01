# HireGrid – Smart Hiring & AI-Powered Recruitment Platform

HireGrid is a modern, production-oriented recruitment platform connecting job seekers (candidates) and employers (recruiters). It features a native Android application built with **Jetpack Compose (Material 3)** and a **FastAPI** backend featuring a **local, 100% free AI/ATS Resume Analyzer**.

---

## 🚀 Key Highlights & Architecture

- **Dual-Role Experience:** Distinct, role-protected workflows for **Job Seekers** and **Recruiters**.
- **Zero-Cost ATS Resume Engine:** Uses local, open-source NLP (`pdfplumber`, `python-docx`, taxonomy token classification, and cosine similarity) running directly on the backend server. **Zero external paid AI API keys required ($0.00 cost).**
- **5-Stage Hiring Pipeline:** Tracks candidates through `Applied` $\rightarrow$ `Under Review` $\rightarrow$ `Shortlisted` $\rightarrow$ `Interview` $\rightarrow$ `Selected` with full audit history logging.
- **Google Play Store Ready:** Implements scoped storage via Android Storage Access Framework (SAF), encrypted transit (HTTPS/TLS 1.3), transparent Data Safety declarations, and in-app self-service account deletion.

---

## 🛠️ Technology Stack

### Android Application
- **Language & Framework:** Kotlin 1.9.23, Jetpack Compose, Material 3
- **Architecture:** MVVM (Model-View-ViewModel), Unidirectional Data Flow (UDF)
- **State Management:** Kotlin Coroutines & `StateFlow`
- **Networking:** Retrofit 2, OkHttp 4, Gson, `AuthInterceptor` (JWT Bearer injection)
- **Local Persistence:** Android Preferences DataStore
- **Document Selection:** Android Storage Access Framework (SAF)

### Backend API
- **Framework:** Python 3.11+ / 3.14, FastAPI, Pydantic v2
- **Database ORM:** SQLAlchemy 2.0 (PostgreSQL for production, SQLite for turnkey zero-config development)
- **Security:** `bcrypt` password hashing, stateless JSON Web Tokens (JWT)
- **NLP Document Parsing:** `pdfplumber`, `python-docx`
- **Testing:** `pytest` (24 passing automated tests)

---

## 📁 Repository Structure

```
hiregrid/
├── android/                               # Complete Android Studio project
│   ├── app/
│   │   ├── src/main/java/com/hiregrid/app/
│   │   │   ├── core/                      # Design system, theme, DataStore, RetrofitClient
│   │   │   ├── data/                      # DTOs, AuthRepository, JobRepository, AtsRepository
│   │   │   └── presentation/              # Compose screens, ViewModels & NavGraph
│   │   ├── build.gradle.kts
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                               # FastAPI backend service
│   ├── app/
│   │   ├── core/                          # Security (bcrypt, JWT) & RBAC dependency guards
│   │   ├── models/                        # SQLAlchemy relational models (User, Job, App, Resume)
│   │   ├── routers/                       # Auth, Users, Jobs, Applications, Candidates, Employers, ATS
│   │   ├── schemas/                       # Pydantic validation models
│   │   ├── services/                      # Local ATS Resume Parsing & Scoring Engine
│   │   ├── config.py                      # App settings & environment reader
│   │   ├── database.py                    # Database connection manager
│   │   └── main.py                        # FastAPI entry point & CORS configuration
│   ├── tests/                             # Automated test suite
│   ├── requirements.txt                   # Backend dependencies
│   └── .env.example                       # Environment template
│
├── PRIVACY_POLICY.md                      # Google Play compliant privacy policy
├── TERMS_OF_SERVICE.md                    # Platform terms of service
├── PLAY_STORE_DATA_SAFETY.md              # Google Play Data Safety answers guide
├── RELEASE_BUILD_GUIDE.md                 # Signed AAB generation & keystore guide
└── README.md                              # Master project documentation
```

---

## 🏁 Quickstart Guide

### 1. Start the FastAPI Backend

Open a terminal in the project root:

```powershell
# 1. Activate the Python virtual environment
.\backend\venv\Scripts\Activate.ps1

# 2. Launch the backend server with live reload
python -m uvicorn app.main:app --app-dir backend --reload --host 0.0.0.0 --port 8000
```

- **Interactive Swagger Documentation:** Open [http://localhost:8000/docs](http://localhost:8000/docs) in your browser.
- **Health Check:** [http://localhost:8000/health](http://localhost:8000/health)

### 2. Run Backend Test Suite

To verify authentication, marketplace, applications, and the ATS parser:

```powershell
backend\venv\Scripts\pytest.exe backend\tests -v
```

All 24 automated tests will run and pass:
```text
backend/tests/test_auth.py: 10 passed
backend/tests/test_jobs_applications.py: 8 passed
backend/tests/test_ats.py: 6 passed
```

### 3. Open & Run the Android Application

1. Launch **Android Studio**.
2. Select **Open** and choose the `hiregrid/android` folder.
3. Allow Gradle to sync dependencies automatically.
4. Launch an Android Virtual Device (AVD) emulator (API 30+ recommended).
5. Click **Run 'app'** (`Shift + F10`).

> [!TIP]
> **Android Emulator Networking:**  
> The Android emulator communicates with your workstation's `localhost` via the special IP address `10.0.2.2`. The app's [`RetrofitClient.kt`](file:///c:/Users/Himanshi%20Arora/Downloads/hiregrid/android/app/src/main/java/com/hiregrid/app/core/network/RetrofitClient.kt) is pre-configured to `http://10.0.2.2:8000/api/v1/`, so no configuration is required to communicate with your local backend.

---

## 🔒 Security & Google Play Compliance

- **Authentication & RBAC:** Role enforcement is validated on every endpoint on the backend. Client-side changes cannot alter account permissions.
- **Privacy & Storage:** Android Storage Access Framework (SAF) is used for resume selection, preventing unnecessary device-wide storage permissions.
- **Account Deletion:** Users can delete their account directly within the app (`Profile Screen -> Delete Account`), permanently wiping all personal data in compliance with Google Play Data Safety policies.

For publishing details, see [PLAY_STORE_DATA_SAFETY.md](file:///c:/Users/Himanshi%20Arora/Downloads/hiregrid/PLAY_STORE_DATA_SAFETY.md) and [RELEASE_BUILD_GUIDE.md](file:///c:/Users/Himanshi%20Arora/Downloads/hiregrid/RELEASE_BUILD_GUIDE.md).
