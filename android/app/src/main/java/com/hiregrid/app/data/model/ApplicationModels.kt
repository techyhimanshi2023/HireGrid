package com.hiregrid.app.data.model

import com.google.gson.annotations.SerializedName

data class ApplicationCreateRequest(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("resume_id") val resumeId: String? = null,
    @SerializedName("cover_note") val coverNote: String? = null
)

data class ApplicationStatusUpdateRequest(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String? = null
)

data class StatusHistoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("from_status") val fromStatus: String? = null,
    @SerializedName("to_status") val toStatus: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class CandidateBriefDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("years_of_experience") val yearsOfExperience: Int = 0
)

data class ApplicationDto(
    @SerializedName("id") val id: String,
    @SerializedName("job_id") val jobId: String,
    @SerializedName("candidate_id") val candidateId: String,
    @SerializedName("resume_id") val resumeId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("cover_note") val coverNote: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("job") val job: JobDto? = null,
    @SerializedName("candidate") val candidate: CandidateBriefDto? = null,
    @SerializedName("status_history") val statusHistory: List<StatusHistoryDto> = emptyList()
)

data class ApplicationListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<ApplicationDto>
)

data class CandidateDashboardResponse(
    @SerializedName("candidate_name") val candidateName: String,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("total_applications") val totalApplications: Int = 0,
    @SerializedName("saved_jobs_count") val savedJobsCount: Int = 0,
    @SerializedName("recent_jobs") val recentJobs: List<JobDto> = emptyList()
)

data class EmployerDashboardResponse(
    @SerializedName("company_name") val companyName: String,
    @SerializedName("total_jobs") val totalJobs: Int = 0,
    @SerializedName("active_jobs") val activeJobs: Int = 0,
    @SerializedName("total_applicants") val totalApplicants: Int = 0,
    @SerializedName("shortlisted") val shortlisted: Int = 0,
    @SerializedName("interview") val interview: Int = 0,
    @SerializedName("selected") val selected: Int = 0,
    @SerializedName("recent_applications") val recentApplications: List<ApplicationDto> = emptyList()
)
