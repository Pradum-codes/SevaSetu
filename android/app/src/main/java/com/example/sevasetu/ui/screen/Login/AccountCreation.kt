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
import com.example.sevasetu.utils.JurisdictionConstants
import com.example.sevasetu.utils.TokenManager

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
    val repository = remember { AuthRepository(NetworkModule.provideAuthApi(context), TokenManager(context)) }
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repository))
    val uiState by viewModel.uiState.collectAsState()

    // ==================== PERSONAL INFO ====================
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // ==================== IDENTITY VERIFICATION ====================
    var idType by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // ==================== LOCATION & JURISDICTION ====================
    var selectedDistrict by remember { mutableStateOf("") }
    var selectedDistrictId by remember { mutableStateOf("") }
    var areaType by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var selectedWard by remember { mutableStateOf("") }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedPanchayat by remember { mutableStateOf("") }
    var addressLocality by remember { mutableStateOf("") }
    var addressLandmark by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }

    // ==================== VALIDATION STATE ====================
    var validationErrors by remember { mutableStateOf(ValidationErrors()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileSize = getFileSize(context, it)
            if (fileSize > 5 * 1024 * 1024) {
                Toast.makeText(context, "File size exceeds 5MB limit", Toast.LENGTH_SHORT).show()
            } else {
                selectedFileName = getFileName(context, it)
            }
        }
    }

    val primaryGreen = Color(0xFF006837)
    val lightGreenBg = Color(0xFFF1F8F4)
    val bgColor = Color(0xFFF7F9FB)

    val focusManager = LocalFocusManager.current

    // Auto-populate area type based on district selection
    LaunchedEffect(selectedDistrictId) {
        if (selectedDistrictId.isNotEmpty()) {
            areaType = JurisdictionConstants.getCategory(selectedDistrictId)

            // Auto-populate city/ward or block/panchayat
            if (JurisdictionConstants.isUrban(selectedDistrictId)) {
                val urbanLocation = JurisdictionConstants.getUrbanLocation(selectedDistrictId)
                selectedCity = urbanLocation?.cityName ?: ""
                selectedWard = urbanLocation?.wardName ?: ""
            } else {
                val ruralLocation = JurisdictionConstants.getRuralLocation(selectedDistrictId)
                selectedBlock = ruralLocation?.blockName ?: ""
                selectedPanchayat = ruralLocation?.panchayatName ?: ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
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

        if (uiState.infoMessage != null) {
            Text(
                text = uiState.infoMessage!!,
                color = primaryGreen,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ========== PERSONAL INFO SECTION ==========
        SectionHeader(text = "PERSONAL INFO", color = primaryGreen)
        Spacer(modifier = Modifier.height(12.dp))

        ProfessionalInputField(
            label = "FULL NAME",
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Enter your name as per ID",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            hasError = validationErrors.fullName
        )

        ProfessionalInputField(
            label = "PHONE NUMBER",
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            placeholder = "Enter Your Phone number",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            hasError = validationErrors.phoneNumber
        )

        ProfessionalInputField(
            label = "EMAIL ADDRESS",
            value = emailAddress,
            onValueChange = { emailAddress = it },
            placeholder = "name@gmail.com",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            hasError = validationErrors.emailAddress
        )

        ProfessionalDropdownField(
            label = "GENDER",
            selectedValue = gender,
            options = listOf("Male", "Female", "Other"),
            onOptionSelected = { gender = it },
            hasError = validationErrors.gender
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ========== IDENTITY VERIFICATION SECTION ==========
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
                    onOptionSelected = { idType = it },
                    hasError = validationErrors.idType
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ProfessionalInputField(
                    label = "ID NUMBER",
                    value = idNumber,
                    onValueChange = { idNumber = it },
                    placeholder = "ID number",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    hasError = validationErrors.idNumber
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGreen)
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
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        if (selectedFileName != null) {
            Text(
                text = "Selected: $selectedFileName (Max 5MB)",
                style = TextStyle(color = Color.Gray, fontSize = 12.sp),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ========== LOCATION & JURISDICTION SECTION ==========
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

        // State (Fixed as Punjab)
        ProfessionalInputField(
            label = "STATE",
            value = JurisdictionConstants.PUNJAB_STATE_NAME,
            onValueChange = {},
            placeholder = "Punjab",
            enabled = false
        )

        // District Selection
        ProfessionalDropdownField(
            label = "DISTRICT",
            selectedValue = selectedDistrict,
            options = JurisdictionConstants.DISTRICTS.map { it.name },
            onOptionSelected = { districtName ->
                selectedDistrict = districtName
                val district = JurisdictionConstants.DISTRICTS.find { it.name == districtName }
                selectedDistrictId = district?.id ?: ""
            },
            hasError = validationErrors.district
        )

        // Area Type (Auto-populated, read-only display)
        if (areaType.isNotEmpty()) {
            ProfessionalInputField(
                label = "AREA TYPE",
                value = if (areaType == "URBAN") "Urban" else "Rural",
                onValueChange = {},
                placeholder = "Auto-populated",
                enabled = false
            )
        }

        // Urban-specific fields
        if (areaType == "URBAN" && selectedDistrictId.isNotEmpty()) {
            ProfessionalInputField(
                label = "CITY",
                value = selectedCity,
                onValueChange = {},
                placeholder = "Auto-populated",
                enabled = false
            )

            ProfessionalInputField(
                label = "WARD",
                value = selectedWard,
                onValueChange = {},
                placeholder = "Auto-populated",
                enabled = false
            )
        }

        // Rural-specific fields
        if (areaType == "RURAL" && selectedDistrictId.isNotEmpty()) {
            ProfessionalInputField(
                label = "BLOCK",
                value = selectedBlock,
                onValueChange = {},
                placeholder = "Auto-populated",
                enabled = false
            )

            ProfessionalInputField(
                label = "PANCHAYAT",
                value = selectedPanchayat,
                onValueChange = {},
                placeholder = "Auto-populated",
                enabled = false
            )
        }

        // Additional address fields
        ProfessionalInputField(
            label = "LOCALITY",
            value = addressLocality,
            onValueChange = { addressLocality = it },
            placeholder = "Enter locality (Optional)",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        ProfessionalInputField(
            label = "LANDMARK",
            value = addressLandmark,
            onValueChange = { addressLandmark = it },
            placeholder = "Enter landmark (Optional)",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
        )

        ProfessionalInputField(
            label = "FULL ADDRESS",
            value = fullAddress,
            onValueChange = { fullAddress = it },
            placeholder = "House No, Street, etc.",
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            hasError = validationErrors.fullAddress
        )

        ProfessionalInputField(
            label = "PIN CODE",
            value = pinCode,
            onValueChange = { pinCode = it },
            placeholder = "000000",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            hasError = validationErrors.pinCode
        )

        Spacer(modifier = Modifier.height(60.dp))

        LaunchedEffect(uiState.registrationCompleted) {
            if (uiState.registrationCompleted) {
                onRegistrationSuccess()
            }
        }

        // Register Button
        Button(
            onClick = {
                // Validate all required fields
                validationErrors = ValidationErrors(
                    fullName = fullName.isBlank(),
                    phoneNumber = phoneNumber.isBlank(),
                    emailAddress = emailAddress.isBlank(),
                    gender = gender.isBlank(),
                    idType = idType.isBlank(),
                    idNumber = idNumber.isBlank(),
                    district = selectedDistrictId.isBlank(),
                    fullAddress = fullAddress.isBlank(),
                    pinCode = pinCode.isBlank()
                )

                // Check if there are any errors
                if (validationErrors.hasErrors()) {
                    Toast.makeText(
                        context,
                        "Please fill all required fields (marked in red)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Build the request with proper jurisdiction IDs
                    val finalJurisdictionId = JurisdictionConstants.getFinalJurisdictionId(selectedDistrictId) ?: ""
                    val urbanLoc = JurisdictionConstants.getUrbanLocation(selectedDistrictId)
                    val ruralLoc = JurisdictionConstants.getRuralLocation(selectedDistrictId)

                    val registerRequest = RegisterRequest(
                        name = fullName,
                        email = emailAddress,
                        phone = phoneNumber,
                        gender = gender,
                        idType = idType,
                        idNumber = idNumber,
                        jurisdictionId = finalJurisdictionId,
                        addressDistrict = selectedDistrictId,
                        addressAreaType = areaType,
                        addressCity = urbanLoc?.cityId,
                        addressWard = urbanLoc?.wardId,
                        addressBlock = ruralLoc?.blockId,
                        addressPanchayat = ruralLoc?.panchayatId,
                        addressLocality = addressLocality.ifBlank { null },
                        addressLandmark = addressLandmark.ifBlank { null },
                        addressText = fullAddress,
                        pinCode = pinCode
                    )

                    viewModel.register(registerRequest)
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
            style = TextStyle(fontSize = 15.sp, color = Color.Gray)
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

/**
 * Data class to track validation errors for form fields
 */
data class ValidationErrors(
    val fullName: Boolean = false,
    val phoneNumber: Boolean = false,
    val emailAddress: Boolean = false,
    val gender: Boolean = false,
    val idType: Boolean = false,
    val idNumber: Boolean = false,
    val district: Boolean = false,
    val fullAddress: Boolean = false,
    val pinCode: Boolean = false
) {
    fun hasErrors(): Boolean {
        return fullName || phoneNumber || emailAddress || gender || idType || idNumber ||
                district || fullAddress || pinCode
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
    onImeAction: () -> Unit = {},
    enabled: Boolean = true,
    hasError: Boolean = false
) {
    val labelGray = Color(0xFF444444)
    val borderColor = if (hasError) Color(0xFFD32F2F) else Color(0xFFE0E0E0)
    val labelColor = if (hasError) Color(0xFFD32F2F) else labelGray

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    letterSpacing = 0.3.sp
                )
            )
            if (hasError) {
                Text(
                    text = "Required",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) Color.White else Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = if (enabled) onValueChange else { {} },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = if (enabled) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF006837)),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                singleLine = true,
                enabled = enabled,
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
    modifier: Modifier = Modifier,
    hasError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val labelGray = Color(0xFF444444)
    val borderColor = if (hasError) Color(0xFFD32F2F) else Color(0xFFE0E0E0)
    val labelColor = if (hasError) Color(0xFFD32F2F) else labelGray

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    letterSpacing = 0.3.sp
                )
            )
            if (hasError) {
                Text(
                    text = "Required",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
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
