package com.hiregrid.app.presentation.candidate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.presentation.candidate.components.JobCard

@Composable
fun JobExploreScreen(
    viewModel: CandidateViewModel,
    onNavigateToJobDetail: (jobId: String) -> Unit
) {
    val jobsState by viewModel.jobsState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedWorkplaceFilter.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Header
            Text(
                text = "Explore Jobs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.searchQuery.value = it
                    viewModel.loadJobs(query = it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = Shapes.large,
                placeholder = { Text("Search by title, skill, or location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.loadJobs(query = "")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = {
                        viewModel.selectedWorkplaceFilter.value = null
                        viewModel.loadJobs(workplaceType = null)
                    },
                    label = { Text("All Workplaces") }
                )

                FilterChip(
                    selected = selectedFilter == "remote",
                    onClick = {
                        viewModel.selectedWorkplaceFilter.value = "remote"
                        viewModel.loadJobs(workplaceType = "remote")
                    },
                    label = { Text("Remote Only") }
                )

                FilterChip(
                    selected = selectedFilter == "hybrid",
                    onClick = {
                        viewModel.selectedWorkplaceFilter.value = "hybrid"
                        viewModel.loadJobs(workplaceType = "hybrid")
                    },
                    label = { Text("Hybrid") }
                )

                FilterChip(
                    selected = selectedFilter == "onsite",
                    onClick = {
                        viewModel.selectedWorkplaceFilter.value = "onsite"
                        viewModel.loadJobs(workplaceType = "onsite")
                    },
                    label = { Text("On-site") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Jobs List Content
            when (val state = jobsState) {
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
                            Button(onClick = { viewModel.loadJobs() }) { Text("Retry") }
                        }
                    }
                }
                is NetworkResult.Success -> {
                    val jobs = state.data.items

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.data.total} vacancies found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (jobs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No vacancies match your search criteria.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(jobs, key = { it.id }) { job ->
                                JobCard(
                                    job = job,
                                    onClick = { onNavigateToJobDetail(job.id) },
                                    onBookmarkClick = { viewModel.toggleSaveJob(job.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
