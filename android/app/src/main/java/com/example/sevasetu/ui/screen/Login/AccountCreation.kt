package com.example.sevasetu.ui.screen.Login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sevasetu.data.remote.dto.RegisterRequest
import com.example.sevasetu.network.NetworkModule
import com.example.sevasetu.ui.common.AuthViewModel
import com.example.sevasetu.ui.common.AuthViewModelFactory
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.Login
import com.example.sevasetu.data.repository.AuthRepository

class AccountCreation : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SevaSetuTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White
                ) { innerPadding ->
                    AccountCreationScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = { finish() },
                        onLoginClick = { 
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        },
                        onRegistrationSuccess = {
                            // After successful registration, usually we go to Dashboard
                            // For now, let's just finish or go to Login
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountCreationScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRegistrationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository(NetworkModule.provideAuthApi(context), com.example.sevasetu.utils.TokenManager(context)) }
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repository))
    val uiState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileSize = getFileSize(context, it)
            if (fileSize > 5 * 1024 * 1024) { // 5MB limit
                Toast.makeText(context, "File size exceeds 5MB limit", Toast.LENGTH_SHORT).show()
            } else {
                selectedFileUri = it
                selectedFileName = getFileName(context, it)
            }
        }
    }

    val primaryGreen = Color(0xFF006837)
    val lightGreenBg = Color(0xFFF1F8F4)
    val bgColor = Color(0xFFF7F9FB)

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Professional Top Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "SevaSetu",
                style = TextStyle(
                    color = primaryGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create Account",
            style = TextStyle(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = (-0.5).sp
            )
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- PERSONAL INFO SECTION ---
        SectionHeader(text = "PERSONAL INFO", color = primaryGreen)
        Spacer(modifier = Modifier.height(12.dp))

        ProfessionalInputField(
            label = "FULL NAME",
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Full name",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        ProfessionalInputField(
            label = "PHONE NUMBER",
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            placeholder = "Enter Your Phone number",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        ProfessionalInputField(
            label = "EMAIL ADDRESS",
            value = emailAddress,
            onValueChange = { emailAddress = it },
            placeholder = "name@gmail.com",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        ProfessionalDropdownField(
            label = "GENDER",
            selectedValue = gender,
            options = listOf("Male", "Female", "Other"),
            onOptionSelected = { gender = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- IDENTITY VERIFICATION SECTION ---
        SectionHeader(text = "IDENTITY VERIFICATION", color = primaryGreen)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ProfessionalDropdownField(
                    label = "ID TYPE",
                    selectedValue = idType,
                    options = listOf("Aadhaar Card", "PAN Card", "Voter ID"),
                    onOptionSelected = { idType = it }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ProfessionalInputField(
                    label = "ID NUMBER",
                    value = idNumber,
                    onValueChange = { idNumber = it },
                    placeholder = "ID number",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Document Upload Button
        OutlinedButton(
            onClick = { filePickerLauncher.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, primaryGreen),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = primaryGreen
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedFileName != null) "Change Document" else "Upload ID Proof",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        if (selectedFileName != null) {
            Text(
                text = "Selected: $selectedFileName (Max 5MB)",
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        } else {
            Text(
                text = "Allowed formats: PDF, PNG, JPG (Max 5MB)",
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- YOUR LOCATION SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(text = "YOUR LOCATION", color = primaryGreen)
            
            Surface(
                onClick = { /* Handle Detect Location */ },
                color = lightGreenBg,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = primaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Detect",
                        style = TextStyle(
                            color = primaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        ProfessionalInputField(
            label = "ADDRESS",
            value = address,
            onValueChange = { address = it },
            placeholder = "House No, Street, Landmark",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProfessionalInputField(
                modifier = Modifier.weight(1f),
                label = "CITY",
                value = city,
                onValueChange = { city = it },
                placeholder = "Your city",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Right) }
            )
            ProfessionalInputField(
                modifier = Modifier.weight(1f),
                label = "PIN CODE",
                value = pinCode,
                onValueChange = { pinCode = it },
                placeholder = "000000",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        LaunchedEffect(uiState.isAuthenticated) {
            if (uiState.isAuthenticated) {
                onRegistrationSuccess()
            }
        }

        // Primary Action Button
        Button(
            onClick = {
                if (fullName.isBlank() || phoneNumber.isBlank() || emailAddress.isBlank() ||
                    gender.isBlank() || idType.isBlank() || idNumber.isBlank() ||
                    address.isBlank() || city.isBlank() || pinCode.isBlank()
                ) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.register(
                        RegisterRequest(
                            fullName = fullName,
                            email = emailAddress,
                            phoneNumber = phoneNumber,
                            gender = gender,
                            address = address,
                            city = city,
                            pinCode = pinCode,
                            idType = idType,
                            idNumber = idNumber
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(30.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Create Account",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buildAnnotatedString {
                append("Already have an account? ")
                withStyle(style = SpanStyle(
                    color = primaryGreen, 
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = TextDecoration.None
                )) {
                    append("Log In")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLoginClick() }
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 15.sp,
                color = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SectionHeader(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
fun ProfessionalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {}
) {
    val labelGray = Color(0xFF444444)
    val borderColor = Color(0xFFE0E0E0)

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelGray,
                letterSpacing = 0.3.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp, 
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF006837)),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onAny = { onImeAction() }
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = Color.LightGray,
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun ProfessionalDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val labelGray = Color(0xFF444444)
    val borderColor = Color(0xFFE0E0E0)

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelGray,
                letterSpacing = 0.3.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedValue.isEmpty()) "Select" else selectedValue,
                    style = TextStyle(
                        color = if (selectedValue.isEmpty()) Color.LightGray else Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountCreationPreview() {
    SevaSetuTheme {
        AccountCreationScreen()
    }
}

private fun getFileSize(context: Context, uri: Uri): Long {
    var size: Long = 0
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }
    return size
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}
