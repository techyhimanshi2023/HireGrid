# Privacy Policy for HireGrid

**Last Updated:** November 1, 2026

**HireGrid** ("we," "our," or "the Platform") is committed to protecting the privacy and security of your personal data. This Privacy Policy outlines our practices regarding the collection, use, disclosure, and protection of information when you access or use the HireGrid mobile application and related backend services.

This policy has been designed in strict accordance with the **Google Play Developer Program Policies (User Data & Data Safety)**, as well as applicable data privacy regulations including GDPR and CCPA.

---

## 1. Information We Collect

### A. Information You Directly Provide
- **Account Credentials:** When you register, we collect your email address, full name, selected account role (`candidate` or `employer`), and a securely hashed password.
- **Candidate Profiles:** Professional headline, summary, location, phone number, years of experience, educational level, and optional links to LinkedIn or GitHub.
- **Employer & Company Information:** Company name, corporate email address, designation, company logo URL, website, location, and corporate description.
- **Resumes and Uploaded Documents:** When candidates use the ATS Resume Analyzer or apply for jobs, we process uploaded documents (PDF and DOCX formats). Text is extracted from these documents solely to evaluate candidate skills, experience duration, and job compatibility.
- **Job Postings & Applications:** Job vacancies published by employers, application cover notes, status history, and bookmarked jobs.

### B. Information Collected Automatically
- **App Diagnostics:** Standard network communication logs, error responses, and service uptime diagnostics required to maintain app reliability. We do not track fine-grained GPS location.

---

## 2. How We Use Your Information

We process personal data strictly for recruitment and application functionality:
1. **Facilitating Hiring Workflows:** Connecting candidates with authorized prospective employers.
2. **AI & ATS Compatibility Analysis:** Extracting technical skills, experience duration, and keyword coverage from uploaded resumes to generate explainable match scores.
3. **Application Tracking:** Recording timeline audit trails (`Applied` $\rightarrow$ `Under Review` $\rightarrow$ `Shortlisted` $\rightarrow$ `Interview` $\rightarrow$ `Selected`) visible to candidates and recruiters.
4. **Security & Authentication:** Verifying role permissions via JSON Web Tokens (JWT) and preventing unauthorized access or duplicate spam applications.

---

## 3. Data Sharing & Disclosure

- **Between Candidates and Authorized Employers:** When a candidate applies for a specific vacancy, their profile details and submitted resume are accessible **only** to the employer managing that specific job posting.
- **No Third-Party Sale of Data:** We do not sell, rent, or monetize your personal data or resume documents to data brokers, advertisers, or third parties.
- **No Cloud AI Data Training:** Uploaded resume text is parsed on isolated servers solely to generate your personal score. It is never used to train third-party public AI models.

---

## 4. Data Security

We implement rigorous technical safeguards to protect your information:
- **Encryption in Transit:** All communications between the Android application and the backend utilize HTTPS with TLS 1.3 encryption.
- **Cryptographic Password Storage:** Passwords are never stored in plain text; they are hashed using the `bcrypt` cryptographic algorithm with unique salts.
- **Protected File Storage:** Uploaded resume documents are stored with randomized UUID filenames outside public web roots and are accessible only to verified, authorized users.
- **Client-Side Keystore:** Mobile sessions and access tokens are stored securely in Android Preferences DataStore backed by the Android Keystore.

---

## 5. User Rights, Account Deletion & Data Retention

In compliance with Google Play Store policies and international data protection laws:
- **Self-Service Account Deletion:** You can permanently delete your account and all associated personal data directly within the app at any time by navigating to **Profile $\rightarrow$ Delete Account & Personal Data**, or via the API endpoint `DELETE /api/v1/users/me`.
- **Immediate Data Purging:** When an account is deleted, all personal records, candidate profiles, uploaded resume files, applications, and ATS analysis reports are permanently and irreversibly purged from our database and disk storage.
- **Resume Document Deletion:** Candidates can delete any individual uploaded resume or past ATS evaluation report directly from the ATS Analyzer section without deleting their entire account.

---

## 6. Algorithmic Transparency & AI Disclosures

- The HireGrid ATS Analyzer provides algorithmic assistance by comparing resume keywords and skill sets against job descriptions.
- **Human-in-the-Loop:** The ATS match score does **not** make automated, binding hiring or rejection decisions. All candidate shortlisting, interview invitations, and hiring selections are conducted solely by human hiring managers.

---

## 7. Contact Us

If you have questions, feedback, or requests regarding this Privacy Policy or your personal data, please contact our Data Protection Team at:
- **Email:** privacy@hiregrid.com
- **Website:** https://hiregrid.com/support
