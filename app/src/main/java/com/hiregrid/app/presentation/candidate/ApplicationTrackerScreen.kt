package com.hiregrid.app.presentation.candidate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.components.StatusBadge
import com.hiregrid.app.core.ui.theme.CardBorderLight
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.core.ui.theme.Success
import com.hiregrid.app.data.model.ApplicationDto

@Composable
fun ApplicationTrackerScreen(
    viewModel: CandidateViewModel,
    onNavigateToJobDetail: (jobId: String) -> Unit
) {
    val applicationsState by viewModel.applicationsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApplications()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "My Applications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            when (val state = applicationsState) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NetworkResult.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadApplications() }) { Text("Retry") }
                        }
                    }
                }
                is NetworkResult.Success -> {
                    val apps = state.data.items

                    if (apps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No applications submitted yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Explore vacancies and apply to track your hiring progress here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(apps, key = { it.id }) { application ->
                                ApplicationCard(
                                    application = application,
                                    onWithdrawClick = { viewModel.withdrawApplication(application.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: ApplicationDto,
    onWithdrawClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        border = BorderStroke(1.dp, CardBorderLight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Job Title and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.job?.title ?: "Position",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = application.job?.company?.name ?: "Company",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                StatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Pipeline Progress Tracker
            PipelineProgressTracker(currentStatus = application.status)

            Spacer(modifier = Modifier.height(16.dp))

            // Status Timeline
            if (application.statusHistory.isNotEmpty()) {
                val latestUpdate = application.statusHistory.last()
                Surface(
                    shape = Shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Latest: ${latestUpdate.notes ?: "Stage updated to ${latestUpdate.toStatus}"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Withdraw Action
            if (application.status.lowercase() !in listOf("selected", "rejected", "withdrawn")) {
                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium
                ) {
                    Text("Withdraw Application")
                }
            }
        }
    }
}

@Composable
private fun PipelineProgressTracker(currentStatus: String) {
    val stages = listOf("Applied", "Review", "Shortlist", "Interview", "Selected")
    val currentStep = when (currentStatus.lowercase()) {
        "applied" -> 0
        "under_review" -> 1
        "shortlisted" -> 2
        "interview" -> 3
        "selected" -> 4
        "rejected", "withdrawn" -> -1
        else -> 0
    }

    val isTerminalNegative = currentStatus.lowercase() in listOf("rejected", "withdrawn")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        stages.forEachIndexed { index, label ->
            val isPassed = index <= currentStep && !isTerminalNegative
            val isCurrent = index == currentStep && !isTerminalNegative

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when {
                                isTerminalNegative && index == 0 -> MaterialTheme.colorScheme.error
                                isPassed -> Success
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (isTerminalNegative && index == 0) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPassed || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < stages.size - 1) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    color = if (index < currentStep && !isTerminalNegative) Success else MaterialTheme.colorScheme.outlineVariant,
                    thickness = 2.dp
                )
            }
        }
    }
}
