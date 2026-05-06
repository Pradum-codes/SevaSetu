package com.example.sevasetu.ui.screen.dashboard

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.remote.dto.TimelineUpdateDto
import com.example.sevasetu.data.repository.IssueRepository
import com.example.sevasetu.network.ApiService
import com.example.sevasetu.utils.JurisdictionConstants
import com.example.sevasetu.utils.TokenManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import org.osmdroid.util.GeoPoint

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val issueRepository = IssueRepository(ApiService.issueApi(appContext))
    private val tokenManager = TokenManager(appContext)

    private var hasLoaded = false

    private val _mapUiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val mapUiState: StateFlow<MapUiState> = _mapUiState.asStateFlow()

    private val _dashboardUiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val dashboardUiState: StateFlow<DashboardUiState> = _dashboardUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _selectedIssue = MutableStateFlow<IssueDto?>(null)
    val selectedIssue: StateFlow<IssueDto?> = _selectedIssue.asStateFlow()

    private val _selectedIssueTimeline = MutableStateFlow<List<TimelineUpdateDto>?>(null)
    val selectedIssueTimeline: StateFlow<List<TimelineUpdateDto>?> = _selectedIssueTimeline.asStateFlow()

    private val _isTimelineLoading = MutableStateFlow(false)
    val isTimelineLoading: StateFlow<Boolean> = _isTimelineLoading.asStateFlow()

    private val _voteInFlight = MutableStateFlow(false)
    val voteInFlight: StateFlow<Boolean> = _voteInFlight.asStateFlow()

    val userDistrictId: String = tokenManager.getUserDistrict()?.takeIf { it.isNotBlank() }
        ?: DEFAULT_NEARBY_DISTRICT_ID

    val userDistrictName: String = JurisdictionConstants.DISTRICTS
        .find { it.id == userDistrictId }?.name ?: "Unknown"

    val userDistrictCoords: GeoPoint = JurisdictionConstants.DISTRICTS
        .find { it.id == userDistrictId }
        ?.let { GeoPoint(it.lat, it.lng) }
        ?: GeoPoint(31.6340, 74.8723)

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchNearbyIssues()
            fetchDashboard()
            _isRefreshing.value = false
        }
    }

    fun dismissIssueModal() {
        _selectedIssue.value = null
        _selectedIssueTimeline.value = null
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

                    val currentMap = _mapUiState.value
                    if (currentMap is MapUiState.Success) {
                        _mapUiState.value = currentMap.copy(
                            fullIssues = currentMap.fullIssues.map {
                                if (it.id == issue.id) it.copy(
                                    voteCount = response.totalVotes,
                                    isVotedByMe = response.voted
                                ) else it
                            }
                        )
                    }
                }
                .onFailure {
                    Toast.makeText(appContext, "Failed to update vote: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            _voteInFlight.value = false
        }
    }

    val mapCenterPoint: GeoPoint
        get() = _currentLocation.value?.let { GeoPoint(it.first, it.second) } ?: userDistrictCoords

    private suspend fun fetchNearbyIssues() {
        _mapUiState.value = MapUiState.Loading
        val location = fetchCurrentLocation()
        _currentLocation.value = location

        issueRepository.getNearbyIssues(
            lat = location?.first,
            lng = location?.second,
            radiusKm = 5.0,
            districtId = userDistrictId
        ).onSuccess { response ->
            val votedSet = tokenManager.getVotedIssues()
            val enrichedIssues = response.issues.map {
                it.copy(isVotedByMe = it.isVotedByMe ?: votedSet.contains(it.id))
            }

            val mapIssues = enrichedIssues.mapNotNull { issue ->
                val issueLat = issue.lat ?: return@mapNotNull null
                val issueLng = issue.lng ?: return@mapNotNull null
                MapIssue(
                    id = issue.id,
                    title = issue.title.ifBlank { "Issue" },
                    addressText = issue.addressText,
                    lat = issueLat,
                    lng = issueLng,
                    imageUrl = issue.images.firstNotNullOfOrNull { img -> img.imageUrl?.trim()?.takeIf(String::isNotEmpty) }
                        ?: issue.imageUrls?.firstNotNullOfOrNull { it.trim().takeIf(String::isNotEmpty) }
                        ?: issue.imageUrl?.trim()?.takeIf(String::isNotEmpty),
                    priority = issue.priority
                )
            }

            val displayInfo = when {
                response.searchMode == "location" && response.location != null -> "Location-based search (${response.location.radiusKm}km)"
                response.searchMode == "district" && response.district != null -> "District-based search (${response.district.name})"
                else -> "Nearby Issues"
            }

            _mapUiState.value = MapUiState.Success(
                issues = mapIssues,
                displayInfo = displayInfo,
                searchMode = response.searchMode,
                fullIssues = enrichedIssues
            )
        }.onFailure { throwable ->
            _mapUiState.value = MapUiState.Error(throwable.message ?: "Unable to load nearby issues")
        }
    }

    private suspend fun fetchDashboard() {
        _dashboardUiState.value = DashboardUiState.Loading

        val location = _currentLocation.value ?: fetchCurrentLocation().also { _currentLocation.value = it }

        issueRepository.getDashboard(
            lat = location?.first,
            lng = location?.second,
            radiusKm = 5.0,
            districtId = userDistrictId,
            insightWindowDays = 30
        ).onSuccess { response ->
            _dashboardUiState.value = DashboardUiState.Success(response)
        }.onFailure { throwable ->
            _dashboardUiState.value = DashboardUiState.Error(throwable.message ?: "Unable to load dashboard")
        }
    }

    private suspend fun fetchCurrentLocation(): Pair<Double, Double>? {
        return try {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (!hasLocationPermission) return null

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
            val tokenSource = CancellationTokenSource()
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            cont.resume(Pair(location.latitude, location.longitude), onCancellation = null)
                        } else {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { lastLocation: Location? ->
                                    cont.resume(lastLocation?.let { Pair(it.latitude, it.longitude) }, onCancellation = null)
                                }
                                .addOnFailureListener {
                                    cont.resume(null, onCancellation = null)
                                }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null, onCancellation = null)
                    }
            }
        } catch (_: Exception) {
            null
        }
    }
}
