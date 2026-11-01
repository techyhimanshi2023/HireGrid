import re
import os
from typing import List, Dict, Set, Tuple, Any, Optional
import io
import pdfplumber
import docx

# Comprehensive dictionary of technical and soft skills (free, local, no external APIs needed)
SKILL_TAXONOMY = {
    # Mobile Development
    "android": "Android",
    "kotlin": "Kotlin",
    "jetpack compose": "Jetpack Compose",
    "compose": "Jetpack Compose",
    "java": "Java",
    "swift": "Swift",
    "ios": "iOS",
    "flutter": "Flutter",
    "dart": "Dart",
    "react native": "React Native",
    "coroutine": "Coroutines",
    "coroutines": "Coroutines",
    "stateflow": "StateFlow",
    "retrofit": "Retrofit",
    "room database": "Room",
    "room": "Room",
    "mvvm": "MVVM",
    "mvi": "MVI",
    "dagger hilt": "Dagger Hilt",
    "hilt": "Dagger Hilt",
    "koin": "Koin",
    "viewmodel": "ViewModel",

    # Backend & APIs
    "python": "Python",
    "fastapi": "FastAPI",
    "django": "Django",
    "flask": "Flask",
    "node.js": "Node.js",
    "nodejs": "Node.js",
    "express": "Express.js",
    "express.js": "Express.js",
    "spring": "Spring Boot",
    "spring boot": "Spring Boot",
    "golang": "Go",
    "go": "Go",
    "c#": "C#",
    ".net": ".NET",
    "rest": "REST APIs",
    "rest api": "REST APIs",
    "rest apis": "REST APIs",
    "restful": "REST APIs",
    "graphql": "GraphQL",
    "microservices": "Microservices",
    "grpc": "gRPC",

    # Web & Frontend
    "javascript": "JavaScript",
    "typescript": "TypeScript",
    "react": "React",
    "react.js": "React",
    "vue": "Vue.js",
    "vue.js": "Vue.js",
    "angular": "Angular",
    "next.js": "Next.js",
    "html": "HTML",
    "html5": "HTML5",
    "css": "CSS",
    "css3": "CSS3",
    "tailwind": "Tailwind CSS",
    "tailwind css": "Tailwind CSS",
    "bootstrap": "Bootstrap",

    # Database & Storage
    "sql": "SQL",
    "postgresql": "PostgreSQL",
    "postgres": "PostgreSQL",
    "mysql": "MySQL",
    "sqlite": "SQLite",
    "mongodb": "MongoDB",
    "redis": "Redis",
    "sqlalchemy": "SQLAlchemy",
    "firebase": "Firebase",

    # DevOps & Infrastructure
    "git": "Git",
    "github": "GitHub",
    "gitlab": "GitLab",
    "docker": "Docker",
    "kubernetes": "Kubernetes",
    "ci/cd": "CI/CD",
    "aws": "AWS",
    "amazon web services": "AWS",
    "gcp": "Google Cloud",
    "google cloud": "Google Cloud",
    "azure": "Azure",
    "linux": "Linux",
    "nginx": "Nginx",

    # Testing & Architecture
    "unit testing": "Unit Testing",
    "junit": "JUnit",
    "pytest": "PyTest",
    "espresso": "Espresso",
    "clean architecture": "Clean Architecture",
    "solid": "SOLID Principles",
    "design patterns": "Design Patterns",

    # Soft Skills & Management
    "agile": "Agile",
    "scrum": "Scrum",
    "leadership": "Leadership",
    "communication": "Communication",
    "teamwork": "Teamwork",
    "problem solving": "Problem Solving",
    "collaboration": "Collaboration",
    "mentoring": "Mentoring",
    "time management": "Time Management"
}

def extract_text_from_pdf(file_bytes: bytes) -> str:
    """Extract readable text from a PDF file using pdfplumber."""
    text_content = []
    with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
        for page in pdf.pages:
            extracted = page.extract_text()
            if extracted:
                text_content.append(extracted)
    return "\n".join(text_content).strip()

def extract_text_from_docx(file_bytes: bytes) -> str:
    """Extract readable text from a DOCX file using python-docx."""
    doc = docx.Document(io.BytesIO(file_bytes))
    paragraphs = [p.text for p in doc.paragraphs if p.text]
    for table in doc.tables:
        for row in table.rows:
            row_text = [cell.text.strip() for cell in row.cells if cell.text.strip()]
            if row_text:
                paragraphs.append(" | ".join(row_text))
    return "\n".join(paragraphs).strip()

