package com.example.sevasetu.ui.screen.Reports

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.outlined.ThumbUp
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
import com.example.sevasetu.utils.ThemePreferenceManager
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.remote.dto.TimelineUpdateDto
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
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferenceManager = ThemePreferenceManager(this)
        enableEdgeToEdge()
        setContent {
            val themePreference = remember { themePreferenceManager.getTheme() }
            SevaSetuTheme(themePreference = themePreference) {
                MyReportsScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
    var voteInFlight by remember { mutableStateOf(false) }
    var selectedIssueTimeline by remember { mutableStateOf<List<TimelineUpdateDto>?>(null) }
    var isTimelineLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIssue?.id) {
        val issueId = selectedIssue?.id
        if (issueId != null) {
            isTimelineLoading = true
            selectedIssueTimeline = null
            issueRepository.getIssueTimeline(issueId)
                .onSuccess { response ->
                    selectedIssueTimeline = response.timeline
                }
                .onFailure {
                    // Fail silently
                }
            isTimelineLoading = false
        }
    }

    val handleVote: (IssueDto) -> Unit = { issue ->
        if (!voteInFlight) {
            voteInFlight = true
            scope.launch {
                issueRepository.voteIssue(issue.id)
                    .onSuccess { response ->
                        // Update local vote tracker
                        val tokenManager = com.example.sevasetu.utils.TokenManager(context)
                        if (response.voted) {
                            tokenManager.addVotedIssue(issue.id)
                        } else {
                            tokenManager.removeVotedIssue(issue.id)
                        }

                        if (selectedIssue?.id == issue.id) {
                            selectedIssue = selectedIssue?.copy(
                                voteCount = response.totalVotes,
                                isVotedByMe = response.voted
                            )
                        }

                        // Update in uiState lists
                        val updatedFullIssues = uiState.fullIssues.map {
                            if (it.id == issue.id) it.copy(
                                voteCount = response.totalVotes,
                                isVotedByMe = response.voted
                            ) else it
                        }
                        uiState = uiState.copy(
                            fullIssues = updatedFullIssues,
                            reports = updatedFullIssues.map { it.toReportListItem() }
                        )
                    }
                    .onFailure {
                        android.widget.Toast.makeText(
                            context,
                            "Failed to update vote: ${it.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                voteInFlight = false
            }
        }
    }

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
                    val tokenManager = com.example.sevasetu.utils.TokenManager(context)
                    val votedSet = tokenManager.getVotedIssues()
                    val enrichedIssues = response.issues.map {
                        it.copy(isVotedByMe = it.isVotedByMe ?: votedSet.contains(it.id))
                    }

                    uiState = ReportsUiState(
                        isLoading = false,
                        reports = enrichedIssues.map { it.toReportListItem() },
                        fullIssues = enrichedIssues
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
                        color = MaterialTheme.colorScheme.primary,
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
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.align(Alignment.Center).size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
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
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "My Reports",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tracking your contributions to a cleaner city.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Search Bar
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Search reports...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    if (selectedFilter != filter) {
                                        selectedFilter = filter
                                    }
                                },
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "No reports found for this filter.",
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            context.startActivity(Intent(context, IssueReport::class.java))
                        }
                ) {
                    val dashColor = MaterialTheme.colorScheme.outline
                    Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                        drawRoundRect(
                            color = dashColor,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Don't see an issue? Report one now\nto help your community grow.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Submit New Report",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
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
            onDismiss = { selectedIssue = null },
            onVoteClick = { handleVote(selectedIssue!!) },
            isVoteLoading = voteInFlight,
            timeline = selectedIssueTimeline,
            isTimelineLoading = isTimelineLoading
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
    val imageUrl: String?,
    val voteCount: Int,
    val isVotedByMe: Boolean
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Reported ${report.reportedDateLabel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Vote Count in Reports List
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (report.isVotedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    tint = if (report.isVotedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = report.voteCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (report.isVotedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun reportStatusColors(status: String): Triple<Color, Color, Color> {
    return when (status.uppercase(Locale.ROOT)) {
        "OPEN" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        "RESOLVED" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        "IN_PROGRESS" -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        "REJECTED" -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
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

private fun previewReports(): List<ReportListItem> {
    return listOf(
        ReportListItem(
            id = "1",
            title = "Garbage accumulation at Sector 14",
            reportedDateLabel = "24 Oct, 2023",
            statusLabel = "OPEN",
            statusRaw = "OPEN",
            imageUrl = null,
            voteCount = 5,
            isVotedByMe = false
        ),
        ReportListItem(
            id = "2",
            title = "Broken street light flickering",
            reportedDateLabel = "12 Oct, 2023",
            statusLabel = "RESOLVED",
            statusRaw = "RESOLVED",
            imageUrl = null,
            voteCount = 10,
            isVotedByMe = true
        )
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyReportsScreenPreview() {
    SevaSetuTheme {
        MyReportsScreen()
    }
}
