package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

// Rigorous email & mobile format validators
fun isValidEmail(input: String): Boolean {
    val trimmed = input.trim()
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(trimmed)
}

fun isValidMobile(input: String): Boolean {
    val clean = input.trim().replace(" ", "").replace("-", "")
    val phoneRegex = "^(\\+?[0-9]{1,3})?[6-9][0-9]{9}$".toRegex()
    return phoneRegex.matches(clean)
}

fun isValidIdentifier(input: String): Boolean {
    return isValidEmail(input) || isValidMobile(input)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onCheckUser: suspend (String) -> User?,
    onAuthenticate: suspend (String, String) -> User?,
    onAuthSuccess: (User) -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }

    // Sign In Fields
    var signInIdentifier by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Registration Fields
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regLocation by remember { mutableStateOf("") }

    // Workshop Fields
    var businessName by remember { mutableStateOf("") }
    var providerLocation by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("9:00 AM - 8:00 PM") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Smooth subtle neon pulse for logo
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Brand Header
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(1f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BikePrimary.copy(alpha = pulseGlow),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, BikePrimary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "BikeCare Logo",
                                tint = BikePrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BIKECARE",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Your Bike. Your Care.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BikeSecondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BikeEmergencyRed.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BikeEmergencyRed.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = BikeEmergencyRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(error, color = BikeEmergencyRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Success Banner
                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    successMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BikeAccentGreen.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BikeAccentGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BikeAccentGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = BikeAccentGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Modern Tab Selector (Sign In vs Create Account)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        TabButton(
                            title = "Sign In",
                            isSelected = !isRegistering,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isRegistering = false
                                errorMessage = null
                                successMessage = null
                            }
                        )

                        TabButton(
                            title = "Create Account",
                            isSelected = isRegistering,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isRegistering = true
                                errorMessage = null
                                successMessage = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Form Content
                AnimatedContent(
                    targetState = isRegistering,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "formSwitch"
                ) { registering ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (registering) {
                            // CREATE ACCOUNT FORM
                            Text(
                                text = "Account Type:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RoleSelectCard(
                                    title = "Bike Rider",
                                    description = "Maintenance & repair tracking",
                                    icon = Icons.Default.TwoWheeler,
                                    isSelected = selectedRole == UserRole.CUSTOMER,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedRole = UserRole.CUSTOMER }
                                )

                                RoleSelectCard(
                                    title = "Workshop Partner",
                                    description = "Receive service bookings",
                                    icon = Icons.Default.Build,
                                    isSelected = selectedRole == UserRole.SERVICE_PROVIDER,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedRole = UserRole.SERVICE_PROVIDER }
                                )
                            }

                            // Full Name
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = {
                                    regFullName = it
                                    errorMessage = null
                                },
                                label = { Text("Full Name *") },
                                placeholder = { Text("e.g. Rajesh Kumar") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BikePrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            // Email Address
                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = {
                                    regEmail = it
                                    errorMessage = null
                                },
                                label = { Text("Gmail or Email Address *") },
                                placeholder = { Text("e.g. rajesh.kumar@gmail.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BikePrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // Mobile Number
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = {
                                    regPhone = it
                                    errorMessage = null
                                },
                                label = { Text("10-Digit Mobile Number *") },
                                placeholder = { Text("e.g. 9876543210") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BikePrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // Password
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = {
                                    regPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Password (Min 6 Characters) *") },
                                placeholder = { Text("Create secure password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BikePrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (regPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (selectedRole == UserRole.SERVICE_PROVIDER) ImeAction.Next else ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )

                            if (selectedRole == UserRole.SERVICE_PROVIDER) {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text("Workshop / Garage Name *") },
                                    placeholder = { Text("e.g. Apex MotoCare") },
                                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = BikeSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                OutlinedTextField(
                                    value = providerLocation,
                                    onValueChange = { providerLocation = it },
                                    label = { Text("Workshop Address / City") },
                                    placeholder = { Text("e.g. Koramangala, Bengaluru") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BikeSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                OutlinedTextField(
                                    value = workingHours,
                                    onValueChange = { workingHours = it },
                                    label = { Text("Working Hours") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = BikeSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                )
                            } else {
                                OutlinedTextField(
                                    value = regLocation,
                                    onValueChange = { regLocation = it },
                                    label = { Text("City / Area (Optional)") },
                                    placeholder = { Text("e.g. Mumbai, Maharashtra") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BikePrimary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                )
                            }
                        } else {
                            // SIGN IN FORM
                            OutlinedTextField(
                                value = signInIdentifier,
                                onValueChange = {
                                    signInIdentifier = it
                                    errorMessage = null
                                },
                                label = { Text("Gmail / Email or 10-Digit Mobile") },
                                placeholder = { Text("e.g. name@gmail.com or 9876543210") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BikePrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedTextField(
                                value = signInPassword,
                                onValueChange = {
                                    signInPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BikePrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signInPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (signInPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showForgotDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Forgot Password?", style = MaterialTheme.typography.bodySmall, color = BikePrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                successMessage = null

                                if (registering) {
                                    // 1. Full name validation
                                    val trimmedName = regFullName.trim()
                                    if (trimmedName.length < 2 || !trimmedName.any { it.isLetter() }) {
                                        errorMessage = "Please enter a valid Full Name (letters only, min 2 chars)."
                                        return@Button
                                    }

                                    // 2. Email validation
                                    val trimmedEmail = regEmail.trim()
                                    if (!isValidEmail(trimmedEmail)) {
                                        errorMessage = "Please enter a valid Gmail / Email address (e.g. yourname@gmail.com)."
                                        return@Button
                                    }

                                    // 3. Mobile validation
                                    val trimmedPhone = regPhone.trim().replace(" ", "").replace("-", "")
                                    if (!isValidMobile(trimmedPhone)) {
                                        errorMessage = "Please enter a valid 10-digit mobile number."
                                        return@Button
                                    }

                                    // 4. Password validation
                                    if (regPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }

                                    if (selectedRole == UserRole.SERVICE_PROVIDER && businessName.isBlank()) {
                                        errorMessage = "Please enter your Workshop / Garage Name."
                                        return@Button
                                    }

                                    isLoading = true
                                    coroutineScope.launch {
                                        // Check if user already exists
                                        val existingEmail = onCheckUser(trimmedEmail)
                                        if (existingEmail != null) {
                                            errorMessage = "An account with email '$trimmedEmail' is already registered. Please Sign In."
                                            isLoading = false
                                            return@launch
                                        }

                                        val existingPhone = onCheckUser(trimmedPhone)
                                        if (existingPhone != null) {
                                            errorMessage = "An account with mobile '$trimmedPhone' is already registered. Please Sign In."
                                            isLoading = false
                                            return@launch
                                        }

                                        val newId = "usr_${UUID.randomUUID().toString().take(8)}"
                                        val newUser = User(
                                            id = newId,
                                            name = trimmedName,
                                            email = trimmedEmail,
                                            phone = trimmedPhone,
                                            password = regPassword,
                                            role = selectedRole,
                                            location = if (selectedRole == UserRole.SERVICE_PROVIDER) {
                                                providerLocation.ifBlank { "Bengaluru, India" }
                                            } else {
                                                regLocation.ifBlank { "Bengaluru, India" }
                                            },
                                            businessName = businessName,
                                            servicesOffered = "General Service, Oil Change, Repairs",
                                            workingHours = workingHours
                                        )
                                        isLoading = false
                                        onAuthSuccess(newUser)
                                    }
                                } else {
                                    // SIGN IN VALIDATION
                                    val id = signInIdentifier.trim()
                                    if (id.isBlank()) {
                                        errorMessage = "Please enter your registered Gmail ID or Mobile number."
                                        return@Button
                                    }

                                    if (!isValidIdentifier(id)) {
                                        errorMessage = "Invalid login format. Enter a valid Email (e.g. name@gmail.com) or 10-digit Mobile number."
                                        return@Button
                                    }

                                    if (signInPassword.isBlank()) {
                                        errorMessage = "Please enter your password."
                                        return@Button
                                    }

                                    if (signInPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }

                                    isLoading = true
                                    coroutineScope.launch {
                                        // Check if account exists
                                        val existing = onCheckUser(id)
                                        if (existing == null) {
                                            errorMessage = "No account found registered with '$id'. Please tap 'Create Account' to register."
                                            isLoading = false
                                            return@launch
                                        }

                                        // Verify password
                                        val authUser = onAuthenticate(id, signInPassword)
                                        if (authUser == null) {
                                            errorMessage = "Incorrect password for '$id'. Please check and try again."
                                            isLoading = false
                                            return@launch
                                        }

                                        isLoading = false
                                        onAuthSuccess(authUser)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (registering) "Create Account" else "Sign In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Instructional hint card
                        if (!registering) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = BikeSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "New user? Tap 'Create Account' to register with your Gmail ID or Mobile number.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Text("Password Recovery", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("A secure password reset verification code has been dispatched to your registered email or phone.")
            },
            confirmButton = {
                Button(onClick = { showForgotDialog = false }) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) BikePrimary else Color.Transparent,
        animationSpec = tween(180),
        label = "tabBg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "tabText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTextColor
        )
    }
}

@Composable
fun RoleSelectCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BikePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) BikePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
