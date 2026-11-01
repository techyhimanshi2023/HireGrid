package com.hiregrid.app.presentation.ats

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiregrid.app.core.common.NetworkResult
import com.hiregrid.app.data.model.*
import com.hiregrid.app.data.repository.AtsRepository
import com.hiregrid.app.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AtsViewModel(
    private val atsRepository: AtsRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    // Upload & Resumes List State
    private val _uploadState = MutableStateFlow<NetworkResult<ResumeDto>?>(null)
    val uploadState: StateFlow<NetworkResult<ResumeDto>?> = _uploadState.asStateFlow()

    private val _resumesState = MutableStateFlow<NetworkResult<ResumeListResponse>>(NetworkResult.Loading)
    val resumesState: StateFlow<NetworkResult<ResumeListResponse>> = _resumesState.asStateFlow()

    // Analysis Report State
    private val _reportState = MutableStateFlow<NetworkResult<AtsReportDto>?>(null)
    val reportState: StateFlow<NetworkResult<AtsReportDto>?> = _reportState.asStateFlow()

    // Past History State
    private val _historyState = MutableStateFlow<NetworkResult<AtsHistoryResponse>>(NetworkResult.Loading)
    val historyState: StateFlow<NetworkResult<AtsHistoryResponse>> = _historyState.asStateFlow()

    // Active Jobs List for matching
    private val _availableJobsState = MutableStateFlow<NetworkResult<JobListResponse>>(NetworkResult.Loading)
    val availableJobsState: StateFlow<NetworkResult<JobListResponse>> = _availableJobsState.asStateFlow()

    // Form / Selection States
    var selectedResume = MutableStateFlow<ResumeDto?>(null)
    var analysisMode = MutableStateFlow(0) // 0: General Quality, 1: Job Match
    var pastedJobDescription = MutableStateFlow("")
    var selectedJobId = MutableStateFlow<String?>(null)

    init {
        loadResumes()
        loadHistory()
        loadAvailableJobs()
    }

    fun loadResumes() {
        viewModelScope.launch {
            atsRepository.getMyResumes().collect { result ->
                _resumesState.value = result
                if (result is NetworkResult.Success && result.data.items.isNotEmpty() && selectedResume.value == null) {
                    selectedResume.value = result.data.items.first()
                }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            atsRepository.getAtsHistory().collect { result ->
                _historyState.value = result
            }
        }
    }

    fun loadAvailableJobs() {
        viewModelScope.launch {
            jobRepository.listJobs().collect { result ->
                _availableJobsState.value = result
            }
        }
    }

    fun uploadResume(uri: Uri, context: Context) {
        viewModelScope.launch {
            atsRepository.uploadResume(uri, context).collect { result ->
                _uploadState.value = result
                if (result is NetworkResult.Success) {
                    selectedResume.value = result.data
                    loadResumes()
                }
            }
        }
    }

    fun runAnalysis() {
        val resume = selectedResume.value ?: return

        viewModelScope.launch {
            if (analysisMode.value == 0) {
                // General standalone evaluation
                atsRepository.analyzeGeneral(resume.id).collect { result ->
                    _reportState.value = result
                    if (result is NetworkResult.Success) {
                        loadHistory()
                    }
                }
            } else {
                // Job description compatibility match
                val req = AtsMatchRequestDto(
                    resumeId = resume.id,
                    jobId = selectedJobId.value,
                    jobDescription = pastedJobDescription.value.ifBlank { null }
                )
                atsRepository.analyzeJobMatch(req).collect { result ->
                    _reportState.value = result
                    if (result is NetworkResult.Success) {
                        loadHistory()
                    }
                }
            }
        }
    }

    fun deleteResume(resumeId: String) {
        viewModelScope.launch {
            atsRepository.deleteResume(resumeId).collect { result ->
                if (result is NetworkResult.Success) {
                    if (selectedResume.value?.id == resumeId) {
                        selectedResume.value = null
                    }
                    loadResumes()
                }
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            atsRepository.deleteAtsReport(reportId).collect { result ->
                if (result is NetworkResult.Success) {
                    loadHistory()
                }
            }
        }
    }

    fun resetReport() {
        _reportState.value = null
    }

    fun resetUploadState() {
        _uploadState.value = null
    }
}
