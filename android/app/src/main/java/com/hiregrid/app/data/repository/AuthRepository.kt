package com.hiregrid.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.datastore.SessionManager
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    fun register(
        email: String,
        password: String,
        role: String,
        fullName: String,
        companyName: String? = null
    ): Flow<NetworkResult<AuthResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.register(
                RegisterRequest(
                    email = email.trim(),
                    password = password,
                    role = role,
                    fullName = fullName.trim(),
                    companyName = companyName?.trim()
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                sessionManager.saveSession(
                    token = authData.accessToken,
                    userId = authData.userId,
                    email = authData.email,
                    role = authData.role,
                    fullName = authData.fullName
                )
                emit(NetworkResult.Success(authData))
            } else {
                val errorMsg = parseErrorBody(response.errorBody()?.string())
                emit(NetworkResult.Error(errorMsg, response.code()))
            }
        } catch (e: IOException) {
            emit(NetworkResult.Error("Network error. Please check your connection to the server."))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Unexpected error occurred during registration."))
        }
    }.flowOn(Dispatchers.IO)

    fun login(email: String, password: String): Flow<NetworkResult<AuthResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.login(LoginRequest(email.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                sessionManager.saveSession(
                    token = authData.accessToken,
                    userId = authData.userId,
                    email = authData.email,
                    role = authData.role,
                    fullName = authData.fullName
                )
                emit(NetworkResult.Success(authData))
            } else {
                val errorMsg = parseErrorBody(response.errorBody()?.string())
                emit(NetworkResult.Error(errorMsg, response.code()))
            }
        } catch (e: IOException) {
            emit(NetworkResult.Error("Network error. Please check your connection to the server."))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Unexpected error occurred during login."))
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentUser(): Flow<NetworkResult<UserDto>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                val errorMsg = parseErrorBody(response.errorBody()?.string())
                emit(NetworkResult.Error(errorMsg, response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Failed to fetch user profile."))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun logout() {
        sessionManager.clearSession()
    }

    fun deleteAccount(): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) {
                sessionManager.clearSession()
                emit(NetworkResult.Success("Account deleted successfully."))
            } else {
                emit(NetworkResult.Error("Failed to delete account."))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.localizedMessage ?: "Error deleting account."))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseErrorBody(errorString: String?): String {
        if (errorString.isNullOrEmpty()) return "An error occurred."
        return try {
            val json = Gson().fromJson(errorString, JsonObject::class.java)
            if (json.has("detail")) {
                val detail = json.get("detail")
                if (detail.isJsonPrimitive) detail.asString else detail.toString()
            } else {
                errorString
            }
        } catch (e: Exception) {
            errorString
        }
    }
}
