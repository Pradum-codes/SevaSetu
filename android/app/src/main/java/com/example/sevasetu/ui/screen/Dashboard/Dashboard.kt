package com.example.sevasetu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sevasetu.ui.screen.Alerts.AlertsScreen
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.screen.Reports.ReportScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme

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
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Reports") },
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

            // Map Area Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFA5D6A7)) // Light green map-like background
            ) {
                // Map markers placeholder
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFF00875A),
                    modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp)
                )
                Icon(
                    Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.align(Alignment.Center).offset(x = 60.dp, y = 10.dp)
                )
                Icon(
                    Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.align(Alignment.Center).offset(x = (-60).dp, y = 40.dp)
                )

                // Community Pulse Info Box
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Community Pulse", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Live updates from your sector", fontSize = 10.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00875A)))
                            Spacer(Modifier.width(4.dp))
                            Text("12 RESOLVED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                            Spacer(Modifier.width(4.dp))
                            Text("5 ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Zoom Buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { },
                        containerColor = Color.White,
                        contentColor = Color.Gray,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) { Icon(Icons.Default.Add, contentDescription = null) }
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { },
                        containerColor = Color.White,
                        contentColor = Color.Gray,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape
                    ) { Icon(Icons.Default.Remove, contentDescription = null) }
                }
            }

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
                    Icon(Icons.Default.ArrowForward, contentDescription = null, size = 16.sp, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Issue Cards
            IssueCard(
                title = "Garbage Overflow near Park",
                time = "2h ago",
                desc = "The central bin near Sector 5 Park entrance is overflowing and attracting strays. Needs urge...",
                location = "Sector 5, Main Gate",
                status = "PENDING",
                statusColor = Color(0xFFFFEBEE),
                statusTextColor = Color(0xFFD32F2F)
            )
            Spacer(Modifier.height(16.dp))
            IssueCard(
                title = "Non-functional Streetlight",
                time = "5h ago",
                desc = "Three lights in a row are out on Avenue 12. Unsafe for pedestrians at night.",
                location = "Avenue 12, West Side",
                status = "IN PROGRESS",
                statusColor = Color(0xFFE3F2FD),
                statusTextColor = Color(0xFF1976D2)
            )
            Spacer(Modifier.height(16.dp))
            IssueCard(
                title = "Major Pothole Fixed",
                time = "1d ago",
                desc = "The deep pothole on MG Road has been filled and leveled. Traffic flow is now back to normal.",
                location = "MG Road, Intersection",
                status = "RESOLVED",
                statusColor = Color(0xFFE8F5E9),
                statusTextColor = Color(0xFF388E3C)
            )
        }
    }
}

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
                // Placeholder for Issue Image
                Text("Image Placeholder", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                
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
