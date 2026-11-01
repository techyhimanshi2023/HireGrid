package com.hiregrid.app.data.model

import com.google.gson.annotations.SerializedName

data class CompanyBriefDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("website") val website: String? = null
)

data class JobDto(
    @SerializedName("id") val id: String,
    @SerializedName("company_id") val companyId: String,
    @SerializedName("posted_by_user_id") val postedByUserId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("responsibilities") val responsibilities: String? = null,
    @SerializedName("requirements") val requirements: String? = null,
    @SerializedName("job_type") val jobType: String,
    @SerializedName("workplace_type") val workplaceType: String,
    @SerializedName("location") val location: String? = null,
    @SerializedName("min_experience_years") val minExperienceYears: Int = 0,
    @SerializedName("max_experience_years") val maxExperienceYears: Int? = null,
    @SerializedName("min_salary") val minSalary: Double? = null,
    @SerializedName("max_salary") val maxSalary: Double? = null,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("vacancies") val vacancies: Int = 1,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("skills") val skills: List<String> = emptyList(),
    @SerializedName("company") val company: CompanyBriefDto? = null,
    @SerializedName("applicant_count") val applicantCount: Int = 0,
    @SerializedName("is_saved") val isSaved: Boolean = false
)

data class JobListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<JobDto>
)

data class JobCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("responsibilities") val responsibilities: String? = null,
    @SerializedName("requirements") val requirements: String? = null,
    @SerializedName("job_type") val jobType: String = "full_time",
    @SerializedName("workplace_type") val workplaceType: String = "remote",
    @SerializedName("location") val location: String = "Remote",
    @SerializedName("min_experience_years") val minExperienceYears: Int = 0,
    @SerializedName("min_salary") val minSalary: Double? = null,
    @SerializedName("max_salary") val maxSalary: Double? = null,
    @SerializedName("vacancies") val vacancies: Int = 1,
    @SerializedName("required_skills") val requiredSkills: List<String> = emptyList()
)
