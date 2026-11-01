package com.hiregrid.app.presentation.employer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.components.HireGridTextField
import com.hiregrid.app.core.ui.components.StatusBadge
import com.hiregrid.app.core.ui.theme.CardBorderLight
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.data.model.ApplicationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantReviewScreen(
    jobId: String,
    viewModel: EmployerViewModel,
    onNavigateBack: () -> Unit
) {
    val applicantsState by viewModel.applicantsState.collectAsState()

    var selectedAppForUpdate by remember { mutableStateOf<ApplicationDto?>(null) }
    var targetStatus by remember { mutableStateOf("shortlisted") }
    var statusNote by remember { mutableStateOf("") }

    LaunchedEffect(jobId) {
        viewModel.loadApplicants(jobId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Applicant Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = applicantsState) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NetworkResult.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadApplicants(jobId) }) { Text("Retry") }
                        }
                    }
                }
                is NetworkResult.Success -> {
                    val applicants = state.data.items

                    if (applicants.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No applications for this vacancy yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Candidates who apply will appear here for review.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(applicants, key = { it.id }) { application ->
                                CandidateApplicationCard(
                                    application = application,
                                    onUpdateStatusClick = {
                                        selectedAppForUpdate = application
                                        targetStatus = "shortlisted"
                                        statusNote = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Status Update Dialog
    selectedAppForUpdate?.let { app ->
        val candidateName = app.candidate?.fullName ?: "Candidate"

        AlertDialog(
            onDismissRequest = { selectedAppForUpdate = null },
            title = { Text("Update Hiring Stage", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Advance $candidateName to next recruitment stage:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val stageOptions = listOf(
                        "under_review" to "Under Review",
                        "shortlisted" to "Shortlisted",
                        "interview" to "Move to Interview",
                        "selected" to "Select Candidate",
                        "rejected" to "Reject"
                    )

                    stageOptions.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = targetStatus == key,
                                onClick = { targetStatus = key }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HireGridTextField(
                        value = statusNote,
                        onValueChange = { statusNote = it },
                        label = "Recruiter Notes",
                        placeholder = "e.g. Cleared technical interview round",
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApplicantStatus(
                            applicationId = app.id,
                            newStatus = targetStatus,
                            notes = statusNote.ifBlank { null },
                            jobId = jobId
                        )
                        selectedAppForUpdate = null
                    }
                ) {
                    Text("Save Stage")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForUpdate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CandidateApplicationCard(
    application: ApplicationDto,
    onUpdateStatusClick: () -> Unit
) {
    val candidate = application.candidate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        border = BorderStroke(1.dp, CardBorderLight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate?.fullName ?: "Candidate Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = candidate?.headline ?: "Candidate Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${candidate?.location ?: "Location not specified"} • ${candidate?.yearsOfExperience ?: 0}+ yrs exp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = application.status)
            }

            if (!application.coverNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = Shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${application.coverNote}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onUpdateStatusClick,
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium
            ) {
                Text("Update Hiring Stage")
            }
        }
    }
}
