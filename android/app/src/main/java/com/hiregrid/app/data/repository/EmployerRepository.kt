package com.hiregrid.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.model.EmployerDashboardResponse
import com.hiregrid.app.data.model.JobCreateRequest
import com.hiregrid.app.data.model.JobDto
import com.hiregrid.app.data.model.JobListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class EmployerRepository(private val apiService: ApiService) {

    fun getEmployerDashboard(): Flow<NetworkResult<EmployerDashboardResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getEmployerDashboard()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load recruitment dashboard."))
        }
    }.flowOn(Dispatchers.IO)

    fun getEmployerJobs(): Flow<NetworkResult<JobListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getEmployerJobs()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load posted jobs."))
        }
    }.flowOn(Dispatchers.IO)

    fun createJob(request: JobCreateRequest): Flow<NetworkResult<JobDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.createJob(request)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to create job posting."))
        }
    }.flowOn(Dispatchers.IO)

    fun updateJobStatus(jobId: String, status: String): Flow<NetworkResult<JobDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.updateJobStatus(jobId, status)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to update job status."))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteJob(jobId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.deleteJob(jobId)
            if (response.isSuccessful) {
                emit(NetworkResult.Success("Job deleted successfully."))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to delete job."))
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
