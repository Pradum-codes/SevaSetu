package com.example.sevasetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sevasetu.ui.theme.SevaSetuTheme

class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Phone") }

    val primaryGreen = Color(0xFF006D44)
    // Professional background tones: A very subtle mint-to-white gradient
    val bgGradientStart = Color(0xFFF2F9F6)
    val bgGradientEnd = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 🔹 Professional Background: Gradient + Fixed Watermark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(bgGradientStart, bgGradientEnd)
                    )
                )
        )

        // Improved background watermark visibility
        Image(
            painter = painterResource(id = R.drawable.backgroundscreen),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // 🔹 Main Adaptive Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // 🔹 Professional Logo & Branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = primaryGreen.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.appicon),
                            contentDescription = "logo",
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "SevaSetu",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryGreen,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 🔹 Welcome Section
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Sign in to continue your civic journey.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(44.dp))

                // 🔹 Professional Tab Switcher
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F0EB), // Slightly more tinted to show structure
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(5.dp)) {
                        TabItem(
                            text = "Phone Number",
                            isSelected = selectedTab == "Phone",
                            onClick = { selectedTab = "Phone" },
                            modifier = Modifier.weight(1f)
                        )
                        TabItem(
                            text = "Email Address",
                            isSelected = selectedTab == "Email",
                            onClick = { selectedTab = "Email" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 🔹 Dynamic Input Section
                if (selectedTab == "Phone") {
                    InputLabel("MOBILE NUMBER")
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInputField {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "+91", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))
                            Spacer(modifier = Modifier.width(14.dp))
                            BasicTextField(
                                value = phone,
                                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                                decorationBox = { inner ->
                                    if (phone.isEmpty()) {
                                        Text("Enter Mobile Number", color = Color.Gray.copy(0.45f), fontSize = 18.sp)
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                } else {
                    InputLabel("EMAIL ADDRESS")
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInputField {
                        BasicTextField(
                            value = email,
                            onValueChange = { email = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                            decorationBox = { inner ->
                                if (email.isEmpty()) {
                                    Text("Enter your email", color = Color.Gray.copy(0.45f), fontSize = 18.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 🔹 Primary Action Button
                Button(
                    onClick = { /* TODO: OTP logic */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Get OTP", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "→", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // 🔹 Professional Divider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                    Text(
                        text = " OR CONTINUE WITH ",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 🔹 Social Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialButton(
                        iconRes = R.drawable.iconsgoogle,
                        label = "Google",
                        modifier = Modifier.weight(1f)
                    )
                    SocialButton(
                        iconRes = R.drawable.iconsbiometric,
                        label = "Biometric",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // 🔹 Adaptive Footer (Always at bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "New to SevaSetu? ", color = Color.Black, fontSize = 16.sp)
                Text(
                    text = "Create an Account",
                    color = primaryGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { /* Navigate to register */ }
                )
            }
        }
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF006D44) else Color.Gray,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(0.8f),
            letterSpacing = 0.8.sp
        )
    )
}

@Composable
fun CustomInputField(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.6f),
        border = BorderStroke(1.5.dp, Color(0xFFE0EAE4)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            content()
        }
    }
}

@Composable
fun SocialButton(iconRes: Int, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0EAE4)),
        onClick = { /* Social login action */ }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    SevaSetuTheme {
        LoginScreen()
    }
}
