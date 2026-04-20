package com.example.sevasetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sevasetu.ui.theme.SevaSetuTheme

class AlertsScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                AlertsScreenContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreenContent() {
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
                    IconButton(onClick = { }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                                .padding(1.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFFD1D5D3))
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier.padding(6.dp),
                                    tint = Color(0xFF00875A)
                                )
                            }
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
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("HOME") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Reports") },
                    label = { Text("REPORTS") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("ALERTS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00875A),
                        selectedTextColor = Color(0xFF00875A),
                        indicatorColor = Color(0xFFE8F5E9)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
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
                .background(Color(0xFFF2F5F3)) // Darker background for visibility
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Notifications",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Track the pulse of your civic contributions.",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SectionHeader("TODAY")
                
                NotificationItem(
                    title = "Issue Resolved",
                    time = "2h ago",
                    description = "Your report on 'Pothole at Sector 4' has been successfully resolved. Thank you for your contribution!",
                    icon = Icons.Default.CheckCircle,
                    iconBgColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF006D47),
                    hasButton = true,
                    buttonText = "View Results"
                )
                
                NotificationItem(
                    title = "Update on Report",
                    time = "5h ago",
                    description = "The status of 'Street Light Repair' has changed to In Progress. A technician has been assigned.",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    iconBgColor = Color(0xFFFFF1F1),
                    iconColor = Color(0xFFD32F2F)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SectionHeader("YESTERDAY")
                
                NotificationItem(
                    title = "Area Alert",
                    time = "1d ago",
                    description = "Scheduled maintenance for water pipelines in Vasant Kunj will occur tomorrow from 10 AM to 4 PM.",
                    icon = Icons.Default.Campaign,
                    iconBgColor = Color(0xFFF1F4F2),
                    iconColor = Color(0xFF006D47)
                )
                
                // Civic Hero Badge Card (Green)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00875A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Civic Hero Badge!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "1d ago",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Congratulations! You've reached the Silver Tier for active reporting this month.",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Map Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFFDDE2DF),
                    border = BorderStroke(1.dp, Color(0xFFD1D5D3))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Placeholder for Map Background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.1f))
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFD1D5D3)),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "Active Impact Map",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006D47)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.width(16.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
    }
}

@Composable
fun NotificationItem(
    title: String,
    time: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    hasButton: Boolean = false,
    buttonText: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFD1D5D3)) // Visible border
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = iconBgColor,
                border = BorderStroke(1.dp, iconColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
                
                if (hasButton) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D47)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp),
                        border = BorderStroke(1.dp, Color(0xFF004D32)) // Border for button visibility
                    ) {
                        Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AlertsScreenPreview() {
    SevaSetuTheme {
        AlertsScreenContent()
    }
}
