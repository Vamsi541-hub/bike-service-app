package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    initialService: ServiceItem?,
    initialProvider: ServiceProvider?,
    allServices: List<ServiceItem>,
    allBikes: List<Bike>,
    providers: List<ServiceProvider>,
    selectedBike: Bike?,
    onBookingConfirmed: (ServiceProvider, Bike, List<ServiceItem>, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    // Current wizard step (1 to 6)
    var currentStep by remember { mutableIntStateOf(1) }

    // Selected state across steps
    var selectedServicesList by remember {
        mutableStateOf(if (initialService != null) listOf(initialService) else emptyList())
    }
    var currentSelectedBike by remember {
        mutableStateOf(selectedBike ?: allBikes.firstOrNull())
    }
    var currentSelectedProvider by remember {
        mutableStateOf(initialProvider ?: providers.firstOrNull())
    }

    val availableDates = remember {
        listOf(
            "Tomorrow, 29 Sep",
            "Wednesday, 30 Sep",
            "Thursday, 01 Oct",
            "Friday, 02 Oct",
            "Saturday, 03 Oct"
        )
    }
    var selectedDate by remember { mutableStateOf(availableDates.first()) }

    val availableTimes = remember {
        listOf("09:00 AM", "10:30 AM", "12:00 PM", "02:30 PM", "04:00 PM", "05:30 PM")
    }
    var selectedTime by remember { mutableStateOf(availableTimes[1]) }

    var bookingNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Book Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step $currentStep of 6: ${getStepTitle(currentStep)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BikePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < 6) {
                                currentStep++
                            } else {
                                currentSelectedProvider?.let { prov ->
                                    currentSelectedBike?.let { bk ->
                                        onBookingConfirmed(
                                            prov,
                                            bk,
                                            selectedServicesList.ifEmpty { listOf(allServices.first()) },
                                            selectedDate,
                                            selectedTime,
                                            bookingNotes
                                        )
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 6) BikeAccentGreen else BikePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = when (currentStep) {
                            1 -> selectedServicesList.isNotEmpty()
                            2 -> currentSelectedBike != null
                            3 -> currentSelectedProvider != null
                            else -> true
                        }
                    ) {
                        Text(
                            text = if (currentStep == 6) "Confirm Booking" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == 6) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Step Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = BikePrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (currentStep) {
                // Step 1: Select Service
                1 -> {
                    Text(
                        text = "Select one or more services:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allServices) { srv ->
                            val isSelected = selectedServicesList.any { it.id == srv.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedServicesList = if (isSelected) {
                                            selectedServicesList.filterNot { it.id == srv.id }
                                        } else {
                                            selectedServicesList + srv
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BikePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = srv.name, fontWeight = FontWeight.Bold)
                                        Text(text = srv.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "₹${srv.estimatedPrice} • ~${srv.durationMinutes}m", color = BikePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedServicesList = if (isSelected) {
                                                selectedServicesList.filterNot { it.id == srv.id }
                                            } else {
                                                selectedServicesList + srv
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: Select Bike
                2 -> {
                    Text(
                        text = "Select your bike for service:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allBikes) { bike ->
                            val isSelected = currentSelectedBike?.id == bike.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentSelectedBike = bike },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BikePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { currentSelectedBike = bike }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${bike.brand} ${bike.model}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${bike.fuelType.name} • Year ${bike.year} • ${bike.registrationNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Select Service Provider
                3 -> {
                    Text(
                        text = "Select nearby certified workshop:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(providers) { prov ->
                            val isSelected = currentSelectedProvider?.id == prov.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentSelectedProvider = prov },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BikePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { currentSelectedProvider = prov }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prov.businessName, fontWeight = FontWeight.Bold)
                                        Text(text = "${prov.distanceKm} km away • Rating ${prov.rating}★", style = MaterialTheme.typography.bodySmall, color = BikeSecondary)
                                        Text(text = prov.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 4: Select Date
                4 -> {
                    Text(
                        text = "Select preferred appointment date:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        availableDates.forEach { dateStr ->
                            val isSelected = selectedDate == dateStr
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedDate = dateStr },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BikePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isSelected) BikePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 5: Select Time Slot
                5 -> {
                    Text(
                        text = "Select convenient time slot:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        availableTimes.forEach { slot ->
                            val isSelected = selectedTime == slot
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTime = slot },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) BikePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isSelected) BikePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = slot,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 6: Booking Confirmation Review
                6 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Review Booking Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ReviewItemRow("Vehicle", "${currentSelectedBike?.brand} ${currentSelectedBike?.model} (${currentSelectedBike?.registrationNumber})")
                                ReviewItemRow("Service Provider", currentSelectedProvider?.businessName ?: "Apex MotoCare")
                                ReviewItemRow("Services", selectedServicesList.joinToString(", ") { it.name })
                                ReviewItemRow("Date & Time", "$selectedDate at $selectedTime")
                                HorizontalDivider()
                                val totalCost = selectedServicesList.sumOf { it.estimatedPrice }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimated Total", fontWeight = FontWeight.Bold)
                                    Text("₹$totalCost", fontWeight = FontWeight.Black, color = BikePrimary, fontSize = 18.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = bookingNotes,
                            onValueChange = { bookingNotes = it },
                            label = { Text("Special instructions / symptoms (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun getStepTitle(step: Int): String {
    return when (step) {
        1 -> "Select Service"
        2 -> "Select Bike"
        3 -> "Select Workshop"
        4 -> "Select Date"
        5 -> "Select Time"
        6 -> "Review & Confirm"
        else -> ""
    }
}

@Composable
fun BookingConfirmationScreen(
    booking: Booking,
    onViewBookings: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BikeAccentGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BikeAccentGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Service Booked Successfully!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your booking confirmation ID has been generated",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Booking ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("#${booking.id}", fontWeight = FontWeight.Bold, color = BikePrimary)
                }

                HorizontalDivider()

                ReviewItemRow("Service Provider", booking.providerName)
                ReviewItemRow("Bike", booking.bikeDetails)
                ReviewItemRow("Services", booking.serviceNames)
                ReviewItemRow("Scheduled Date & Time", "${booking.bookingDate} • ${booking.bookingTime}")

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estimated Cost", fontWeight = FontWeight.Bold)
                    Text("₹${booking.totalCost}", fontWeight = FontWeight.Black, color = BikePrimary, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onViewBookings,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Track in My Bookings", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back to Home")
        }
    }
}
