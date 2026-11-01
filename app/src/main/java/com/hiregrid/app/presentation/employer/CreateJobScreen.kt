package com.hiregrid.app.presentation.employer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.components.HireGridButton
import com.hiregrid.app.core.ui.components.HireGridTextField
import com.hiregrid.app.core.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    viewModel: EmployerViewModel,
    onNavigateBack: () -> Unit
) {
    val createJobState by viewModel.createJobState.collectAsState()

    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val responsibilities by viewModel.responsibilities.collectAsState()
    val requirements by viewModel.requirements.collectAsState()
    val location by viewModel.location.collectAsState()
    val workplaceType by viewModel.workplaceType.collectAsState()
    val minExp by viewModel.minExperienceYears.collectAsState()
    val minSalary by viewModel.minSalary.collectAsState()
    val maxSalary by viewModel.maxSalary.collectAsState()
    val vacancies by viewModel.vacancies.collectAsState()
    val currentSkill by viewModel.currentSkillInput.collectAsState()
    val skillsList by viewModel.skillsList.collectAsState()

    val titleError by viewModel.titleError.collectAsState()
    val descriptionError by viewModel.descriptionError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createJobState) {
        when (val state = createJobState) {
            is NetworkResult.Success -> {
                snackbarHostState.showSnackbar("Job posted successfully!")
                viewModel.resetCreateJobState()
                onNavigateBack()
            }
            is NetworkResult.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetCreateJobState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Job Posting", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
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
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            HireGridTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = "Job Title",
                placeholder = "e.g. Senior Android Engineer",
                errorMessage = titleError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Workplace Type Selection Chips
            Text("Workplace Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = workplaceType == "remote",
                    onClick = { viewModel.workplaceType.value = "remote" },
                    label = { Text("Remote") }
                )
                FilterChip(
                    selected = workplaceType == "hybrid",
                    onClick = { viewModel.workplaceType.value = "hybrid" },
                    label = { Text("Hybrid") }
                )
                FilterChip(
                    selected = workplaceType == "onsite",
                    onClick = { viewModel.workplaceType.value = "onsite" },
                    label = { Text("On-site") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location & Experience Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HireGridTextField(
                    value = location,
                    onValueChange = { viewModel.location.value = it },
                    label = "Location",
                    placeholder = "e.g. San Francisco or Remote",
                    modifier = Modifier.weight(1f)
                )

                HireGridTextField(
                    value = minExp,
                    onValueChange = { viewModel.minExperienceYears.value = it },
                    label = "Min Exp (Yrs)",
                    placeholder = "2",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Salary Range Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HireGridTextField(
                    value = minSalary,
                    onValueChange = { viewModel.minSalary.value = it },
                    label = "Min Salary ($/yr)",
                    placeholder = "80000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                HireGridTextField(
                    value = maxSalary,
                    onValueChange = { viewModel.maxSalary.value = it },
                    label = "Max Salary ($/yr)",
                    placeholder = "120000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Required Skills Tag Manager
            Text("Required Skills", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentSkill,
                    onValueChange = { viewModel.currentSkillInput.value = it },
                    placeholder = { Text("Add a skill (e.g. Kotlin)") },
                    singleLine = true,
                    shape = Shapes.medium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { viewModel.addSkill(currentSkill) },
                    shape = Shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skill Chips Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skillsList.forEach { skill ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeSkill(skill) },
                        label = { Text(skill) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove skill",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Job Description
            HireGridTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = "Job Description",
                placeholder = "Describe the role, team, and scope of work...",
                singleLine = false,
                errorMessage = descriptionError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Responsibilities
            HireGridTextField(
                value = responsibilities,
                onValueChange = { viewModel.responsibilities.value = it },
                label = "Key Responsibilities (Optional)",
                placeholder = "Bullet points or summary of day-to-day duties...",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Requirements
            HireGridTextField(
                value = requirements,
                onValueChange = { viewModel.requirements.value = it },
                label = "Qualifications & Requirements (Optional)",
                placeholder = "Degrees, certifications, and technical proficiencies...",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(28.dp))

            HireGridButton(
                text = "Publish Job Vacancy",
                onClick = { viewModel.createJob() },
                isLoading = createJobState is NetworkResult.Loading
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
