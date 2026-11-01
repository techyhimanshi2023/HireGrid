package com.hiregrid.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AtsRepository(private val apiService: ApiService) {

    fun uploadResume(uri: Uri, context: Context): Flow<NetworkResult<ResumeDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val contentResolver = context.contentResolver
            var fileName = "resume.pdf"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/pdf"
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not read selected document.")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val response = apiService.uploadResume(filePart)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to upload resume document."))
        }
    }.flowOn(Dispatchers.IO)

    fun getMyResumes(): Flow<NetworkResult<ResumeListResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.listMyResumes()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to fetch resumes."))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteResume(resumeId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.deleteResume(resumeId)
            if (response.isSuccessful) {
                emit(NetworkResult.Success("Resume deleted successfully."))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to delete resume."))
        }
    }.flowOn(Dispatchers.IO)

    fun analyzeGeneral(resumeId: String): Flow<NetworkResult<AtsReportDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.analyzeGeneral(resumeId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to analyze resume."))
        }
    }.flowOn(Dispatchers.IO)

    fun analyzeJobMatch(request: AtsMatchRequestDto): Flow<NetworkResult<AtsReportDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.analyzeJobMatch(request)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to compute compatibility score."))
        }
    }.flowOn(Dispatchers.IO)

    fun getAtsHistory(): Flow<NetworkResult<AtsHistoryResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getAtsHistory()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to fetch analysis history."))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteAtsReport(reportId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.deleteAtsReport(reportId)
            if (response.isSuccessful) {
                emit(NetworkResult.Success("Report deleted successfully."))
            } else {
                emit(NetworkResult.Error(parseError(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to delete report."))
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
