package com.hiregrid.app.data.model

import com.google.gson.annotations.SerializedName

data class ResumeDto(
    @SerializedName("id") val id: String,
    @SerializedName("candidate_id") val candidateId: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_mime_type") val fileMimeType: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("created_at") val createdAt: String
)

data class ResumeListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<ResumeDto>
)

data class AtsMatchRequestDto(
    @SerializedName("resume_id") val resumeId: String,
    @SerializedName("job_id") val jobId: String? = null,
    @SerializedName("job_title") val jobTitle: String? = null,
    @SerializedName("job_description") val jobDescription: String? = null,
    @SerializedName("min_experience_years") val minExperienceYears: Int = 0
)

data class AtsReportDto(
    @SerializedName("id") val id: String,
    @SerializedName("resume_id") val resumeId: String,
    @SerializedName("job_id") val jobId: String? = null,
    @SerializedName("overall_score") val overallScore: Double,
    @SerializedName("skill_match_score") val skillMatchScore: Double? = null,
    @SerializedName("experience_score") val experienceScore: Double? = null,
    @SerializedName("keyword_score") val keywordScore: Double? = null,
    @SerializedName("matched_skills") val matchedSkills: List<String> = emptyList(),
    @SerializedName("missing_skills") val missingSkills: List<String> = emptyList(),
    @SerializedName("extracted_skills") val extractedSkills: List<String> = emptyList(),
    @SerializedName("extracted_experience_years") val extractedExperienceYears: Double? = null,
    @SerializedName("improvement_suggestions") val improvementSuggestions: List<String> = emptyList(),
    @SerializedName("strengths") val strengths: List<String> = emptyList(),
    @SerializedName("weaknesses") val weaknesses: List<String> = emptyList(),
    @SerializedName("created_at") val createdAt: String
)

data class AtsHistoryResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<AtsReportDto>
)
