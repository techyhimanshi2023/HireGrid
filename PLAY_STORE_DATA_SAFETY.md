# Google Play Console — Data Safety Form Guide for HireGrid

When submitting **HireGrid** to the Google Play Console, Google requires developers to complete the **Data Safety** questionnaire. This guide provides exact, compliant answers based on the HireGrid architecture.

---

## 1. Overview & Data Collection Summary

| Question | Answer | Reason / Explanation |
|---|---|---|
| **Does your app collect or share any of the required user data types?** | **Yes** | The app collects user account details and uploaded resume documents. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All network communications use HTTPS with TLS 1.3 encryption. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | In-app self-service account deletion permanently purges all user data and resumes. |

---

## 2. Data Types Breakdown

### A. Personal Info
#### 1. Name
- **Collected?** Yes
- **Shared?** No (Shared only with the specific employer when candidate applies)
- **Ephemeral?** No (Stored in database for profile display)
- **Required or Optional?** Required for registration
- **Purposes:**
  - `App functionality`
  - `Account management`

#### 2. Email Address
- **Collected?** Yes
- **Shared?** No
- **Ephemeral?** No (Stored as unique account identifier)
- **Required or Optional?** Required
- **Purposes:**
  - `App functionality`
  - `Account management`
  - `Developer communications`

---

### B. Files and Documents
#### 1. Files and Docs (Resumes / CVs)
- **Collected?** Yes
- **Shared?** Yes — Shared **only** with authorized employers when the candidate explicitly submits an application to a vacancy. Not shared with third parties or data brokers.
- **Ephemeral?** No (Stored securely until user deletes document or deletes account)
- **Required or Optional?** Optional (Required only when using the ATS analyzer or applying to jobs)
- **Purposes:**
  - `App functionality` (Resume parsing, skill matching, and application delivery)

---

## 3. Security Practices

| Item | Details |
|---|---|
| **Encryption in Transit** | Data is transferred over secure HTTPS channels with TLS 1.3 protocol. |
| **Account Deletion URL / In-App Feature** | Users can delete their account directly within the app: `Profile Screen -> Delete Account & Personal Data`. The backend immediately purges the user, profile, resumes, and application records (`DELETE /api/v1/users/me`). |
| **Privacy Policy URL** | Host `PRIVACY_POLICY.md` on your web domain (e.g. `https://hiregrid.com/privacy`) and paste the link into the Play Console store listing. |

---

## 4. App Access & Credentials for Google Reviewers

Google Play reviewers require test credentials to review authenticated apps:
- **Test Candidate Account:**
  - **Email:** `google.review.candidate@hiregrid.com`
  - **Password:** `ReviewPass2026!`
- **Test Employer Account:**
  - **Email:** `google.review.employer@hiregrid.com`
  - **Password:** `ReviewPass2026!`
