package com.example.sevasetu.ui.screen.Reports

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sevasetu.Dashboard
import com.example.sevasetu.ui.screen.Alerts.AlertsScreen
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme

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
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Reports") },
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filters = listOf("ALL REPORTS" to true, "PENDING" to false, "IN PR..." to false)
                    filters.forEach { (label, isSelected) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF006D47) else Color.White,
                            modifier = Modifier.height(36.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF006D47) else Color(0xFFD1D5D3))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
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

            // Report Cards
            val reports = listOf(
                Triple("Garbage accumulation at Sector 14", "Oct 24, 2023", "PENDING"),
                Triple("Broken street light flickering", "Oct 12, 2023", "RESOLVED"),
                Triple("Deep pothole hazard near Metro", "Oct 05, 2023", "IN PROGRESS"),
                Triple("Public park bench maintenance", "Sep 28, 2023", "RESOLVED")
            )

            items(reports) { (title, date, status) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5D3))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image Placeholder
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF0F0F0))
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center),
                                tint = Color.LightGray
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reported $date",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val (bgColor, txtColor, borderColor) = when (status) {
                                "PENDING" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Color(0xFFFFCDD2))
                                "RESOLVED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFC8E6C9))
                                "IN PROGRESS" -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), Color(0xFFB2DFDB))
                                else -> Triple(Color.LightGray, Color.DarkGray, Color.Gray)
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bgColor,
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Text(
                                    text = status,
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
                Spacer(modifier = Modifier.height(16.dp))
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
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyReportsScreenPreview() {
    SevaSetuTheme {
        MyReportsScreen()
    }
}
