package com.hiregrid.app.presentation.candidate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hiregrid.app.presentation.ats.AtsAnalyzerScreen
import com.hiregrid.app.presentation.ats.AtsViewModel

@Composable
fun CandidateMainScreen(
    viewModel: CandidateViewModel,
    atsViewModel: AtsViewModel,
    onNavigateToJobDetail: (jobId: String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Explore") },
                    label = { Text("Explore") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Applications") },
                    label = { Text("Applications") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "ATS") },
                    label = { Text("ATS Analyzer") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> CandidateHomeScreen(
                    viewModel = viewModel,
                    onNavigateToJobDetail = onNavigateToJobDetail,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToAts = { selectedTab = 3 },
                    onNavigateToApplications = { selectedTab = 2 }
                )
                1 -> JobExploreScreen(
                    viewModel = viewModel,
                    onNavigateToJobDetail = onNavigateToJobDetail
                )
                2 -> ApplicationTrackerScreen(
                    viewModel = viewModel,
                    onNavigateToJobDetail = onNavigateToJobDetail
                )
                3 -> AtsAnalyzerScreen(
                    viewModel = atsViewModel
                )
            }
        }
    }
}