def extract_skills(text: str) -> List[str]:
    """Identify technical and soft skills present in the text."""
    lower_text = " " + re.sub(r"[^\w\+\#\./-]", " ", text.lower()) + " "
    found_skills: Set[str] = set()

    for keyword, canonical_name in SKILL_TAXONOMY.items():
        pattern = r"(?<!\w)" + re.escape(keyword) + r"(?!\w)"
        if re.search(pattern, lower_text):
            found_skills.add(canonical_name)

    return sorted(list(found_skills))

def extract_experience_years(text: str) -> float:
    """Detect total years of experience indicated in the resume."""
    patterns = [
        r"(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)(?:\s+of)?\s+(?:experience|exp)",
        r"(?:experience|exp)(?:\s+of)?\s*:?\s*(\d+(?:\.\d+)?)\+?\s*(?:years?|yrs?)",
        r"over\s+(\d+(?:\.\d+)?)\s+years?",
        r"(\d+)\+?\s+years?\s+in\s+[a-zA-Z]+"
    ]
    max_years = 0.0
    for pat in patterns:
        matches = re.findall(pat, text, re.IGNORECASE)
        for m in matches:
            try:
                val = float(m)
                if val > max_years and val <= 40:
                    max_years = val
            except ValueError:
                continue
    return max_years

def extract_education(text: str) -> List[str]:
    """Detect degree or educational qualifications mentioned."""
    degrees = []
    edu_patterns = [
        (r"\b(Ph\.?D|Doctorate)\b", "PhD / Doctorate"),
        (r"\b(Master(?:'s)?|M\.?S|M\.?Tech|MBA|MCA)\b", "Master's Degree"),
        (r"\b(Bachelor(?:'s)?|B\.?S|B\.?E|B\.?Tech|BCA|BBA)\b", "Bachelor's Degree"),
        (r"\b(Associate Degree|Diploma)\b", "Associate / Diploma")
    ]
    for pat, label in edu_patterns:
        if re.search(pat, text, re.IGNORECASE):
            if label not in degrees:
                degrees.append(label)
    return degrees

def analyze_general_resume(raw_text: str) -> Dict[str, Any]:
    """
    Perform standalone ATS quality analysis:
    - Overall resume score (0-100)
    - Extracted skills
    - Detected education & experience
    - Strengths, Weaknesses, and Actionable Suggestions
    """
    skills = extract_skills(raw_text)
    exp_years = extract_experience_years(raw_text)
    education = extract_education(raw_text)
    
    strengths = []
    weaknesses = []
    suggestions = []
    
    score = 50.0  # Base score for valid document

    # Skills evaluation
    if len(skills) >= 8:
        score += 20.0
        strengths.append(f"Strong skill presence: Found {len(skills)} recognizable technical/soft skills.")
    elif len(skills) >= 4:
        score += 12.0
        strengths.append(f"Found {len(skills)} recognizable skills.")
        suggestions.append("Add more specific tools, frameworks, and technologies you have worked with.")
    else:
        score += 5.0
        weaknesses.append("Very few recognizable technical skills detected.")
        suggestions.append("Create a dedicated 'Skills' section listing your programming languages, tools, and platforms.")

    # Experience evaluation
    if exp_years > 0:
        score += 15.0
        strengths.append(f"Clear years of experience stated (~{exp_years:g} years).")
    else:
        weaknesses.append("Could not clearly detect total years of professional experience.")
        suggestions.append("Explicitly state your cumulative experience (e.g. 'Software Engineer with 3+ years of experience').")

    # Education evaluation
    if education:
        score += 10.0
        strengths.append(f"Detected education credentials ({', '.join(education)}).")
    else:
        weaknesses.append("No standard degree or diploma credential detected.")
        suggestions.append("Include an Education section detailing your degree, institution, and graduation year.")

    # Section structure & length check
    word_count = len(raw_text.split())
    if 250 <= word_count <= 1200:
        score += 5.0
        strengths.append(f"Optimal resume length ({word_count} words).")
    elif word_count < 250:
        weaknesses.append("Resume appears too brief or lacking detail.")
        suggestions.append("Expand on project contributions, responsibilities, and key accomplishments.")
    else:
        suggestions.append("Resume is somewhat lengthy; ensure content is concise and focused on high-impact results.")

    # Action verbs check
    action_verbs = ["developed", "built", "designed", "architected", "implemented", "optimized", "managed", "led"]
    found_verbs = [v for v in action_verbs if re.search(r"\b" + v + r"\b", raw_text, re.IGNORECASE)]
    if len(found_verbs) >= 3:
        strengths.append(f"Good use of strong action verbs ({', '.join(found_verbs[:3])}).")
    else:
        suggestions.append("Use strong action verbs (e.g. 'Architected', 'Optimized', 'Implemented') at the start of bullet points.")

    final_score = min(max(round(score, 1), 10.0), 100.0)

    return {
        "overall_score": final_score,
        "extracted_skills": skills,
        "extracted_experience_years": exp_years if exp_years > 0 else None,
        "detected_education": education,
        "strengths": strengths,
        "weaknesses": weaknesses,
        "improvement_suggestions": suggestions
    }

