package com.example.sevasetu.ui.screen.Reports

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.sevasetu.Dashboard
import com.example.sevasetu.data.remote.cloudinary.CloudinaryUploader
import com.example.sevasetu.data.remote.dto.CreateIssueRequest
import com.example.sevasetu.data.repository.IssueRepository
import com.example.sevasetu.network.NetworkModule
import com.example.sevasetu.ui.screen.Alerts.AlertsScreen
import com.example.sevasetu.ui.screen.Profile.ProfileScreen
import com.example.sevasetu.ui.theme.SevaSetuTheme
import com.example.sevasetu.utils.CategoryConstants
import com.example.sevasetu.utils.JurisdictionConstants
import com.example.sevasetu.utils.LocationAddressResolver
import com.example.sevasetu.utils.ResolvedLocationAddress
import com.example.sevasetu.utils.TokenManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.location.LocationManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

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

private const val MAX_REPORT_IMAGES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueReportScreen() {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scope = rememberCoroutineScope()
    val appContext = context.applicationContext

    val tokenManager = if (inPreview) {
        null
    } else {
        remember(appContext) { TokenManager(appContext) }
    }

    val issueRepository = if (inPreview) {
        null
    } else {
        remember(appContext) { IssueRepository(NetworkModule.provideIssueApi(appContext)) }
    }

    val cloudinaryUploader = if (inPreview) {
        null
    } else {
        remember(appContext) { CloudinaryUploader(appContext) }
    }

    val fusedLocationClient = if (inPreview) {
        null
    } else {
        remember(appContext) { LocationServices.getFusedLocationProviderClient(appContext) }
    }

    val locationAddressResolver = if (inPreview) {
        null
    } else {
        remember(appContext) { LocationAddressResolver(appContext) }
    }

    val clientId = rememberSaveable { UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf("") }
    var addressText by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(CategoryConstants.ROADS_INFRASTRUCTURE_ID) }
    var selectedImageUris by rememberSaveable { mutableStateOf(listOf<String>()) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var selectedPriority by rememberSaveable { mutableStateOf("medium") }
    var isPriorityExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var formMessage by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }
    var resolvedLocationAddress by remember { mutableStateOf<ResolvedLocationAddress?>(null) }
    var locationStatus by remember { mutableStateOf("Location not detected yet") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val profileDistrictId = if (inPreview) {
        JurisdictionConstants.AMRITSAR_DISTRICT_ID
    } else {
        tokenManager?.getUserDistrict()?.trim().orEmpty()
    }

    val detectedMatchedDistrictId = resolvedLocationAddress?.matchedDistrict?.id.orEmpty()
    val reportDistrictId = detectedMatchedDistrictId.ifBlank { profileDistrictId }

    val district = remember(reportDistrictId) {
        JurisdictionConstants.DISTRICTS.find { it.id == reportDistrictId }
    }
    val urbanLocation = remember(reportDistrictId) { JurisdictionConstants.getUrbanLocation(reportDistrictId) }
    val ruralLocation = remember(reportDistrictId) { JurisdictionConstants.getRuralLocation(reportDistrictId) }
    val districtCategory = remember(reportDistrictId) {
        when {
            JurisdictionConstants.isUrban(reportDistrictId) -> "URBAN"
            JurisdictionConstants.isRural(reportDistrictId) -> "RURAL"
            else -> null
        }
    }
    val isUsingProfileDistrictFallback = resolvedLocationAddress != null &&
        resolvedLocationAddress?.matchedDistrict == null &&
        profileDistrictId.isNotBlank()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openLocationSettings() {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun applyResolvedAddress(resolvedAddress: ResolvedLocationAddress) {
        resolvedLocationAddress = resolvedAddress
        currentLat = resolvedAddress.lat
        currentLng = resolvedAddress.lng

        if (addressText.isBlank()) {
            addressText = resolvedAddress.addressText.orEmpty()
        }
        if (locality.isBlank()) {
            locality = resolvedAddress.locality.orEmpty()
        }

        locationStatus = when {
            resolvedAddress.addressText.isNullOrBlank() -> {
                "Coordinates detected. Enter address details manually."
            }
            resolvedAddress.matchedDistrict != null -> {
                "Current location and address detected"
            }
            profileDistrictId.isNotBlank() -> {
                "Current address detected. Unsupported district; using registered district for routing."
            }
            else -> {
                "Current address detected, but district is outside supported areas."
            }
        }
    }

    fun fetchCurrentLocation() {
        if (inPreview) {
            currentLat = 31.6340
            currentLng = 74.8723
            resolvedLocationAddress = ResolvedLocationAddress(
                lat = 31.6340,
                lng = 74.8723,
                addressText = "Amritsar, Punjab",
                locality = "Amritsar",
                district = "Amritsar",
                state = JurisdictionConstants.PUNJAB_STATE_NAME,
                pinCode = null,
                matchedDistrict = JurisdictionConstants.findDistrictByName("Amritsar")
            )
            locationStatus = "Preview location loaded"
            return
        }

        if (fusedLocationClient == null) {
            locationStatus = "Location service unavailable"
            return
        }

        if (!hasLocationPermission()) {
            locationStatus = "Grant location permission to auto-fill latitude and longitude"
            return
        }

        if (!isLocationServicesEnabled()) {
            locationStatus = "Turn on device location (GPS) to detect current coordinates"
            return
        }

        locationStatus = "Fetching current location..."
        val tokenSource = CancellationTokenSource()
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    scope.launch {
                        val resolver = locationAddressResolver
                        if (resolver == null) {
                            locationStatus = "Current coordinates detected"
                        } else {
                            applyResolvedAddress(resolver.resolve(location.latitude, location.longitude))
                        }
                    }
                } else {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                currentLat = lastLocation.latitude
                                currentLng = lastLocation.longitude
                                scope.launch {
                                    val resolver = locationAddressResolver
                                    if (resolver == null) {
                                        locationStatus = "Using last known coordinates"
                                    } else {
                                        applyResolvedAddress(resolver.resolve(lastLocation.latitude, lastLocation.longitude))
                                    }
                                }
                            } else {
                                locationStatus = "Unable to determine location"
                            }
                        }
                        .addOnFailureListener { error ->
                            locationStatus = error.message ?: "Unable to determine location"
                        }
                }
            }
            .addOnFailureListener { error ->
                locationStatus = error.message ?: "Unable to determine location"
            }
    }

    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            fetchCurrentLocation()
        } else {
            val activity = context as? Activity
            val fineRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            val coarseRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            } ?: false
            locationStatus = if (!fineRationale && !coarseRationale) {
                "Location permission denied permanently. Open app settings to allow access."
            } else {
                "Location permission denied. Please allow it to auto-fill latitude and longitude."
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = GetMultipleContents()
    ) { uris ->
        val remainingSlots = MAX_REPORT_IMAGES - selectedImageUris.size
        if (remainingSlots <= 0) {
            Toast.makeText(context, "You can add up to $MAX_REPORT_IMAGES photos", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val pickedUris = uris.take(remainingSlots).map(Uri::toString)
        selectedImageUris = (selectedImageUris + pickedUris).distinct().take(MAX_REPORT_IMAGES)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null

        if (success && capturedUri != null) {
            selectedImageUris = (selectedImageUris + capturedUri.toString()).distinct().take(MAX_REPORT_IMAGES)
        }
    }

    fun requestCameraCapture() {
        if (selectedImageUris.size >= MAX_REPORT_IMAGES) {
            Toast.makeText(context, "You can add up to $MAX_REPORT_IMAGES photos", Toast.LENGTH_SHORT).show()
            return
        }
        val imageUri = createCameraTempFile(appContext)
        pendingCameraUri = imageUri
        takePictureLauncher.launch(imageUri)
    }

    fun validateForm(): String? {
        return when {
            title.trim().isBlank() -> "Please enter a title for the issue"
            addressText.trim().isBlank() -> "Please enter the address or location text"
            description.trim().isBlank() -> "Please add a short description"
            selectedImageUris.isEmpty() -> "Please add at least one photo"
            reportDistrictId.isBlank() || district == null -> "District could not be determined. Please complete your profile or detect a supported current district."
            else -> null
        }
    }

    LaunchedEffect(Unit) {
        if (inPreview) {
            currentLat = 31.6340
            currentLng = 74.8723
            locationStatus = "Preview location"
        } else if (hasLocationPermission()) {
            fetchCurrentLocation()
        } else {
            locationStatus = "Tap the button below to detect your current location"
        }
    }

    val categoryName = CategoryConstants.getCategoryName(selectedCategoryId) ?: "Select category"
    val displayDistrictName = resolvedLocationAddress?.matchedDistrict?.name
        ?: resolvedLocationAddress?.district?.takeIf { it.isNotBlank() }
        ?: district?.name
        ?: "Unknown district"
    val detectedPlaceName = listOfNotNull(
        resolvedLocationAddress?.locality,
        displayDistrictName
    )
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
    val locationDisplayName = detectedPlaceName.ifBlank { displayDistrictName }
    val displayState = resolvedLocationAddress?.state
        ?.takeIf { it.isNotBlank() }
        ?: JurisdictionConstants.PUNJAB_STATE_NAME
    val displayPinCode = resolvedLocationAddress?.pinCode?.takeIf { it.isNotBlank() }
    val displayAreaType = when {
        resolvedLocationAddress != null && resolvedLocationAddress?.matchedDistrict == null -> "UNSUPPORTED"
        resolvedLocationAddress?.matchedDistrict != null -> resolvedLocationAddress?.matchedDistrict?.category
        else -> districtCategory
    } ?: "UNKNOWN"
    val jurisdictionSummary = when {
        urbanLocation != null -> "${urbanLocation.cityName} • ${urbanLocation.wardName}"
        ruralLocation != null -> "${ruralLocation.blockName} • ${ruralLocation.panchayatName}"
        else -> "Using registered district fallback"
    }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF006D47)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, ProfileScreen::class.java)) }) {
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
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = false,
                    onClick = { context.startActivity(Intent(context, Dashboard::class.java)) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("HOME") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
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
                    onClick = { context.startActivity(Intent(context, AlertsScreen::class.java)) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("ALERTS") }
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF8FAF9))
                .padding(horizontal = 20.dp)
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
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1C1A),
                    lineHeight = 36.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your report helps us keep our neighborhood thriving and safe.",
                    fontSize = 15.sp,
                    color = Color(0xFF5F635F),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (formError != null) {
                    MessageCard(
                        backgroundColor = Color(0xFFFFEBEE),
                        borderColor = Color(0xFFFFCDD2),
                        textColor = Color(0xFFC62828),
                        text = formError!!
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (formMessage != null) {
                    MessageCard(
                        backgroundColor = Color(0xFFE8F5E9),
                        borderColor = Color(0xFFC8E6C9),
                        textColor = Color(0xFF2E7D32),
                        text = formMessage!!
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                SectionTitle(text = "ISSUE DETAILS")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        formError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Issue title") },
                    placeholder = { Text("Garbage pile near main market") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = addressText,
                    onValueChange = {
                        addressText = it
                        formError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Address text") },
                    placeholder = { Text("Market Road, Ward 12, Bhopal") },
                    singleLine = false,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = locality,
                        onValueChange = { locality = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Locality") },
                        placeholder = { Text("Old Market") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Landmark") },
                        placeholder = { Text("Near Hanuman Mandir") },
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        formError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Detailed description") },
                    placeholder = { Text("Provide details like severity, exact location, or what happened...") },
                    singleLine = false,
                    minLines = 4
                )
                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(text = "ISSUE CATEGORY")
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            ),
                        trailingIcon = {
                            Icon(
                                if (isCategoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        },
                        label = { Text("Select issue category") }
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        CategoryConstants.CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(text = "PRIORITY")
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = isPriorityExpanded,
                    onExpandedChange = { isPriorityExpanded = !isPriorityExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPriority.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            ),
                        trailingIcon = {
                            Icon(
                                if (isPriorityExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        },
                        label = { Text("Select priority level") }
                    )
                    ExposedDropdownMenu(
                        expanded = isPriorityExpanded,
                        onDismissRequest = { isPriorityExpanded = false }
                    ) {
                        listOf("low", "medium", "high").forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                                onClick = {
                                    selectedPriority = priority
                                    isPriorityExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                SectionTitle(text = "PHOTOS")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UploadActionCard(
                        icon = Icons.Default.PhotoCamera,
                        label = "Take Photo",
                        modifier = Modifier.weight(1f),
                        onClick = { requestCameraCapture() }
                    )
                    UploadActionCard(
                        icon = Icons.Default.FileUpload,
                        label = "Gallery",
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${selectedImageUris.size}/$MAX_REPORT_IMAGES photos selected",
                    fontSize = 12.sp,
                    color = Color(0xFF747974)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedImageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(selectedImageUris, key = { it }) { uriString ->
                            SelectedImagePreview(
                                uriString = uriString,
                                onRemove = {
                                    selectedImageUris = selectedImageUris.filterNot { it == uriString }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                SectionTitle(text = "LOCATION")
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f)
                        ),
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
                                text = "DETECTED DISTRICT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF747974)
                            )
                            Text(
                                text = displayAreaType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006D47)
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
                                text = locationDisplayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = jurisdictionSummary,
                            fontSize = 12.sp,
                            color = Color(0xFF5F635F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LocationDetailRow(label = "State", value = displayState)
                        LocationDetailRow(label = "District", value = displayDistrictName)
                        resolvedLocationAddress?.locality
                            ?.takeIf { it.isNotBlank() }
                            ?.let { LocationDetailRow(label = "Locality", value = it) }
                        displayPinCode?.let { LocationDetailRow(label = "Pin code", value = it) }
                        if (addressText.isNotBlank()) {
                            LocationDetailRow(label = "Full address", value = addressText)
                        }
                        if (isUsingProfileDistrictFallback) {
                            Spacer(modifier = Modifier.height(8.dp))
                            MessageCard(
                                backgroundColor = Color(0xFFFFF8E1),
                                borderColor = Color(0xFFFFECB3),
                                textColor = Color(0xFF8A5A00),
                                text = "Current district is outside supported areas; using your registered district for routing."
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0D2421))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridColor = Color.White.copy(alpha = 0.05f)
                                val step = 48.dp.toPx()
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
                                    x += step
                                }
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y))
                                    y += step
                                }
                            }
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).size(52.dp).padding(bottom = 10.dp),
                                tint = Color(0xFF4DB6AC)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LocationValueCard(
                                label = "Latitude",
                                value = currentLat?.let { formatCoordinate(it) } ?: "Not detected"
                            )
                            LocationValueCard(
                                label = "Longitude",
                                value = currentLng?.let { formatCoordinate(it) } ?: "Not detected"
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = locationStatus,
                            fontSize = 12.sp,
                            color = Color(0xFF5F635F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (hasLocationPermission()) {
                                        fetchCurrentLocation()
                                    } else {
                                        requestLocationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Detect Location")
                            }

                            TextButton(
                                onClick = { openLocationSettings() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Enable GPS")
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        val validationError = validateForm()
                        if (validationError != null) {
                            formError = validationError
                            formMessage = null
                            Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (issueRepository == null || cloudinaryUploader == null) {
                            formError = "Issue submission is unavailable in preview mode"
                            return@Button
                        }

                        scope.launch {
                            isSubmitting = true
                            formError = null
                            formMessage = null

                            try {
                                val uploadedUrls = cloudinaryUploader
                                    .uploadImages(selectedImageUris.map(Uri::parse))
                                    .getOrThrow()

                                val request = CreateIssueRequest(
                                    clientId = clientId,
                                    categoryId = selectedCategoryId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    addressText = addressText.trim(),
                                    locality = locality.trim().takeIf { it.isNotBlank() },
                                    landmark = landmark.trim().takeIf { it.isNotBlank() },
                                    lat = currentLat,
                                    lng = currentLng,
                                    imageUrls = uploadedUrls,
                                    priority = selectedPriority,
                                    districtId = reportDistrictId,
                                    cityId = urbanLocation?.cityId,
                                    wardId = urbanLocation?.wardId,
                                    blockId = ruralLocation?.blockId,
                                    panchayatId = ruralLocation?.panchayatId
                                )

                                val response = issueRepository.createIssue(request).getOrThrow()
                                formMessage = response.message.ifBlank {
                                    if (response.idempotent) {
                                        "Issue already exists for this clientId"
                                    } else {
                                        "Issue reported successfully"
                                    }
                                }
                                Toast.makeText(context, formMessage, Toast.LENGTH_LONG).show()

                                if (context is Activity) {
                                    context.finish()
                                }
                            } catch (throwable: Throwable) {
                                formError = throwable.message ?: "Unable to submit issue"
                                Toast.makeText(context, formError, Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0xFF00875A).copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(if (isSubmitting) "Submitting..." else "Submit Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF747974)
    )
}

@Composable
private fun MessageCard(
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = textColor,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun LocationDetailRow(
    label: String,
    value: String
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(82.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF747974)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = Color(0xFF1A1C1A),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UploadActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFFBCC2BC),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                ),
                cornerRadius = CornerRadius(24.dp.toPx())
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

@Composable
private fun SelectedImagePreview(
    uriString: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
    ) {
        AsyncImage(
            model = uriString.toUri(),
            contentDescription = "Selected photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LocationValueCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF747974))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1C1A))
        }
    }
}

private fun formatCoordinate(value: Double): String {
    return String.format(java.util.Locale.US, "%.6f", value)
}

private fun createCameraTempFile(context: Context): Uri {
    val imageDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("issue_", ".jpg", imageDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun IssueReportScreenPreview() {
    SevaSetuTheme {
        IssueReportScreen()
    }
}
