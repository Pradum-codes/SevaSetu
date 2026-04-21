package com.example.sevasetu.ui.screen.Reports

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.sevasetu.utils.CategoryConstants

class IssueReport : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                IssueReportScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueReportScreen() {
    val context = LocalContext.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(CategoryConstants.ROADS_INFRASTRUCTURE_ID) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Report Issue",
                        color = Color(0xFF006D47),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { backPressedDispatcher?.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF006D47))
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
                                tint = Color(0xFF006D47)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
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
                    onClick = {
                        // Already in reports flow
                    },
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
                .background(Color(0xFFF8FAF9)) // Light grey-green background for professional look
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NEW SUBMISSION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006D47),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Identify the Civic Concern",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1C1A),
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your report helps us keep our neighborhood thriving and safe.",
                    fontSize = 15.sp,
                    color = Color(0xFF5F635F),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Photo Upload Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UploadOption(
                        icon = Icons.Default.PhotoCamera,
                        label = "Take Photo",
                        modifier = Modifier.weight(1f)
                    )
                    UploadOption(
                        icon = Icons.Default.FileUpload,
                        label = "Gallery",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Location Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3F1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DETECTED LOCATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF747974)
                            )
                            Text(
                                text = "Change",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006D47),
                                modifier = Modifier.clickable { }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF006D47),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sector 15, Rohini, New Delhi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1A)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Map Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0D2421)) // Dark forest green/black for map
                        ) {
                            // Map representation
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Subtly draw map grid or something
                            }
                            // Map Pin icons
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).size(52.dp).offset(y = (-12).dp),
                                tint = Color(0xFF4DB6AC)
                            )
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).size(24.dp).offset(x = (-70).dp, y = (-25).dp),
                                tint = Color(0xFF4DB6AC).copy(alpha = 0.4f)
                            )
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).size(24.dp).offset(x = (50).dp, y = (-35).dp),
                                tint = Color(0xFF4DB6AC).copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Issue Category
                Text(
                    text = "ISSUE CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF747974)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CategoryConstants.getCategoryName(selectedCategoryId) ?: "Select Category",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1C1A)
                            )
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF747974)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFDDE4DE), thickness = 1.5.dp)
                    }

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        CategoryConstants.CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Detailed Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETAILED DESCRIPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF747974)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color(0xFF006D47),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VOICE INPUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006D47)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            "Provide details like exact landmark or severity...",
                            color = Color(0xFFAFB4AF),
                            fontSize = 16.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color(0xFFDDE4DE),
                        focusedIndicatorColor = Color(0xFF006D47)
                    )
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Submit Button
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFF00875A).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Submit Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Your report will be verified by local community heads.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color(0xFF747974)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun UploadOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .clickable { }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFFBCC2BC), // Darker dashed border for better visibility
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                ),
                cornerRadius = CornerRadius(32.dp.toPx())
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF006D47), modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun IssueReportScreenPreview() {
    SevaSetuTheme {
        IssueReportScreen()
    }
}
