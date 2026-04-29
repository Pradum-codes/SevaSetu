package com.example.sevasetu.ui.screen.Reports

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.sevasetu.Dashboard
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.repository.IssueRepository
import com.example.sevasetu.network.ApiService
import com.example.sevasetu.ui.common.IssueDetailModal
import com.example.sevasetu.ui.screen.Alerts.AlertsScreen
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReportScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                MyReportsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val issueRepository = remember(context) { IssueRepository(ApiService.issueApi(context)) }

    var selectedFilter by rememberSaveable { mutableStateOf(ReportStatusFilter.ALL) }
    var uiState by remember { mutableStateOf(ReportsUiState(isLoading = true)) }
    var selectedIssue by remember { mutableStateOf<IssueDto?>(null) }

    val refreshReports: () -> Unit = {
        if (isPreview) {
            uiState = ReportsUiState(reports = previewReports())
        } else {
            scope.launch {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                val result = issueRepository.getMyReportedIssues(
                    page = 1,
                    limit = 50,
                    status = selectedFilter.apiValue
                )

                result.onSuccess { response ->
                    uiState = ReportsUiState(
                        isLoading = false,
                        reports = response.issues.map { it.toReportListItem() },
                        fullIssues = response.issues
                    )
                }.onFailure { throwable ->
                    uiState = ReportsUiState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load reports"
                    )
                }
            }
        }
    }

    LaunchedEffect(selectedFilter, isPreview) {
        refreshReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Reports",
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.align(Alignment.Center).size(24.dp),
                                tint = Color(0xFF00875A)
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
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, Dashboard::class.java))
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("HOME") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
                    label = { Text("REPORTS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00875A),
                        selectedTextColor = Color(0xFF00875A),
                        indicatorColor = Color(0xFFE8F5E9)
                    )
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF2F5F3)) // Distinct background for card contrast
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My Reports",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tracking your contributions to a cleaner city.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Search Bar
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Search reports...", color = Color.LightGray, fontSize = 16.sp)
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReportStatusFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF006D47) else Color.White,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    if (selectedFilter != filter) {
                                        selectedFilter = filter
                                    }
                                },
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF006D47) else Color(0xFFD1D5D3))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00875A))
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    color = Color(0xFF2D2D2D),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = refreshReports) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                uiState.reports.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = "No reports found for this filter.",
                                modifier = Modifier.padding(20.dp),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    items(uiState.reports, key = { it.id }) { report ->
                        val fullIssue = uiState.fullIssues.find { it.id == report.id }
                        ReportIssueCard(
                            report = report,
                            onClick = { fullIssue?.let { selectedIssue = it } }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Submit New Report Section with dashed border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .clickable {
                            context.startActivity(Intent(context, IssueReport::class.java))
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                        drawRoundRect(
                            color = Color(0xFFBDBDBD),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            ),
                            cornerRadius = CornerRadius(32.dp.toPx())
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Don't see an issue? Report one now\nto help your community grow.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Submit New Report",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00875A),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Show issue detail modal
    if (selectedIssue != null) {
        IssueDetailModal(
            issue = selectedIssue!!,
            onDismiss = { selectedIssue = null }
        )
    }
}

private enum class ReportStatusFilter(val label: String, val apiValue: String?) {
    ALL("ALL REPORTS", null),
    OPEN("OPEN", "OPEN"),
    IN_PROGRESS("IN PROGRESS", "IN_PROGRESS"),
    RESOLVED("RESOLVED", "RESOLVED"),
    REJECTED("REJECTED", "REJECTED")
}

private data class ReportsUiState(
    val isLoading: Boolean = false,
    val reports: List<ReportListItem> = emptyList(),
    val fullIssues: List<IssueDto> = emptyList(),
    val errorMessage: String? = null
)

private data class ReportListItem(
    val id: String,
    val title: String,
    val reportedDateLabel: String,
    val statusLabel: String,
    val statusRaw: String,
    val imageUrl: String?
)

@Composable
private fun ReportIssueCard(
    report: ReportListItem,
    onClick: () -> Unit = {}
) {
    val (bgColor, txtColor, borderColor) = reportStatusColors(report.statusRaw)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFD1D5D3))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                if (!report.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = report.imageUrl,
                        contentDescription = report.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Reported ${report.reportedDateLabel}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        text = report.statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = txtColor
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun reportStatusColors(status: String): Triple<Color, Color, Color> {
    return when (status.uppercase(Locale.ROOT)) {
        "OPEN" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Color(0xFFFFCDD2))
        "RESOLVED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFC8E6C9))
        "IN_PROGRESS" -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), Color(0xFFB2DFDB))
        "REJECTED" -> Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), Color(0xFFFFE0B2))
        else -> Triple(Color.LightGray, Color.DarkGray, Color.Gray)
    }
}

private fun IssueDto.toReportListItem(): ReportListItem {
    val normalizedStatus = status?.uppercase(Locale.ROOT) ?: "OPEN"
    return ReportListItem(
        id = id,
        title = title.ifBlank { "Untitled issue" },
        reportedDateLabel = createdAt.toReportDateLabel(),
        statusLabel = normalizedStatus.replace('_', ' '),
        statusRaw = normalizedStatus,
        imageUrl = resolvePreviewImageUrl()
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

private fun previewReports(): List<ReportListItem> {
    return listOf(
        ReportListItem(
            id = "1",
            title = "Garbage accumulation at Sector 14",
            reportedDateLabel = "24 Oct, 2023",
            statusLabel = "OPEN",
            statusRaw = "OPEN",
            imageUrl = null
        ),
        ReportListItem(
            id = "2",
            title = "Broken street light flickering",
            reportedDateLabel = "12 Oct, 2023",
            statusLabel = "RESOLVED",
            statusRaw = "RESOLVED",
            imageUrl = null
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyReportsScreenPreview() {
    SevaSetuTheme {
        MyReportsScreen()
    }
}
