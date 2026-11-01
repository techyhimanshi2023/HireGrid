package com.hiregrid.app.presentation.navigation

sealed class Screen(val route: String) {
    // Auth Flow
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Candidate Navigation Graphs
    object CandidateMain : Screen("candidate_main")
    object CandidateHome : Screen("candidate_home")
    object JobExplore : Screen("job_explore")
    object CandidateApplications : Screen("candidate_applications")
    object CandidateAts : Screen("candidate_ats")
    object CandidateProfile : Screen("candidate_profile")
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String): String = "job_detail/$jobId"
    }
    object AtsReportDetail : Screen("ats_report_detail/{reportId}") {
        fun createRoute(reportId: String): String = "ats_report_detail/$reportId"
    }

    // Employer Navigation Graphs
    object EmployerMain : Screen("employer_main")
    object EmployerDashboard : Screen("employer_dashboard")
    object EmployerMyJobs : Screen("employer_my_jobs")
    object CreateJob : Screen("create_job")
    object EmployerCompany : Screen("employer_company")
    object EmployerApplicants : Screen("employer_applicants/{jobId}") {
        fun createRoute(jobId: String): String = "employer_applicants/$jobId"
    }
}
