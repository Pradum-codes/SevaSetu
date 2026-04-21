package com.example.sevasetu

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.Brush
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

    val fetchNearbyIssues: () -> Unit = {
        scope.launch {
            mapUiState = MapUiState.Loading
            issueRepository.getNearbyIssues(DEFAULT_NEARBY_DISTRICT_ID)
                .onSuccess { issues ->
                    val mapIssues = issues.mapNotNull { it.toMapIssue() }
                    mapUiState = MapUiState.Success(mapIssues)
                }
                .onFailure { throwable ->
                    mapUiState = MapUiState.Error(
                        throwable.message ?: "Unable to load nearby issues"
                    )
                }
        }
    }

    LaunchedEffect(Unit) {
        fetchNearbyIssues()
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
                onRetry = fetchNearbyIssues
            )

            Spacer(Modifier.height(16.dp))

            // My Impact Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF00875A)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF00875A), Color(0xFF006D47))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text("MY IMPACT", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("08", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Text("Issues resolved this month", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Contributions", color = Color.White)
                        }
                    }
                    // Decorative Leaf Icon (Placeholder)
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp).align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick Insights
            Text("Quick Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
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
                        Text("Avg. Response", color = Color.Gray, fontSize = 14.sp)
                        Text("2.4 Days", color = Color(0xFF00875A), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Top Category", color = Color.Gray, fontSize = 14.sp)
                        Text("Sanitation", color = Color(0xFF00875A), fontWeight = FontWeight.Bold)
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

                IssueCard(
                    title = issue.title,
                    time = "Nearby",
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
    onRetry: () -> Unit
) {
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
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyIssuesMap(
    issues: List<MapIssue>,
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

            val points = issues.map { issue ->
                GeoPoint(issue.lat, issue.lng)
            }
            val center = points.firstOrNull() ?: GeoPoint(23.2599, 77.4126)
            view.controller.setCenter(center)

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
    data class Success(val issues: List<MapIssue>) : MapUiState
    data class Error(val message: String) : MapUiState
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

private const val DEFAULT_NEARBY_DISTRICT_ID = "22222222-2222-2222-2222-222222222222"

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
