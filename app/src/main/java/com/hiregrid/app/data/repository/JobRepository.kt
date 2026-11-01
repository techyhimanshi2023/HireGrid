package com.hiregrid.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.model.CandidateDashboardResponse
import com.hiregrid.app.data.model.JobDto
import com.hiregrid.app.data.model.JobListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class JobRepository(private val apiService: ApiService) {

    fun listJobs(
        query: String? = null,
        location: String? = null,
        jobType: String? = null,
        workplaceType: String? = null,
        minSalary: Double? = null,
        maxExperience: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Flow<NetworkResult<JobListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.listJobs(
                query = query,
                location = location,
                jobType = jobType,
                workplaceType = workplaceType,
                minSalary = minSalary,
                maxExperience = maxExperience,
                limit = limit,
                offset = offset
            )
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load jobs."))
        }
    }.flowOn(Dispatchers.IO)

    fun getJobById(jobId: String): Flow<NetworkResult<JobDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getJobById(jobId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load job details."))
        }
    }.flowOn(Dispatchers.IO)

    fun toggleSaveJob(jobId: String): Flow<NetworkResult<Boolean>> = flow {
        try {
            val response = apiService.toggleSaveJob(jobId)
            if (response.isSuccessful && response.body() != null) {
                val isSaved = response.body()!!["is_saved"] as? Boolean ?: false
                emit(NetworkResult.Success(isSaved))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to bookmark job."))
        }
    }.flowOn(Dispatchers.IO)

    fun getSavedJobs(): Flow<NetworkResult<JobListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getSavedJobs()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load saved jobs."))
        }
    }.flowOn(Dispatchers.IO)

    fun getCandidateDashboard(): Flow<NetworkResult<CandidateDashboardResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getCandidateDashboard()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load candidate dashboard."))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseError(errorString: String?): String {
        if (errorString.isNullOrEmpty()) return "An error occurred."
        return try {
            val json = Gson().fromJson(errorString, JsonObject::class.java)
            if (json.has("detail")) json.get("detail").asString else errorString
        } catch (e: Exception) {
            errorString
        }
    }
}
