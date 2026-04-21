package com.example.sevasetu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sevasetu.data.repository.AuthContainer
import com.example.sevasetu.ui.common.AuthViewModel
import com.example.sevasetu.ui.common.AuthViewModelFactory
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.ui.screen.Login.AccountCreation

class Login : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthContainer.provideAuthRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authViewModel.restoreSession()) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                LoginScreen(
                    authViewModel = authViewModel,
                    onAuthSuccess = {
                        startActivity(Intent(this, Dashboard::class.java))
                        finish()
                    },
                    onCreateAccountClick = {
                        startActivity(Intent(this, AccountCreation::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onCreateAccountClick: () -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    val primaryGreen = Color(0xFF006D44)
    val bgGradientStart = Color(0xFFF2F9F6)
    val bgGradientEnd = Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(bgGradientStart, bgGradientEnd)
                    )
                )
        )

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))

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

                InputLabel("EMAIL ADDRESS")
                Spacer(modifier = Modifier.height(10.dp))
                CustomInputField {
                    BasicTextField(
                        value = uiState.email,
                        onValueChange = { authViewModel.onEmailChanged(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                        decorationBox = { inner ->
                            if (uiState.email.isEmpty()) {
                                Text("Enter your email", color = Color.Gray.copy(0.45f), fontSize = 18.sp)
                            }
                            inner()
                        }
                    )
                }

                if (uiState.otpSent) {
                    Spacer(modifier = Modifier.height(16.dp))
                    InputLabel("EMAIL OTP")
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInputField {
                        BasicTextField(
                            value = uiState.otp,
                            onValueChange = {
                                val digitsOnly = it.filter(Char::isDigit).take(6)
                                authViewModel.onOtpChanged(digitsOnly)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                            decorationBox = { inner ->
                                if (uiState.otp.isEmpty()) {
                                    Text("Enter 6-digit OTP", color = Color.Gray.copy(0.45f), fontSize = 18.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (uiState.otpSent) {
                            authViewModel.verifyOtp()
                        } else {
                            authViewModel.sendOtp()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    enabled = !uiState.isLoading
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.otpSent) "Verify OTP" else "Get OTP",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "→", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.errorMessage != null || uiState.infoMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.errorMessage ?: uiState.infoMessage ?: "",
                        color = if (uiState.errorMessage != null) Color(0xFFB3261E) else Color(0xFF006D44),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

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
                    modifier = Modifier.clickable { onCreateAccountClick() }
                )
            }
        }
    }
}

//@Composable
//fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
//    Surface(
//        shape = RoundedCornerShape(16.dp),
//        color = if (isSelected) Color.White else Color.Transparent,
//        shadowElevation = if (isSelected) 3.dp else 0.dp,
//        modifier = modifier.clickable { onClick() }
//    ) {
//        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
//            Text(
//                text = text,
//                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
//                color = if (isSelected) Color(0xFF006D44) else Color.Gray,
//                fontSize = 15.sp
//            )
//        }
//    }
//}

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

//@Composable
//fun SocialButton(iconRes: Int, label: String, modifier: Modifier) {
//    Surface(
//        modifier = modifier.height(60.dp),
//        shape = RoundedCornerShape(16.dp),
//        color = Color.White,
//        border = BorderStroke(1.dp, Color(0xFFE0EAE4)),
//        onClick = { }
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center,
//            modifier = Modifier.fillMaxSize()
//        ) {
//            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
//            Spacer(modifier = Modifier.width(12.dp))
//            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
//        }
//    }
//}
