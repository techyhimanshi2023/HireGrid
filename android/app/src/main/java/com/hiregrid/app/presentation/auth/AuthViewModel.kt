package com.hiregrid.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.data.model.AuthResponse
import com.hiregrid.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val authData: AuthResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form Fields
    var email = MutableStateFlow("")
    var password = MutableStateFlow("")
    var fullName = MutableStateFlow("")
    var companyName = MutableStateFlow("")
    var selectedRole = MutableStateFlow("candidate") // "candidate" or "employer"

    // Validation Errors
    var emailError = MutableStateFlow<String?>(null)
    var passwordError = MutableStateFlow<String?>(null)
    var fullNameError = MutableStateFlow<String?>(null)
    var companyNameError = MutableStateFlow<String?>(null)

    fun onRoleSelected(role: String) {
        selectedRole.value = role
        if (role == "candidate") {
            companyName.value = ""
            companyNameError.value = null
        }
    }

    private fun validateLoginForm(): Boolean {
        var isValid = true
        val emailVal = email.value.trim()
        val passVal = password.value

        if (emailVal.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            emailError.value = "Enter a valid email address"
            isValid = false
        } else {
            emailError.value = null
        }

        if (passVal.isEmpty()) {
            passwordError.value = "Password is required"
            isValid = false
        } else {
            passwordError.value = null
        }

        return isValid
    }

    private fun validateRegisterForm(): Boolean {
        var isValid = true
        val emailVal = email.value.trim()
        val passVal = password.value
        val nameVal = fullName.value.trim()
        val roleVal = selectedRole.value
        val compVal = companyName.value.trim()

        if (nameVal.length < 2) {
            fullNameError.value = "Full name must be at least 2 characters"
            isValid = false
        } else {
            fullNameError.value = null
        }

        if (emailVal.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            emailError.value = "Enter a valid email address"
            isValid = false
        } else {
            emailError.value = null
        }

        if (passVal.length < 6) {
            passwordError.value = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError.value = null
        }

        if (roleVal == "employer" && compVal.isEmpty()) {
            companyNameError.value = "Company name is required for employers"
            isValid = false
        } else {
            companyNameError.value = null
        }

        return isValid
    }

    fun login() {
        if (!validateLoginForm()) return

        viewModelScope.launch {
            authRepository.login(email.value, password.value).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = AuthUiState.Loading
                    is NetworkResult.Success -> _uiState.value = AuthUiState.Success(result.data)
                    is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun register() {
        if (!validateRegisterForm()) return

        viewModelScope.launch {
            authRepository.register(
                email = email.value,
                password = password.value,
                role = selectedRole.value,
                fullName = fullName.value,
                companyName = if (selectedRole.value == "employer") companyName.value else null
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = AuthUiState.Loading
                    is NetworkResult.Success -> _uiState.value = AuthUiState.Success(result.data)
                    is NetworkResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
