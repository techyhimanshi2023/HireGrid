package com.hiregrid.app.presentation.ats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.components.HireGridButton
import com.hiregrid.app.core.ui.components.HireGridTextField
import com.hiregrid.app.core.ui.theme.CardBorderLight
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.data.model.JobDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtsAnalyzerScreen(
    viewModel: AtsViewModel
) {
    val context = LocalContext.current
    val resumesState by viewModel.resumesState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    val availableJobsState by viewModel.availableJobsState.collectAsState()

    val selectedResume by viewModel.selectedResume.collectAsState()
    val analysisMode by viewModel.analysisMode.collectAsState()
    val pastedJobDescription by viewModel.pastedJobDescription.collectAsState()
    val selectedJobId by viewModel.selectedJobId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Android Storage Access Framework (SAF) Document Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadResume(it, context)
        }
    }

    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is NetworkResult.Success -> {
                snackbarHostState.showSnackbar("Resume uploaded and parsed successfully!")
                viewModel.resetUploadState()
            }
            is NetworkResult.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUploadState()
            }
            else -> Unit
        }
    }

    // Display Full Report Screen if report is generated
    if (reportState is NetworkResult.Success) {
        val report = (reportState as NetworkResult.Success).data
        AtsReportScreen(
            report = report,
            onClose = { viewModel.resetReport() }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ATS Resume Analyzer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Powered by local NLP algorithms to optimize your resume for applicant tracking systems.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Step 1: Active Resume Document Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                border = BorderStroke(1.dp, CardBorderLight),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Active Resume Document",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedResume != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, Shapes.medium)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedResume!!.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${selectedResume!!.fileSizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteResume(selectedResume!!.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        enabled = uploadState !is NetworkResult.Loading
                    ) {
                        if (uploadState is NetworkResult.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedResume == null) "Upload Resume (PDF / DOCX)" else "Replace Resume")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 2: Analysis Type Segmented Buttons
            Text(
                text = "2. Analysis Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = analysisMode == 0,
                    onClick = { viewModel.analysisMode.value = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("General Quality")
                }
                SegmentedButton(
                    selected = analysisMode == 1,
                    onClick = { viewModel.analysisMode.value = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Job Compatibility")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode 1: Job Description Input
            if (analysisMode == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target Job Requirements",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        HireGridTextField(
                            value = pastedJobDescription,
                            onValueChange = { viewModel.pastedJobDescription.value = it },
                            label = "Paste Job Description or Requirements",
                            placeholder = "e.g. Senior Android Engineer with Kotlin, Jetpack Compose, MVVM...",
                            singleLine = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Run Analysis Button
            HireGridButton(
                text = if (analysisMode == 0) "Analyze Resume Quality" else "Match Compatibility Score",
                onClick = { viewModel.runAnalysis() },
                isLoading = reportState is NetworkResult.Loading,
                enabled = selectedResume != null
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Past Reports History Section
            Text(
                text = "Past Analysis Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = historyState) {
                is NetworkResult.Loading -> CircularProgressIndicator()
                is NetworkResult.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
                is NetworkResult.Success -> {
                    val reports = state.data.items
                    if (reports.isEmpty()) {
                        Text(
                            text = "No previous reports saved yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        reports.forEach { reportItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = Shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Score: ${reportItem.overallScore.toInt()}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (reportItem.skillMatchScore != null) "Job Description Match" else "General Quality Evaluation",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteReport(reportItem.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete report",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
