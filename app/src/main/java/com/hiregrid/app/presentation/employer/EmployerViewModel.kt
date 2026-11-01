package com.hiregrid.app.presentation.employer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.data.model.*
import com.hiregrid.app.data.repository.ApplicationRepository
import com.hiregrid.app.data.repository.EmployerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployerViewModel(
    private val employerRepository: EmployerRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    // Dashboard State
    private val _dashboardState = MutableStateFlow<NetworkResult<EmployerDashboardResponse>>(NetworkResult.Loading)
    val dashboardState: StateFlow<NetworkResult<EmployerDashboardResponse>> = _dashboardState.asStateFlow()

    // Posted Jobs State
    private val _myJobsState = MutableStateFlow<NetworkResult<JobListResponse>>(NetworkResult.Loading)
    val myJobsState: StateFlow<NetworkResult<JobListResponse>> = _myJobsState.asStateFlow()

    // Job Applicants Review State
    private val _applicantsState = MutableStateFlow<NetworkResult<ApplicationListResponse>>(NetworkResult.Loading)
    val applicantsState: StateFlow<NetworkResult<ApplicationListResponse>> = _applicantsState.asStateFlow()

    // Job Creation Form States
    var title = MutableStateFlow("")
    var description = MutableStateFlow("")
    var responsibilities = MutableStateFlow("")
    var requirements = MutableStateFlow("")
    var location = MutableStateFlow("Remote")
    var workplaceType = MutableStateFlow("remote") // "remote", "hybrid", "onsite"
    var jobType = MutableStateFlow("full_time")
    var minExperienceYears = MutableStateFlow("2")
    var minSalary = MutableStateFlow("")
    var maxSalary = MutableStateFlow("")
    var vacancies = MutableStateFlow("1")
    var currentSkillInput = MutableStateFlow("")
    var skillsList = MutableStateFlow<List<String>>(listOf("Kotlin", "Jetpack Compose"))

    var titleError = MutableStateFlow<String?>(null)
    var descriptionError = MutableStateFlow<String?>(null)

    private val _createJobState = MutableStateFlow<NetworkResult<JobDto>?>(null)
    val createJobState: StateFlow<NetworkResult<JobDto>?> = _createJobState.asStateFlow()

    init {
        loadDashboard()
        loadMyJobs()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            employerRepository.getEmployerDashboard().collect { result ->
                _dashboardState.value = result
            }
        }
    }

    fun loadMyJobs() {
        viewModelScope.launch {
            employerRepository.getEmployerJobs().collect { result ->
                _myJobsState.value = result
            }
        }
    }

    fun loadApplicants(jobId: String) {
        viewModelScope.launch {
            applicationRepository.getJobApplicants(jobId).collect { result ->
                _applicantsState.value = result
            }
        }
    }

    fun addSkill(skill: String) {
        val trimmed = skill.trim()
        if (trimmed.isNotEmpty() && !skillsList.value.contains(trimmed)) {
            skillsList.value = skillsList.value + trimmed
            currentSkillInput.value = ""
        }
    }

    fun removeSkill(skill: String) {
        skillsList.value = skillsList.value - skill
    }

    private fun validateJobForm(): Boolean {
        var isValid = true
        if (title.value.trim().length < 3) {
            titleError.value = "Job title must be at least 3 characters"
            isValid = false
        } else {
            titleError.value = null
        }

        if (description.value.trim().length < 10) {
            descriptionError.value = "Description must be at least 10 characters"
            isValid = false
        } else {
            descriptionError.value = null
        }

        return isValid
    }

    fun createJob() {
        if (!validateJobForm()) return

        val req = JobCreateRequest(
            title = title.value.trim(),
            description = description.value.trim(),
            responsibilities = responsibilities.value.trim().ifBlank { null },
            requirements = requirements.value.trim().ifBlank { null },
            jobType = jobType.value,
            workplaceType = workplaceType.value,
            location = location.value.trim(),
            minExperienceYears = minExperienceYears.value.toIntOrNull() ?: 0,
            minSalary = minSalary.value.toDoubleOrNull(),
            maxSalary = maxSalary.value.toDoubleOrNull(),
            vacancies = vacancies.value.toIntOrNull() ?: 1,
            requiredSkills = skillsList.value
        )

        viewModelScope.launch {
            employerRepository.createJob(req).collect { result ->
                _createJobState.value = result
                if (result is NetworkResult.Success) {
                    resetForm()
                    loadMyJobs()
                    loadDashboard()
                }
            }
        }
    }

    fun toggleJobStatus(jobId: String, currentStatus: String) {
        val nextStatus = if (currentStatus.lowercase() == "active") "closed" else "active"
        viewModelScope.launch {
            employerRepository.updateJobStatus(jobId, nextStatus).collect { result ->
                if (result is NetworkResult.Success) {
                    loadMyJobs()
                    loadDashboard()
                }
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            employerRepository.deleteJob(jobId).collect { result ->
                if (result is NetworkResult.Success) {
                    loadMyJobs()
                    loadDashboard()
                }
            }
        }
    }

    fun updateApplicantStatus(applicationId: String, newStatus: String, notes: String?, jobId: String) {
        viewModelScope.launch {
            applicationRepository.updateApplicationStatus(applicationId, newStatus, notes).collect { result ->
                if (result is NetworkResult.Success) {
                    loadApplicants(jobId)
                    loadDashboard()
                }
            }
        }
    }

    fun resetCreateJobState() {
        _createJobState.value = null
    }

    private fun resetForm() {
        title.value = ""
        description.value = ""
        responsibilities.value = ""
        requirements.value = ""
        location.value = "Remote"
        minSalary.value = ""
        maxSalary.value = ""
        skillsList.value = listOf("Kotlin", "Jetpack Compose")
    }
}
