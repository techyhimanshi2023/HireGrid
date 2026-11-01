package com.hiregrid.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ApplicationRepository(private val apiService: ApiService) {

    fun applyToJob(
        jobId: String,
        resumeId: String? = null,
        coverNote: String? = null
    ): Flow<NetworkResult<ApplicationDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.applyToJob(
                ApplicationCreateRequest(
                    jobId = jobId,
                    resumeId = resumeId,
                    coverNote = coverNote
                )
            )
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to submit application."))
        }
    }.flowOn(Dispatchers.IO)

    fun getMyApplications(): Flow<NetworkResult<ApplicationListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getMyApplications()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to fetch submitted applications."))
        }
    }.flowOn(Dispatchers.IO)

    fun withdrawApplication(applicationId: String): Flow<NetworkResult<ApplicationDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.withdrawApplication(applicationId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to withdraw application."))
        }
    }.flowOn(Dispatchers.IO)

    fun getJobApplicants(jobId: String): Flow<NetworkResult<ApplicationListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getJobApplicants(jobId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to load applicants."))
        }
    }.flowOn(Dispatchers.IO)

    fun updateApplicationStatus(
        applicationId: String,
        status: String,
        notes: String? = null
    ): Flow<NetworkResult<ApplicationDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.updateApplicationStatus(
                applicationId,
                ApplicationStatusUpdateRequest(status = status, notes = notes)
            )
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to update status."))
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