def analyze_compatibility(
    resume_text: str,
    job_description: str,
    job_required_skills: Optional[List[str]] = None,
    min_experience_years: int = 0
) -> Dict[str, Any]:
    """
    Match candidate resume against a specific job opportunity.
    Computes explainable compatibility score:
    - Skill Match Score
    - Experience Score
    - Keyword Coverage Score
    - Matched Skills & Missing Skills
    """
    resume_skills = set(extract_skills(resume_text))
    
    # Determine job required skills: from list or extracted from description
    target_skills_set = set()
    if job_required_skills:
        for s in job_required_skills:
            canonical = SKILL_TAXONOMY.get(s.lower().strip(), s.strip())
            target_skills_set.add(canonical)
    
    # Also extract any skills in the job description text
    desc_skills = set(extract_skills(job_description))
    target_skills_set.update(desc_skills)

    if not target_skills_set:
        target_skills_set = {"General Software Engineering"}

    # Calculate matched and missing skills
    matched_skills = sorted(list(resume_skills.intersection(target_skills_set)))
    missing_skills = sorted(list(target_skills_set.difference(resume_skills)))

    # 1. Skill Match Score (0 - 100)
    skill_ratio = len(matched_skills) / len(target_skills_set) if target_skills_set else 1.0
    skill_score = round(min(skill_ratio * 100.0, 100.0), 1)

    # 2. Experience Score (0 - 100)
    cand_exp = extract_experience_years(resume_text)
    if min_experience_years <= 0:
        exp_score = 100.0
    else:
        exp_ratio = cand_exp / float(min_experience_years)
        exp_score = round(min(exp_ratio * 100.0, 100.0), 1)

    # 3. Keyword / Term Coverage (0 - 100)
    job_words = set(re.findall(r"\b[a-zA-Z]{4,}\b", job_description.lower()))
    resume_words = set(re.findall(r"\b[a-zA-Z]{4,}\b", resume_text.lower()))
    common_words = job_words.intersection(resume_words)
    keyword_score = round((len(common_words) / len(job_words) * 100.0) if job_words else 80.0, 1)

    # 4. Overall Weighted Score
    # Weighting: 50% Skills, 30% Experience, 20% Keyword Coverage
    overall = (0.50 * skill_score) + (0.30 * exp_score) + (0.20 * keyword_score)
    overall_score = round(min(max(overall, 5.0), 100.0), 1)

    strengths = []
    weaknesses = []
    suggestions = []

    if matched_skills:
        strengths.append(f"Matching technical skills: {', '.join(matched_skills[:6])}.")
    if cand_exp >= min_experience_years and min_experience_years > 0:
        strengths.append(f"Meets the minimum experience requirement ({cand_exp:g} yrs vs {min_experience_years} yrs required).")

    if missing_skills:
        weaknesses.append(f"Missing required skills: {', '.join(missing_skills[:5])}.")
        suggestions.append(f"If you have experience with {', '.join(missing_skills[:3])}, highlight relevant projects or coursework in your resume.")
    
    if cand_exp < min_experience_years and min_experience_years > 0:
        weaknesses.append(f"Experience gap: Job requires {min_experience_years} years, detected ~{cand_exp:g} years.")
        suggestions.append("Emphasize relevant open-source projects or internships to offset the experience difference.")

    if not suggestions:
        suggestions.append("Your resume aligns well with this job requirement! Tailor your summary to emphasize this specific role.")

    return {
        "overall_score": overall_score,
        "skill_match_score": skill_score,
        "experience_score": exp_score,
        "keyword_score": keyword_score,
        "matched_skills": matched_skills,
        "missing_skills": missing_skills,
        "extracted_skills": sorted(list(resume_skills)),
        "extracted_experience_years": cand_exp if cand_exp > 0 else None,
        "strengths": strengths,
        "weaknesses": weaknesses,
        "improvement_suggestions": suggestions
    }
