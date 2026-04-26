package com.example.sevasetu

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.sevasetu.data.remote.dto.DashboardResponse
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.repository.IssueRepository
import com.example.sevasetu.network.ApiService
import com.example.sevasetu.ui.screen.Alerts.AlertsScreen
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.screen.Reports.ReportScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration as OsmConfiguration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.graphics.Color as AndroidColor
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter

class Dashboard : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                DashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val issueRepository = remember { IssueRepository(ApiService.issueApi(context)) }
    val scope = rememberCoroutineScope()
    var mapUiState by remember { mutableStateOf<MapUiState>(MapUiState.Loading) }
    var dashboardUiState by remember { mutableStateOf<DashboardUiState>(DashboardUiState.Loading) }
    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Get user's district from TokenManager
    val userDistrictId = remember {
        val tokenManager = com.example.sevasetu.utils.TokenManager(context)
        tokenManager.getUserDistrict()?.takeIf { it.isNotBlank() } ?: DEFAULT_NEARBY_DISTRICT_ID
    }

    val userDistrictName = remember(userDistrictId) {
        com.example.sevasetu.utils.JurisdictionConstants.DISTRICTS
            .find { it.id == userDistrictId }?.name ?: "Unknown"
    }

    val userDistrictCoords = remember(userDistrictId) {
        val dist = com.example.sevasetu.utils.JurisdictionConstants.DISTRICTS
            .find { it.id == userDistrictId }
        if (dist != null) GeoPoint(dist.lat, dist.lng) else GeoPoint(31.6340, 74.8723)
    }

    val mapCenterPoint = remember(currentLocation, userDistrictCoords) {
        currentLocation?.let { GeoPoint(it.first, it.second) } ?: userDistrictCoords
    }

    val fetchCurrentLocation: (callback: (Pair<Double, Double>?) -> Unit) -> Unit = { callback ->
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            callback(Pair(location.latitude, location.longitude))
                        } else {
                            callback(null)
                        }
                    }
                    .addOnFailureListener {
                        callback(null)
                    }
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            callback(null)
        }
    }

    val fetchNearbyIssues: () -> Unit = {
        scope.launch {
            mapUiState = MapUiState.Loading

            // Try to get current location first
            fetchCurrentLocation { location ->
                scope.launch {
                    currentLocation = location
                    issueRepository.getNearbyIssues(
                        lat = location?.first,
                        lng = location?.second,
                        radiusKm = 5.0,
                        districtId = userDistrictId
                    )
                        .onSuccess { response ->
                            val mapIssues = response.issues.mapNotNull { it.toMapIssue() }
                            val searchMode = response.searchMode
                            val displayInfo = when {
                                searchMode == "location" && response.location != null -> {
                                    "Location-based search (${response.location.radiusKm}km)"
                                }
                                searchMode == "district" && response.district != null -> {
                                    "District-based search (${response.district.name})"
                                }
                                else -> "Nearby Issues"
                            }
                            mapUiState = MapUiState.Success(mapIssues, displayInfo, searchMode)
                        }
                        .onFailure { throwable ->
                            mapUiState = MapUiState.Error(
                                throwable.message ?: "Unable to load nearby issues"
                            )
                        }
                }
            }
        }
    }

    val fetchDashboard: () -> Unit = {
        scope.launch {
            dashboardUiState = DashboardUiState.Loading

            fetchCurrentLocation { location ->
                scope.launch {
                    currentLocation = location
                    issueRepository.getDashboard(
                        lat = location?.first,
                        lng = location?.second,
                        radiusKm = 5.0,
                        districtId = userDistrictId,
                        insightWindowDays = 30
                    )
                        .onSuccess { response ->
                            dashboardUiState = DashboardUiState.Success(response)
                        }
                        .onFailure { throwable ->
                            dashboardUiState = DashboardUiState.Error(
                                throwable.message ?: "Unable to load dashboard"
                            )
                        }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchNearbyIssues()
        fetchDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SevaSetu",
                        color = Color(0xFF00875A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, ProfileScreen::class.java))
                    }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        ) {
                            // Placeholder for profile image
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("HOME") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00875A),
                        selectedTextColor = Color(0xFF00875A),
                        indicatorColor = Color(0xFFE8F5E9)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, ReportScreen::class.java))
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
                    label = { Text("REPORTS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AlertsScreen::class.java))
                    },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("ALERTS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, ProfileScreen::class.java))
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("PROFILE") }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { },
                containerColor = Color(0xFF00875A),
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Report Issue")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Search for locations or report...",
                        color = Color.Gray,
                        modifier = Modifier.weight(1.0f)
                    )
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF00875A))
                }
            }

            Spacer(Modifier.height(16.dp))

            DashboardMapSection(
                mapUiState = mapUiState,
                userDistrictName = userDistrictName,
                centerPoint = mapCenterPoint,
                onRetry = fetchNearbyIssues
            )

            Spacer(Modifier.height(16.dp))

            DashboardSnapshotSection(
                dashboardUiState = dashboardUiState,
                onOpenReports = {
                    context.startActivity(Intent(context, ReportScreen::class.java))
                }
            )

            Spacer(Modifier.height(16.dp))

            // Nearby Insights
            Text("Nearby Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            val currentDashboardState = dashboardUiState
            when (currentDashboardState) {
                is DashboardUiState.Success -> {
                    val insights = currentDashboardState.data.nearbyInsights
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F8E9)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("OPEN", color = Color.Gray, fontSize = 14.sp)
                                Text(insights.open.toString(), color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IN_PROGRESS", color = Color.Gray, fontSize = 14.sp)
                                Text(insights.inProgress.toString(), color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CLOSED", color = Color.Gray, fontSize = 14.sp)
                                Text(insights.closed.toString(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F8E9)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00875A), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick Categories
            Text("Quick Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CategoryChip("Garbage", Icons.Default.Delete) }
                item { CategoryChip("Roads", Icons.Default.EditRoad) }
                item { CategoryChip("Water", Icons.Default.WaterDrop) }
            }

            Spacer(Modifier.height(24.dp))

            // Nearby Issues
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Issues", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = { }) {
                    Text("View All", color = Color.Gray)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, size = 16.sp, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            NearbyIssuesListSection(
                mapUiState = mapUiState,
                onRetry = fetchNearbyIssues
            )
        }
    }
}

@Composable
private fun DashboardSnapshotSection(
    dashboardUiState: DashboardUiState,
    onOpenReports: () -> Unit
) {
    when (dashboardUiState) {
        DashboardUiState.Loading -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00875A))
                }
            }
        }

        is DashboardUiState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = dashboardUiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }

        is DashboardUiState.Success -> {
            val data = dashboardUiState.data
            val snapshot = data.myReportsSnapshot
            val pending = data.myPendingAction
            val risk = data.nearbyRiskSummary

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "My Reports Snapshot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnapshotStatChip(
                            label = "OPEN",
                            count = snapshot.open,
                            background = Color(0xFFFFF3E0),
                            textColor = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotStatChip(
                            label = "IN_PROGRESS",
                            count = snapshot.inProgress,
                            background = Color(0xFFE3F2FD),
                            textColor = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnapshotStatChip(
                            label = "RESOLVED",
                            count = snapshot.resolved,
                            background = Color(0xFFE8F5E9),
                            textColor = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        SnapshotStatChip(
                            label = "REJECTED",
                            count = snapshot.rejected,
                            background = Color(0xFFFFEBEE),
                            textColor = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF7FAF8)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Pending Action",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "You have ${pending.unresolved} unresolved reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            TextButton(onClick = onOpenReports) {
                                Text(pending.cta.label)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F8E9)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Nearby Risk Summary",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${risk.highPriority} high-priority reports",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2D2D2D)
                            )
                            Text(
                                text = "${risk.open} open reports in your area",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Coverage: ${risk.coverageText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00875A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotStatChip(
    label: String,
    count: Int,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun NearbyIssuesListSection(
    mapUiState: MapUiState,
    onRetry: () -> Unit
) {
    when (mapUiState) {
        MapUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00875A))
            }
        }

        is MapUiState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = mapUiState.message,
                        color = Color(0xFF2D2D2D),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }

        is MapUiState.Success -> {
            if (mapUiState.issues.isEmpty()) {
                Text(
                    text = "No nearby issues available right now.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                return
            }

            mapUiState.issues.forEachIndexed { index, issue ->
                val (statusContainerColor, statusTextColor) = statusColorsForPriority(issue.priority)
                val status = statusLabelForPriority(issue.priority)
                val modeLabel = when (mapUiState.searchMode) {
                    "location" -> "Nearby via GPS"
                    "district" -> "District-wide"
                    else -> "Nearby"
                }

                IssueCard(
                    title = issue.title,
                    time = modeLabel,
                    desc = "Community reported issue in your area.",
                    location = issue.addressText ?: "Location unavailable",
                    imageUrl = issue.imageUrl,
                    status = status,
                    statusColor = statusContainerColor,
                    statusTextColor = statusTextColor
                )

                if (index != mapUiState.issues.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardMapSection(
    mapUiState: MapUiState,
    userDistrictName: String,
    centerPoint: GeoPoint,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // District label at top
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF00875A).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color(0xFF00875A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF00875A),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when (mapUiState) {
                        is MapUiState.Success -> mapUiState.displayInfo
                        else -> "Your District: $userDistrictName"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00875A),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            when (mapUiState) {
                MapUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00875A))
                    }
                }

                is MapUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8F5E9))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = mapUiState.message,
                            color = Color(0xFF2D2D2D),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }

                is MapUiState.Success -> {
                    NearbyIssuesMap(
                        issues = mapUiState.issues,
                        centerPoint = centerPoint,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.92f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Community Pulse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${mapUiState.issues.size} nearby issues mapped",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Mode: ${mapUiState.searchMode.capitalized()}",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyIssuesMap(
    issues: List<MapIssue>,
    centerPoint: GeoPoint,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        OsmConfiguration.getInstance().load(appContext, prefs)
        OsmConfiguration.getInstance().userAgentValue = appContext.packageName

        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
        }
    }
    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .pointerInteropFilter { event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN,
                    android.view.MotionEvent.ACTION_MOVE -> {
                        mapView.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        mapView.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            },
        update = { view ->
            view.overlays.clear()

            view.controller.setCenter(centerPoint)

            issues.forEach { issue ->
                val marker = Marker(view).apply {
                    position = GeoPoint(issue.lat, issue.lng)
                    title = issue.title
                    subDescription = issue.addressText ?: "Nearby issue"
                    icon = createMarkerIcon(
                        context = context,
                        fillColor = markerColorForPriority(issue.priority)
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        }
    )
}

private fun createMarkerIcon(
    context: android.content.Context,
    fillColor: Int
): BitmapDrawable {
    val size = 72
    val radius = 18f
    val cx = size / 2f
    val cy = size / 2f

    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = AndroidColor.WHITE
        strokeWidth = 6f
    }

    canvas.drawCircle(cx, cy, radius, fillPaint)
    canvas.drawCircle(cx, cy, radius, strokePaint)

    return bitmap.toDrawable(context.resources)
}

private fun markerColorForPriority(priority: String?): Int {
    return when (priority?.uppercase()) {
        "HIGH" -> "#D32F2F".toColorInt()
        "LOW" -> "#2E7D32".toColorInt()
        else -> "#F57C00".toColorInt()
    }
}

private fun statusColorsForPriority(priority: String?): Pair<Color, Color> {
    return when (priority?.uppercase()) {
        "HIGH" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "LOW" -> Color(0xFFE8F5E9) to Color(0xFF388E3C)
        else -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
    }
}

private fun statusLabelForPriority(priority: String?): String {
    return when (priority?.uppercase()) {
        "HIGH" -> "HIGH"
        "LOW" -> "LOW"
        else -> "MEDIUM"
    }
}

private fun IssueDto.toMapIssue(): MapIssue? {
    val issueLat = lat ?: return null
    val issueLng = lng ?: return null

    return MapIssue(
        id = id,
        title = title.ifBlank { "Issue" },
        addressText = addressText,
        lat = issueLat,
        lng = issueLng,
        imageUrl = resolvePreviewImageUrl(),
        priority = priority
    )
}

private fun IssueDto.resolvePreviewImageUrl(): String? {
    // API typically sends images as objects: images[].imageUrl.
    return images.firstNotNullOfOrNull { it.imageUrl?.trim()?.takeIf(String::isNotEmpty) }
        ?: imageUrls?.firstNotNullOfOrNull { it.trim().takeIf(String::isNotEmpty) }
        ?: imageUrl?.trim()?.takeIf(String::isNotEmpty)
}

private sealed interface MapUiState {
    data object Loading : MapUiState
    data class Success(
        val issues: List<MapIssue>,
        val displayInfo: String = "Nearby Issues",
        val searchMode: String = "unknown"
    ) : MapUiState
    data class Error(val message: String) : MapUiState
}

private sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val data: DashboardResponse) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

private data class MapIssue(
    val id: String,
    val title: String,
    val addressText: String?,
    val lat: Double,
    val lng: Double,
    val imageUrl: String?,
    val priority: String?
)

private const val DEFAULT_NEARBY_DISTRICT_ID = "20000001-0000-0000-0000-000000000000"

@Composable
fun CategoryChip(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F5E9),
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00875A), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color(0xFF00875A), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun IssueCard(
    title: String,
    time: String,
    desc: String,
    location: String,
    imageUrl: String?,
    status: String,
    statusColor: Color,
    statusTextColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder for issues without images
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Image Available", color = Color.Gray)
                    }
                }
                
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(time, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(desc, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, size = 14.sp, tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(location, color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color(0xFF00875A))
                }
            }
        }
    }
}

// Extension to allow Icon size in sp if needed, but usually dp is preferred. 
// Using a helper for simplicity since Icon doesn't take sp directly easily without local density.
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.TextUnit, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(16.dp), // Fixed size for simplicity
        tint = tint
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    SevaSetuTheme {
        DashboardScreen()
    }
}

private fun String.capitalized(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}