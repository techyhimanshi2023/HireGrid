package com.hiregrid.app.core.network

import com.hiregrid.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Authentication & User ---
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @DELETE("users/me")
    suspend fun deleteAccount(): Response<Map<String, String>>

    // --- Jobs Marketplace ---
    @GET("jobs")
    suspend fun listJobs(
        @Query("q") query: String? = null,
        @Query("location") location: String? = null,
        @Query("job_type") jobType: String? = null,
        @Query("workplace_type") workplaceType: String? = null,
        @Query("min_salary") minSalary: Double? = null,
        @Query("max_experience") maxExperience: Int? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<JobListResponse>

    @GET("jobs/{id}")
    suspend fun getJobById(@Path("id") jobId: String): Response<JobDto>

    @POST("jobs")
    suspend fun createJob(@Body request: JobCreateRequest): Response<JobDto>

    @GET("jobs/employer/my-jobs")
    suspend fun getEmployerJobs(): Response<JobListResponse>

    @PATCH("jobs/{id}/status")
    suspend fun updateJobStatus(
        @Path("id") jobId: String,
        @Query("status_val") status: String
    ): Response<JobDto>

    @DELETE("jobs/{id}")
    suspend fun deleteJob(@Path("id") jobId: String): Response<Map<String, String>>

    // --- Applications ---
    @POST("applications/apply")
    suspend fun applyToJob(@Body request: ApplicationCreateRequest): Response<ApplicationDto>

    @GET("applications/my-applications")
    suspend fun getMyApplications(): Response<ApplicationListResponse>

    @PATCH("applications/{id}/withdraw")
    suspend fun withdrawApplication(@Path("id") applicationId: String): Response<ApplicationDto>

    @GET("applications/job/{id}")
    suspend fun getJobApplicants(@Path("id") jobId: String): Response<ApplicationListResponse>

    @PATCH("applications/{id}/status")
    suspend fun updateApplicationStatus(
        @Path("id") applicationId: String,
        @Body request: ApplicationStatusUpdateRequest
    ): Response<ApplicationDto>

    // --- Candidate Dashboards & Bookmarks ---
    @POST("candidates/saved-jobs/{id}")
    suspend fun toggleSaveJob(@Path("id") jobId: String): Response<Map<String, Any>>

    @GET("candidates/saved-jobs")
    suspend fun getSavedJobs(): Response<JobListResponse>

    @GET("candidates/dashboard")
    suspend fun getCandidateDashboard(): Response<CandidateDashboardResponse>

    // --- Employer Dashboard ---
    @GET("employers/dashboard")
    suspend fun getEmployerDashboard(): Response<EmployerDashboardResponse>

    // --- ATS Resume Analyzer ---
    @Multipart
    @POST("ats/upload-resume")
    suspend fun uploadResume(@Part file: MultipartBody.Part): Response<ResumeDto>

    @GET("ats/resumes")
    suspend fun listMyResumes(): Response<ResumeListResponse>

    @DELETE("ats/resumes/{id}")
    suspend fun deleteResume(@Path("id") resumeId: String): Response<Map<String, String>>

    @POST("ats/analyze-general/{id}")
    suspend fun analyzeGeneral(@Path("id") resumeId: String): Response<AtsReportDto>

    @POST("ats/analyze-match")
    suspend fun analyzeJobMatch(@Body request: AtsMatchRequestDto): Response<AtsReportDto>

    @GET("ats/history")
    suspend fun getAtsHistory(): Response<AtsHistoryResponse>

    @GET("ats/history/{id}")
    suspend fun getAtsReportById(@Path("id") reportId: String): Response<AtsReportDto>

    @DELETE("ats/history/{id}")
    suspend fun deleteAtsReport(@Path("id") reportId: String): Response<Map<String, String>>
}
