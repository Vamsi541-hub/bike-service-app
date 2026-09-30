package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bike
import com.example.data.model.MaintenanceReminder
import com.example.data.model.ServiceRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    bike: Bike?,
    serviceRecords: List<ServiceRecord>,
    reminders: List<MaintenanceReminder>,
    onAddServiceRecord: (ServiceRecord) -> Unit,
    onDeleteServiceRecord: (String) -> Unit,
    onAddReminder: (MaintenanceReminder) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onMarkReminderServiced: (MaintenanceReminder, Int, String, Int, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Past Repairs & Logs (${serviceRecords.size})", "Maintenance Intervals (${reminders.size})")

    var showAddRecordSheet by remember { mutableStateOf(false) }
    var showAddReminderSheet by remember { mutableStateOf(false) }
    var markingReminder by remember { mutableStateOf<MaintenanceReminder?>(null) }

    val totalSpent = remember(serviceRecords) { serviceRecords.sumOf { it.cost } }
    val bikeName = remember(bike) {
        bike?.let { "${it.brand} ${it.model} (${it.year})" } ?: "Yamaha MT-15 V2 (2024)"
    }
    val currentOdo = 11450 // Current reference odometer reading for the active bike

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Service & Maintenance Tracker",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$bikeName • Current: $currentOdo KM",
                            style = MaterialTheme.typography.labelSmall,
                            color = BikePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        showAddRecordSheet = true
                    } else {
                        showAddReminderSheet = true
                    }
                },
                containerColor = BikePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.Add else Icons.Default.NotificationAdd,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = if (selectedTab == 0) "Log Repair" else "Set Reminder",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                label = "tabContent"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> PastRepairsTabContent(
                        serviceRecords = serviceRecords,
                        totalSpent = totalSpent,
                        onDeleteRecord = onDeleteServiceRecord,
                        onLogFirstRepair = { showAddRecordSheet = true }
                    )
                    else -> MaintenanceIntervalsTabContent(
                        reminders = reminders,
                        currentOdoKm = currentOdo,
                        onMarkServiced = { reminder -> markingReminder = reminder },
                        onDeleteReminder = onDeleteReminder,
                        onAddFirstReminder = { showAddReminderSheet = true }
                    )
                }
            }
        }
    }

    // Modal Sheet: Log New Past Repair / Service
    if (showAddRecordSheet) {
        LogServiceModalSheet(
            bikeId = bike?.id ?: "bike_1",
            bikeDetails = bikeName,
            currentOdo = currentOdo,
            onDismiss = { showAddRecordSheet = false },
            onSave = { record ->
                onAddServiceRecord(record)
                showAddRecordSheet = false
            }
        )
    }

    // Modal Sheet: Set Future Maintenance Interval Reminder
    if (showAddReminderSheet) {
        SetReminderModalSheet(
            bikeId = bike?.id ?: "bike_1",
            currentOdo = currentOdo,
            onDismiss = { showAddReminderSheet = false },
            onSave = { reminder ->
                onAddReminder(reminder)
                showAddReminderSheet = false
            }
        )
    }

    // Mark Serviced Confirmation Dialog
    markingReminder?.let { reminder ->
        MarkServicedDialog(
            reminder = reminder,
            defaultOdo = currentOdo,
            onDismiss = { markingReminder = null },
            onConfirm = { odo, date, cost, mechanic ->
                onMarkReminderServiced(reminder, odo, date, cost, mechanic)
                markingReminder = null
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 1: Past Repairs & Service Logs
// -------------------------------------------------------------
@Composable
fun PastRepairsTabContent(
    serviceRecords: List<ServiceRecord>,
    totalSpent: Int,
    onDeleteRecord: (String) -> Unit,
    onLogFirstRepair: () -> Unit
) {
    if (serviceRecords.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(BikePrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = BikePrimary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("No repair logs recorded yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Keep track of oil changes, tyre swaps, brake jobs & DIY maintenance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onLogFirstRepair,
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Past Service")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Maintenance Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹$totalSpent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = BikePrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Service Logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${serviceRecords.size} Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(serviceRecords, key = { it.id }) { record ->
                ServiceRecordCard(
                    record = record,
                    onDelete = { onDeleteRecord(record.id) }
                )
            }
        }
    }
}

@Composable
fun ServiceRecordCard(
    record: ServiceRecord,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = getCategoryColor(record.serviceType).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.serviceType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = getCategoryColor(record.serviceType),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Odometer
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = BikeSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${record.odometerKm} KM",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.workshopOrMechanic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.partsReplaced.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Parts Replaced: ${record.partsReplaced}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${record.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Invoice Total & Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.invoiceNumber.isNotBlank()) {
                    Text(
                        text = "#${record.invoiceNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = if (record.cost > 0) "₹${record.cost}" else "Free / Self DIY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (record.cost > 0) BikePrimary else BikeAccentGreen
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: Maintenance Intervals & Reminders
// -------------------------------------------------------------
@Composable
fun MaintenanceIntervalsTabContent(
    reminders: List<MaintenanceReminder>,
    currentOdoKm: Int,
    onMarkServiced: (MaintenanceReminder) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddFirstReminder: () -> Unit
) {
    if (reminders.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(BikeSecondary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BikeSecondary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("No maintenance intervals set", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Never miss an oil change, chain lube, brake fluid flush, or insurance renewal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddFirstReminder,
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.NotificationAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set First Reminder")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Odometer Tracker Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(BikePrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = BikePrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Odometer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currentOdoKm KM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = BikeAccentGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ACTIVE TRACKING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BikeAccentGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            items(reminders, key = { it.id }) { reminder ->
                MaintenanceReminderCard(
                    reminder = reminder,
                    currentOdoKm = currentOdoKm,
                    onMarkServiced = { onMarkServiced(reminder) },
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }
        }
    }
}

@Composable
fun MaintenanceReminderCard(
    reminder: MaintenanceReminder,
    currentOdoKm: Int,
    onMarkServiced: () -> Unit,
    onDelete: () -> Unit
) {
    val kmRemaining = reminder.dueKm - currentOdoKm
    val isOverdue = kmRemaining <= 0

    // Progress towards interval
    val totalInterval = reminder.intervalKm.coerceAtLeast(1)
    val kmSinceService = (currentOdoKm - reminder.lastServicedKm).coerceAtLeast(0)
    val progress = (kmSinceService.toFloat() / totalInterval.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Due Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = getCategoryColor(reminder.componentCategory).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = reminder.componentCategory,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = getCategoryColor(reminder.componentCategory),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isOverdue) BikeEmergencyRed.copy(alpha = 0.15f) else BikeAccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isOverdue) "OVERDUE BY ${-kmRemaining} KM" else "DUE IN $kmRemaining KM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) BikeEmergencyRed else BikeAccentGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last: ${reminder.lastServicedKm} KM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Target: ${reminder.dueKm} KM",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = BikePrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverdue) BikeEmergencyRed else BikePrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (reminder.dueDate.isNotBlank()) {
                Text(
                    text = "Est. Target Date: ${reminder.dueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reminder.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mark Serviced Button
            Button(
                onClick = onMarkServiced,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark Serviced & Advance Interval", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// MODAL SHEET: Log New Service Record
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogServiceModalSheet(
    bikeId: String,
    bikeDetails: String,
    currentOdo: Int,
    onDismiss: () -> Unit,
    onSave: (ServiceRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Engine & Oil") }
    var odoText by remember { mutableStateOf(currentOdo.toString()) }
    var costText by remember { mutableStateOf("1500") }
    var workshop by remember { mutableStateOf("Apex MotoCare Studio") }
    var parts by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Engine & Oil", "Brakes", "Tyres & Wheels", "Chain & Sprocket", "General Service", "Electrical & Battery", "DIY Repair")
    val currentDateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log Past Service / Repair",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Record maintenance details, parts swapped, and mileage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (title.isBlank()) {
                                title = when (cat) {
                                    "Engine & Oil" -> "Synthetic Oil Change & Filter Replacement"
                                    "Brakes" -> "Front & Rear Brake Pad Replacement"
                                    "Chain & Sprocket" -> "Chain Cleaning, Lube & Slack Adjustment"
                                    "Tyres & Wheels" -> "Tyre Replacement & Wheel Balancing"
                                    "Electrical & Battery" -> "Battery Replacement & Terminal Cleaning"
                                    else -> "$cat Service"
                                }
                            }
                        },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Service Description / Title") },
                placeholder = { Text("e.g. Motul 7100 Oil Change + Filter") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it },
                    label = { Text("Odometer (KM)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost (₹)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = workshop,
                onValueChange = { workshop = it },
                label = { Text("Workshop or Mechanic Name") },
                placeholder = { Text("e.g. Apex MotoCare or Self DIY") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = parts,
                onValueChange = { parts = it },
                label = { Text("Parts Replaced (Optional)") },
                placeholder = { Text("e.g. Spark Plug, Air Filter, Brake Pads") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Observations") },
                placeholder = { Text("e.g. Checked tyre pressure, adjusted clutch lever") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val record = ServiceRecord(
                            bikeId = bikeId,
                            bikeDetails = bikeDetails,
                            title = title,
                            serviceType = selectedCategory,
                            date = currentDateStr,
                            odometerKm = odoText.toIntOrNull() ?: currentOdo,
                            workshopOrMechanic = workshop.ifBlank { "Verified Workshop" },
                            cost = costText.toIntOrNull() ?: 0,
                            partsReplaced = parts,
                            notes = notes,
                            invoiceNumber = "LOG-${(1000..9999).random()}"
                        )
                        onSave(record)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Service Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// MODAL SHEET: Set Maintenance Interval Reminder
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderModalSheet(
    bikeId: String,
    currentOdo: Int,
    onDismiss: () -> Unit,
    onSave: (MaintenanceReminder) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Oil & Lubrication") }
    var intervalKmText by remember { mutableStateOf("5000") }
    var intervalDaysText by remember { mutableStateOf("180") }
    var lastServicedKmText by remember { mutableStateOf(currentOdo.toString()) }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Oil & Lubrication", "Drivetrain", "Braking System", "Engine & Filters", "Periodic Inspection", "Statutory & Insurance")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set Maintenance Interval",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure reminder intervals based on mileage and time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (title.isBlank()) {
                                title = when (cat) {
                                    "Oil & Lubrication" -> "Engine Oil & Filter Replacement"
                                    "Drivetrain" -> "Drive Chain Clean & Lube"
                                    "Braking System" -> "Brake Fluid Flush (DOT 4)"
                                    "Engine & Filters" -> "Air Filter Cleaning / Replacement"
                                    "Periodic Inspection" -> "Spark Plug & Valve Clearance Check"
                                    else -> "PUC / Insurance Renewal"
                                }
                            }
                        },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title") },
                placeholder = { Text("e.g. Engine Oil & Filter Change") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = intervalKmText,
                    onValueChange = { intervalKmText = it },
                    label = { Text("Interval (KM)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = intervalDaysText,
                    onValueChange = { intervalDaysText = it },
                    label = { Text("Interval (Days)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = lastServicedKmText,
                onValueChange = { lastServicedKmText = it },
                label = { Text("Last Serviced at Odometer (KM)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Recommended Specs / Notes") },
                placeholder = { Text("e.g. Use 10W40 Full Synthetic JASO MA2") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val intervalKm = intervalKmText.toIntOrNull() ?: 5000
                        val lastKm = lastServicedKmText.toIntOrNull() ?: currentOdo
                        val targetDueKm = lastKm + intervalKm

                        val reminder = MaintenanceReminder(
                            bikeId = bikeId,
                            title = title,
                            componentCategory = selectedCategory,
                            intervalKm = intervalKm,
                            intervalDays = intervalDaysText.toIntOrNull() ?: 180,
                            lastServicedKm = lastKm,
                            dueKm = targetDueKm,
                            dueDate = "Due in $intervalKm KM",
                            notes = notes
                        )
                        onSave(reminder)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary)
            ) {
                Icon(Icons.Default.NotificationAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Maintenance Interval", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// MARK SERVICED DIALOG
// -------------------------------------------------------------
@Composable
fun MarkServicedDialog(
    reminder: MaintenanceReminder,
    defaultOdo: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Int, String) -> Unit
) {
    var odoText by remember { mutableStateOf(defaultOdo.toString()) }
    var costText by remember { mutableStateOf("1200") }
    var workshopText by remember { mutableStateOf("Apex MotoCare Studio") }
    val dateStr = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirm Service Completion", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Marking '${reminder.title}' as completed will automatically advance the next service interval by ${reminder.intervalKm} KM and log this maintenance to your repair history.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it },
                    label = { Text("Completed at Odometer (KM)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = workshopText,
                    onValueChange = { workshopText = it },
                    label = { Text("Workshop / Done By") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odoText.toIntOrNull() ?: defaultOdo
                    val cost = costText.toIntOrNull() ?: 0
                    onConfirm(odo, dateStr, cost, workshopText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary)
            ) {
                Text("Confirm & Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getCategoryColor(category: String): Color {
    return when {
        category.contains("Oil", ignoreCase = true) -> BikePrimary
        category.contains("Brake", ignoreCase = true) -> BikeEmergencyRed
        category.contains("Chain", ignoreCase = true) || category.contains("Drivetrain", ignoreCase = true) -> BikeSecondary
        category.contains("Tyre", ignoreCase = true) -> FuelYellow
        category.contains("Battery", ignoreCase = true) || category.contains("Electrical", ignoreCase = true) -> EvCyan
        else -> BikeAccentGreen
    }
}
