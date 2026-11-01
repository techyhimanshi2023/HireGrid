package com.hiregrid.app.presentation.employer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hiregrid.app.data.repository.AuthRepository
import com.hiregrid.app.presentation.profile.ProfileScreen

@Composable
fun EmployerMainScreen(
    viewModel: EmployerViewModel,
    authRepository: AuthRepository,
    onNavigateToCreateJob: () -> Unit,
    onNavigateToApplicants: (jobId: String) -> Unit,
    onLogoutSuccess: () -> Unit
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
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Work, contentDescription = "My Jobs") },
                    label = { Text("My Jobs") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Business, contentDescription = "Company") },
                    label = { Text("Company") }
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
                0 -> EmployerDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMyJobs = { selectedTab = 1 },
                    onNavigateToProfile = { selectedTab = 2 },
                    onNavigateToCreateJob = onNavigateToCreateJob
                )
                1 -> MyJobsScreen(
                    viewModel = viewModel,
                    onNavigateToCreateJob = onNavigateToCreateJob,
                    onNavigateToApplicants = onNavigateToApplicants
                )
                2 -> ProfileScreen(
                    authRepository = authRepository,
                    onLogoutSuccess = onLogoutSuccess
                )
            }
        }
    }
}
