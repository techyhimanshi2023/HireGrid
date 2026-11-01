package com.hiregrid.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.components.HireGridButton
import com.hiregrid.app.core.ui.components.StatusBadge
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.data.model.UserDto
import com.hiregrid.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onLogoutSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var userState by remember { mutableStateOf<NetworkResult<UserDto>>(NetworkResult.Loading) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authRepository.getCurrentUser().collect { result ->
            userState = result
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = userState) {
            is NetworkResult.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NetworkResult.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                authRepository.getCurrentUser().collect { userState = it }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is NetworkResult.Success -> {
                val user = state.data
                val isEmployer = user.role.lowercase() == "employer"
                val fullName = if (isEmployer) {
                    user.employerProfile?.fullName ?: "Employer"
                } else {
                    user.candidateProfile?.fullName ?: "Candidate"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, Shapes.extraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEmployer) Icons.Default.Business else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusBadge(status = user.role)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Profile Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (isEmployer) {
                                val emp = user.employerProfile
                                ProfileDetailRow("Company", emp?.companyName ?: "Not Assigned")
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileDetailRow("Designation", emp?.designation ?: "Recruiter")
                            } else {
                                val cand = user.candidateProfile
                                ProfileDetailRow("Headline", cand?.headline ?: "Software Professional")
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileDetailRow("Location", cand?.location ?: "Remote")
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileDetailRow("Experience", "${cand?.yearsOfExperience ?: 0} Years")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sign Out Button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                authRepository.logout()
                                onLogoutSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = Shapes.medium
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delete Account Button (Google Play Compliance)
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Account & Personal Data")
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Google Play Account Deletion
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete Account permanently?") },
            text = {
                Text("This action is irreversible. All your personal data, job applications, resumes, and saved items will be deleted immediately in accordance with Google Play data safety policies.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            authRepository.deleteAccount().collect { res ->
                                if (res is NetworkResult.Success) {
                                    showDeleteDialog = false
                                    onLogoutSuccess()
                                }
                                isDeleting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Permanently Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
