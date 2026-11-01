package com.hiregrid.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hiregrid.app.core.datastore.SessionManager
import com.hiregrid.app.core.network.RetrofitClient
import com.hiregrid.app.data.repository.ApplicationRepository
import com.hiregrid.app.data.repository.AtsRepository
import com.hiregrid.app.data.repository.AuthRepository
import com.hiregrid.app.data.repository.EmployerRepository
import com.hiregrid.app.data.repository.JobRepository
import com.hiregrid.app.presentation.ats.AtsViewModel
import com.hiregrid.app.presentation.auth.AuthViewModel
import com.hiregrid.app.presentation.auth.LoginScreen
import com.hiregrid.app.presentation.auth.RegisterScreen
import com.hiregrid.app.presentation.candidate.CandidateMainScreen
import com.hiregrid.app.presentation.candidate.CandidateViewModel
import com.hiregrid.app.presentation.candidate.JobDetailScreen
import com.hiregrid.app.presentation.employer.ApplicantReviewScreen
import com.hiregrid.app.presentation.employer.CreateJobScreen
import com.hiregrid.app.presentation.employer.EmployerMainScreen
import com.hiregrid.app.presentation.employer.EmployerViewModel
import com.hiregrid.app.presentation.profile.ProfileScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HireGridNavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiService = remember { RetrofitClient.getApiService(context) }
    val authRepository = remember { AuthRepository(apiService, sessionManager) }
    val jobRepository = remember { JobRepository(apiService) }
    val applicationRepository = remember { ApplicationRepository(apiService) }
    val employerRepository = remember { EmployerRepository(apiService) }
    val atsRepository = remember { AtsRepository(apiService) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash Router
        composable(Screen.Splash.route) {
            LaunchedEffect(Unit) {
                try {
                    val isLoggedIn = withTimeoutOrNull(1000) { sessionManager.isLoggedIn.first() } ?: false
                    if (isLoggedIn) {
                        val role = withTimeoutOrNull(800) { sessionManager.userRole.first() } ?: "candidate"
                        if (role.lowercase() == "employer") {
                            navController.navigate(Screen.EmployerMain.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.CandidateMain.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Login Screen
        composable(Screen.Login.route) {
            val authViewModel = remember { AuthViewModel(authRepository) }
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { role ->
                    if (role.lowercase() == "employer") {
                        navController.navigate(Screen.EmployerMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.CandidateMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Register Screen
        composable(Screen.Register.route) {
            val authViewModel = remember { AuthViewModel(authRepository) }
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = { role ->
                    if (role.lowercase() == "employer") {
                        navController.navigate(Screen.EmployerMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.CandidateMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Candidate Profile Screen
        composable(Screen.CandidateProfile.route) {
            ProfileScreen(
                authRepository = authRepository,
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Candidate Main Flow (Bottom Navigation Tabs: Home, Explore, Applications, ATS)
        composable(Screen.CandidateMain.route) {
            val candidateViewModel = remember { CandidateViewModel(jobRepository, applicationRepository) }
            val atsViewModel = remember { AtsViewModel(atsRepository, jobRepository) }
            CandidateMainScreen(
                viewModel = candidateViewModel,
                atsViewModel = atsViewModel,
                onNavigateToJobDetail = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.CandidateProfile.route)
                }
            )
        }

        // Job Details Screen
        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val candidateViewModel = remember { CandidateViewModel(jobRepository, applicationRepository) }
            JobDetailScreen(
                jobId = jobId,
                viewModel = candidateViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Employer Main Flow (Bottom Navigation: Dashboard, My Jobs, Company Profile)
        composable(Screen.EmployerMain.route) {
            val employerViewModel = remember { EmployerViewModel(employerRepository, applicationRepository) }
            EmployerMainScreen(
                viewModel = employerViewModel,
                authRepository = authRepository,
                onNavigateToCreateJob = {
                    navController.navigate(Screen.CreateJob.route)
                },
                onNavigateToApplicants = { jobId ->
                    navController.navigate(Screen.EmployerApplicants.createRoute(jobId))
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Employer Create Job Screen
        composable(Screen.CreateJob.route) {
            val employerViewModel = remember { EmployerViewModel(employerRepository, applicationRepository) }
            CreateJobScreen(
                viewModel = employerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Employer Applicant Review Screen
        composable(
            route = Screen.EmployerApplicants.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val employerViewModel = remember { EmployerViewModel(employerRepository, applicationRepository) }
            ApplicantReviewScreen(
                jobId = jobId,
                viewModel = employerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
