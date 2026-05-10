package com.example.sevasetu.ui.screen.Alerts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sevasetu.Dashboard
import com.example.sevasetu.data.remote.dto.NotificationDto
import com.example.sevasetu.data.repository.UserRepository
import com.example.sevasetu.network.NetworkModule
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.screen.Reports.ReportScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.utils.ThemePreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.net.toUri

class AlertsScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferenceManager = ThemePreferenceManager(this)
        enableEdgeToEdge()
        setContent {
            val themePreference = remember { themePreferenceManager.getTheme() }
            SevaSetuTheme(themePreference = themePreference) {
                AlertsScreenContent(
                    openReportUrl = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreenContent(
    openReportUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AlertsViewModel = viewModel(
        factory = AlertsViewModelFactory(UserRepository(NetworkModule.provideUserApi(context)))
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    val unread = uiState.notifications.filter { !it.read }
    val read = uiState.notifications.filter { it.read }
    val handleOpen: (com.example.sevasetu.data.remote.dto.NotificationDto) -> Unit = { notification ->
        // Mark read first
        viewModel.markAsRead(notification.id)

        // Open report/proof URL when present
        notification.reportUrl?.takeIf { it.isNotBlank() }?.let { openReportUrl(it) }
        notification.proofImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SevaSetu",
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
                    if (uiState.unreadCount > 0) {
//                        Text(
//                            "${uiState.unreadCount} unread",
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(end = 8.dp)
//                        )
                        Text(
                            "Read All",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.markAllAsRead() }
                        )
                    }
                    IconButton(onClick = { viewModel.loadNotifications(isRefresh = true) }) {
                        Icon(Icons.Default.Update, contentDescription = "Refresh")
                    }

                    IconButton(onClick = { context.startActivity(Intent(context, ProfileScreen::class.java)) }) {
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
                    onClick = { context.startActivity(Intent(context, Dashboard::class.java)) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("HOME") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { context.startActivity(Intent(context, ReportScreen::class.java)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
                    label = { Text("REPORTS") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("ALERTS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { context.startActivity(Intent(context, ProfileScreen::class.java)) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("PROFILE") }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: "Failed to load")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadNotifications() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications yet")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (unread.isNotEmpty()) {
                        item { SectionTitle("UNREAD") }
                        items(unread, key = { it.id ?: it.hashCode() }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onOpen = { handleOpen(notification) },
                                onMarkRead = { viewModel.markAsRead(notification.id) }
                            )
                        }
                    }

                    if (read.isNotEmpty()) {
                        item { SectionTitle("READ") }
                        items(read, key = { it.id ?: it.hashCode() }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onOpen = { handleOpen(notification) },
                                onMarkRead = { }
                            )
                        }
                    }
                }
            }
        }
    }

    // Issue detail modal removed — notifications no longer open issue details
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        text = label,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun NotificationCard(
    notification: NotificationDto,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit
) {
    val iconData = notificationIcon(notification.type)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = iconData.background
                ) {
                    Icon(
                        imageVector = iconData.icon,
                        contentDescription = null,
                        tint = iconData.tint,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(notification.title ?: "Notification", fontWeight = FontWeight.Bold)
                    Text(
                        relativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // If the backend appends remarks to the message string, remove that suffix
            val rawMessage = notification.message ?: ""
            val remarksText = notification.remarks?.takeIf { it.isNotBlank() }
            val displayMessage = if (remarksText != null && rawMessage.endsWith(remarksText)) {
                rawMessage.removeSuffix(remarksText).trim()
            } else rawMessage

            Text(
                text = displayMessage,
                style = MaterialTheme.typography.bodyMedium
            )

            // Show remarks (if provided) in regular size but italic
            remarksText?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }

            notification.actor?.displayName?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "By $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                // Show Open button only when there's a report or proof URL
                if (!notification.reportUrl.isNullOrBlank()) {
                    OutlinedButton(onClick = onOpen) {
                        Text("Open report")
                    }
                } else if (!notification.proofImageUrl.isNullOrBlank()) {
                    OutlinedButton(onClick = onOpen) {
                        Text("View proof")
                    }
                }

                if (!notification.read) {
                    Button(
                        onClick = onMarkRead,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Mark read")
                    }
                }
            }
        }
    }
}

private data class NotificationIconData(
    val icon: ImageVector,
    val tint: Color,
    val background: Color
)

@Composable
private fun notificationIcon(type: String?): NotificationIconData {
    return when (type) {
        "MONTHLY_REPORT" -> NotificationIconData(
            icon = Icons.Default.PictureAsPdf,
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer
        )
        "ISSUE_CLOSED", "ISSUE_RESOLVED" -> NotificationIconData(
            icon = Icons.Default.CheckCircle,
            tint = Color(0xFF146C43),
            background = Color(0xFFE7F8EF)
        )
        else -> NotificationIconData(
            icon = Icons.Default.TaskAlt,
            tint = MaterialTheme.colorScheme.secondary,
            background = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

private fun relativeTime(value: String?): String {
    if (value.isNullOrBlank()) return "Just now"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val createdAt = parser.parse(value)?.time ?: return value
        val minutes = ((Date().time - createdAt) / 60000).coerceAtLeast(0)
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            else -> "${minutes / 1440}d ago"
        }
    } catch (_: Exception) {
        value
    }
}
