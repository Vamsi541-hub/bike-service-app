package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.BikeEmergencyRed
import com.example.ui.theme.BikePrimary
import com.example.ui.theme.BikeSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (User) -> Unit,
    onSkipToGuest: () -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }

    var emailOrPhone by remember { mutableStateOf("rahul.sharma@example.com") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Customer Extra Fields
    var fullName by remember { mutableStateOf("Rahul Sharma") }
    var customerLocation by remember { mutableStateOf("Koramangala, Bengaluru") }

    // Service Provider Extra Fields
    var businessName by remember { mutableStateOf("Apex MotoCare & Tuning Studio") }
    var providerLocation by remember { mutableStateOf("Koramangala 4th Block, Bengaluru") }
    var servicesProvided by remember { mutableStateOf("General Service, Engine Tuning, Oil Change, EV diagnostics") }
    var workingHours by remember { mutableStateOf("8:30 AM - 8:30 PM") }

    var showForgotDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo & Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(BikePrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = BikePrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "BikeCare",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Your Bike. Your Care.",
                style = MaterialTheme.typography.bodyMedium,
                color = BikeSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login / Register Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isRegistering,
                    onClick = { isRegistering = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Sign In")
                }
                SegmentedButton(
                    selected = isRegistering,
                    onClick = { isRegistering = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Register")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // If Registering, choose Customer vs Service Provider
            if (isRegistering) {
                Text(
                    text = "Register as:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == UserRole.CUSTOMER,
                        onClick = { selectedRole = UserRole.CUSTOMER },
                        label = { Text("Customer") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.SERVICE_PROVIDER,
                        onClick = { selectedRole = UserRole.SERVICE_PROVIDER },
                        label = { Text("Service Provider") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input Fields
            if (isRegistering) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Your Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedRole == UserRole.SERVICE_PROVIDER) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Workshop Name") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = providerLocation,
                        onValueChange = { providerLocation = it },
                        label = { Text("Service Location / Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = servicesProvided,
                        onValueChange = { servicesProvided = it },
                        label = { Text("Services Provided (e.g. Engine, Oil)") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        label = { Text("Working Hours (e.g. 9 AM - 8 PM)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    OutlinedTextField(
                        value = customerLocation,
                        onValueChange = { customerLocation = it },
                        label = { Text("Your City / Location") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("Mobile Number or Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (!isRegistering) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotDialog = true }) {
                        Text("Forgot password?", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit Button
            Button(
                onClick = {
                    val user = User(
                        id = "current_user",
                        name = if (isRegistering) fullName else "Rahul Sharma",
                        email = if (emailOrPhone.contains("@")) emailOrPhone else "user@bikecare.io",
                        phone = if (!emailOrPhone.contains("@")) emailOrPhone else "+91 98765 43210",
                        role = if (isRegistering) selectedRole else UserRole.CUSTOMER,
                        location = if (selectedRole == UserRole.SERVICE_PROVIDER) providerLocation else customerLocation,
                        businessName = businessName,
                        servicesOffered = servicesProvided,
                        workingHours = workingHours
                    )
                    onAuthSuccess(user)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isRegistering) "Create Account" else "Login to BikeCare",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Demo Skip
            OutlinedButton(
                onClick = onSkipToGuest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continue as Demo Rider")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Password Reset") },
            text = { Text("A password recovery SMS and secure link will be dispatched to your registered contact.") },
            confirmButton = {
                Button(onClick = { showForgotDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
