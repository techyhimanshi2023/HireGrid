package com.hiregrid.app.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.ui.components.HireGridButton
import com.hiregrid.app.core.ui.components.HireGridTextField
import com.hiregrid.app.core.ui.components.RoleSelectionCard

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (role: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val fullNameError by viewModel.fullNameError.collectAsState()
    val companyNameError by viewModel.companyNameError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onRegisterSuccess(state.authData.role)
                viewModel.resetState()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Join HireGrid",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Choose your role and create an account to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Step 1: Select Role
            Text(
                text = "I am registering as:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            RoleSelectionCard(
                title = "Job Seeker / Candidate",
                description = "Discover jobs, upload resume & analyze ATS compatibility",
                icon = Icons.Default.Search,
                isSelected = selectedRole == "candidate",
                onClick = { viewModel.onRoleSelected("candidate") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            RoleSelectionCard(
                title = "Employer / Recruiter",
                description = "Post job vacancies, review applicants & hire top talent",
                icon = Icons.Default.Business,
                isSelected = selectedRole == "employer",
                onClick = { viewModel.onRoleSelected("employer") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step 2: Information Fields
            HireGridTextField(
                value = fullName,
                onValueChange = { viewModel.fullName.value = it },
                label = "Full Name",
                placeholder = "John Doe",
                leadingIcon = Icons.Default.Person,
                errorMessage = fullNameError
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = selectedRole == "employer") {
                Column {
                    HireGridTextField(
                        value = companyName,
                        onValueChange = { viewModel.companyName.value = it },
                        label = "Company Name",
                        placeholder = "Acme Technologies Inc.",
                        leadingIcon = Icons.Default.Business,
                        errorMessage = companyNameError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            HireGridTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                label = "Email Address",
                placeholder = "john@example.com",
                leadingIcon = Icons.Default.Email,
                errorMessage = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HireGridTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = "Password",
                placeholder = "At least 6 characters",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                errorMessage = passwordError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(28.dp))

            HireGridButton(
                text = "Create Account",
                onClick = { viewModel.register() },
                isLoading = uiState is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
