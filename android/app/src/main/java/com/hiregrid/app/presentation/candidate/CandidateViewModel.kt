package com.hiregrid.app.presentation.candidate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.data.model.*
import com.hiregrid.app.data.repository.ApplicationRepository
import com.hiregrid.app.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CandidateViewModel(
    private val jobRepository: JobRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    // Dashboard State
    private val _dashboardState = MutableStateFlow<NetworkResult<CandidateDashboardResponse>>(NetworkResult.Loading)
    val dashboardState: StateFlow<NetworkResult<CandidateDashboardResponse>> = _dashboardState.asStateFlow()

    // Jobs Explore State
    private val _jobsState = MutableStateFlow<NetworkResult<JobListResponse>>(NetworkResult.Loading)
    val jobsState: StateFlow<NetworkResult<JobListResponse>> = _jobsState.asStateFlow()

    // Selected Job Detail State
    private val _selectedJobState = MutableStateFlow<NetworkResult<JobDto>>(NetworkResult.Loading)
    val selectedJobState: StateFlow<NetworkResult<JobDto>> = _selectedJobState.asStateFlow()

    // Submitted Applications State
    private val _applicationsState = MutableStateFlow<NetworkResult<ApplicationListResponse>>(NetworkResult.Loading)
    val applicationsState: StateFlow<NetworkResult<ApplicationListResponse>> = _applicationsState.asStateFlow()

    // Application Submission State
    private val _applyState = MutableStateFlow<NetworkResult<ApplicationDto>?>(null)
    val applyState: StateFlow<NetworkResult<ApplicationDto>?> = _applyState.asStateFlow()

    var searchQuery = MutableStateFlow("")
    var selectedWorkplaceFilter = MutableStateFlow<String?>(null) // "remote", "hybrid", "onsite"

    init {
        loadDashboard()
        loadJobs()
        loadApplications()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            jobRepository.getCandidateDashboard().collect { result ->
                _dashboardState.value = result
            }
        }
    }

    fun loadJobs(query: String? = null, workplaceType: String? = null) {
        viewModelScope.launch {
            jobRepository.listJobs(
                query = query ?: searchQuery.value.ifBlank { null },
                workplaceType = workplaceType ?: selectedWorkplaceFilter.value
            ).collect { result ->
                _jobsState.value = result
            }
        }
    }

    fun loadJobDetail(jobId: String) {
        viewModelScope.launch {
            jobRepository.getJobById(jobId).collect { result ->
                _selectedJobState.value = result
            }
        }
    }

    fun loadApplications() {
        viewModelScope.launch {
            applicationRepository.getMyApplications().collect { result ->
                _applicationsState.value = result
            }
        }
    }

    fun toggleSaveJob(jobId: String) {
        viewModelScope.launch {
            jobRepository.toggleSaveJob(jobId).collect { result ->
                if (result is NetworkResult.Success) {
                    // Update state locally in jobs list
                    val currentJobs = (_jobsState.value as? NetworkResult.Success)?.data
                    if (currentJobs != null) {
                        val updated = currentJobs.items.map {
                            if (it.id == jobId) it.copy(isSaved = result.data) else it
                        }
                        _jobsState.value = NetworkResult.Success(currentJobs.copy(items = updated))
                    }
                    // Also refresh dashboard saved count
                    loadDashboard()
                }
            }
        }
    }

    fun applyToJob(jobId: String, coverNote: String?) {
        viewModelScope.launch {
            applicationRepository.applyToJob(jobId = jobId, coverNote = coverNote).collect { result ->
                _applyState.value = result
                if (result is NetworkResult.Success) {
                    loadApplications()
                    loadDashboard()
                }
            }
        }
    }

    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            applicationRepository.withdrawApplication(applicationId).collect { result ->
                if (result is NetworkResult.Success) {
                    loadApplications()
                    loadDashboard()
                }
            }
        }
    }

    fun resetApplyState() {
        _applyState.value = null
    }
}
