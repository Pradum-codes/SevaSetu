package com.example.sevasetu.ui.screen.reports

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.remote.dto.TimelineUpdateDto
import com.example.sevasetu.data.repository.IssueRepository
import com.example.sevasetu.network.ApiService
import com.example.sevasetu.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val issueRepository = IssueRepository(ApiService.issueApi(appContext))
    private val tokenManager = TokenManager(appContext)

    private var hasLoaded = false

    private val _selectedFilter = MutableStateFlow(ReportStatusFilter.ALL)
    val selectedFilter: StateFlow<ReportStatusFilter> = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _selectedIssue = MutableStateFlow<IssueDto?>(null)
    val selectedIssue: StateFlow<IssueDto?> = _selectedIssue.asStateFlow()

    private val _voteInFlight = MutableStateFlow(false)
    val voteInFlight: StateFlow<Boolean> = _voteInFlight.asStateFlow()

    private val _selectedIssueTimeline = MutableStateFlow<List<TimelineUpdateDto>?>(null)
    val selectedIssueTimeline: StateFlow<List<TimelineUpdateDto>?> = _selectedIssueTimeline.asStateFlow()

    private val _isTimelineLoading = MutableStateFlow(false)
    val isTimelineLoading: StateFlow<Boolean> = _isTimelineLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchReports(_selectedFilter.value)
            _isRefreshing.value = false
        }
    }

    fun onFilterChanged(filter: ReportStatusFilter) {
        if (_selectedFilter.value == filter) return
        _selectedFilter.value = filter
        viewModelScope.launch { fetchReports(filter) }
    }

    fun openIssue(issue: IssueDto) {
        _selectedIssue.value = issue
        viewModelScope.launch {
            _isTimelineLoading.value = true
            _selectedIssueTimeline.value = null
            issueRepository.getIssueTimeline(issue.id)
                .onSuccess { response -> _selectedIssueTimeline.value = response.timeline }
                .onFailure { }
            _isTimelineLoading.value = false
        }
    }

    fun dismissIssueModal() {
        _selectedIssue.value = null
        _selectedIssueTimeline.value = null
    }

    fun handleVote() {
        val issue = _selectedIssue.value ?: return
        if (_voteInFlight.value) return
        _voteInFlight.value = true
        viewModelScope.launch {
            issueRepository.voteIssue(issue.id)
                .onSuccess { response ->
                    if (response.voted) tokenManager.addVotedIssue(issue.id) else tokenManager.removeVotedIssue(issue.id)

                    _selectedIssue.update {
                        it?.copy(voteCount = response.totalVotes, isVotedByMe = response.voted)
                    }

                    val updatedFullIssues = _uiState.value.fullIssues.map {
                        if (it.id == issue.id) it.copy(
                            voteCount = response.totalVotes,
                            isVotedByMe = response.voted
                        ) else it
                    }
                    _uiState.update {
                        it.copy(fullIssues = updatedFullIssues, reports = updatedFullIssues.map { dto -> dto.toReportListItem() })
                    }
                }
                .onFailure {
                    Toast.makeText(appContext, "Failed to update vote: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            _voteInFlight.value = false
        }
    }

    private suspend fun fetchReports(filter: ReportStatusFilter) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        issueRepository.getMyReportedIssues(
            page = 1,
            limit = 50,
            status = filter.apiValue
        ).onSuccess { response ->
            val votedSet = tokenManager.getVotedIssues()
            val enrichedIssues = response.issues.map {
                it.copy(isVotedByMe = it.isVotedByMe ?: votedSet.contains(it.id))
            }

            _uiState.value = ReportsUiState(
                isLoading = false,
                reports = enrichedIssues.map { it.toReportListItem() },
                fullIssues = enrichedIssues
            )
        }.onFailure { throwable ->
            _uiState.value = ReportsUiState(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load reports"
            )
        }
    }
}

internal fun IssueDto.toReportListItem(): ReportListItem {
    val normalizedStatus = status?.uppercase(Locale.ROOT) ?: "OPEN"
    return ReportListItem(
        id = id,
        title = title.ifBlank { "Untitled issue" },
        reportedDateLabel = createdAt.toReportDateLabel(),
        statusLabel = normalizedStatus.replace('_', ' '),
        statusRaw = normalizedStatus,
        imageUrl = resolvePreviewImageUrl(),
        voteCount = voteCount,
        isVotedByMe = isVotedByMe == true
    )
}

private fun IssueDto.resolvePreviewImageUrl(): String? {
    return images.firstNotNullOfOrNull { it.imageUrl?.trim()?.takeIf(String::isNotEmpty) }
        ?: imageUrls?.firstNotNullOfOrNull { it.trim().takeIf(String::isNotEmpty) }
        ?: imageUrl?.trim()?.takeIf(String::isNotEmpty)
}

private fun String?.toReportDateLabel(): String {
    if (this.isNullOrBlank()) return "date unavailable"

    val clean = trim()
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    val output = SimpleDateFormat("dd MMM, yyyy", Locale.US)

    patterns.forEach { pattern ->
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = runCatching { parser.parse(clean) }.getOrNull()
        if (date != null) {
            return output.format(date)
        }
    }

    return clean.take(10)
}
