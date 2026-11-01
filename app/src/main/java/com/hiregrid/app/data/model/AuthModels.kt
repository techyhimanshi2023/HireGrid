package com.hiregrid.app.data.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("company_name") val companyName: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("full_name") val fullName: String
)

data class CandidateProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("years_of_experience") val yearsOfExperience: Int = 0,
    @SerializedName("education_level") val educationLevel: String? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String? = null,
    @SerializedName("github_url") val githubUrl: String? = null
)

data class EmployerProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("designation") val designation: String? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    @SerializedName("company_id") val companyId: String? = null,
    @SerializedName("company_name") val companyName: String? = null
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("candidate_profile") val candidateProfile: CandidateProfileDto? = null,
    @SerializedName("employer_profile") val employerProfile: EmployerProfileDto? = null
)
