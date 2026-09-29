package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ServiceProvider
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    provider: ServiceProvider?,
    bookings: List<Booking>,
    onUpdateStatus: (String, BookingStatus) -> Unit,
    onToggleAvailability: (Boolean) -> Unit,
    onCallCustomer: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val pendingBookings = bookings.filter { it.status == BookingStatus.PENDING }
    val activeBookings = bookings.filter { it.status == BookingStatus.ACCEPTED || it.status == BookingStatus.IN_PROGRESS }
    val completedBookings = bookings.filter { it.status == BookingStatus.COMPLETED }
    val totalEarnings = completedBookings.sumOf { it.totalCost } + activeBookings.sumOf { it.totalCost / 2 }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending (${pendingBookings.size})", "Active (${activeBookings.size})", "Completed (${completedBookings.size})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = provider?.businessName ?: "Apex MotoCare Workshop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provider Operations Console",
                            style = MaterialTheme.typography.labelSmall,
                            color = BikeSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (provider?.isOpen == true) "Open" else "Closed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (provider?.isOpen == true) BikeAccentGreen else BikeEmergencyRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = provider?.isOpen ?: true,
                            onCheckedChange = { onToggleAvailability(provider?.isOpen ?: true) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Grid Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Earnings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹$totalEarnings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = BikeAccentGreen)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Completed Jobs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${completedBookings.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = BikePrimary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pending Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${pendingBookings.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = BikeSecondary)
                        }
                    }
                }
            }

            // Tab Row (Pending, Active, Completed)
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            val currentList = when (selectedTab) {
                0 -> pendingBookings
                1 -> activeBookings
                else -> completedBookings
            }

            if (currentList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏍️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No bookings in this tab", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(currentList) { booking ->
                ProviderBookingCard(
                    booking = booking,
                    onUpdateStatus = { newStatus -> onUpdateStatus(booking.id, newStatus) },
                    onCallCustomer = { onCallCustomer(booking.customerName, booking.customerPhone) }
                )
            }
        }
    }
}

@Composable
fun ProviderBookingCard(
    booking: Booking,
    onUpdateStatus: (BookingStatus) -> Unit,
    onCallCustomer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "#${booking.id}", fontWeight = FontWeight.Bold, color = BikePrimary, style = MaterialTheme.typography.labelSmall)
                    Text(text = booking.bikeDetails, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Services: ${booking.serviceNames}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Customer: ${booking.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cost: ₹${booking.totalCost}",
                    fontWeight = FontWeight.Bold,
                    color = BikePrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallCustomer,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = BikeAccentGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Customer")
                }

                when (booking.status) {
                    BookingStatus.PENDING -> {
                        Button(
                            onClick = { onUpdateStatus(BookingStatus.ACCEPTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = BikeAccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept")
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(BookingStatus.CANCELLED) },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Decline")
                        }
                    }
                    BookingStatus.ACCEPTED -> {
                        Button(
                            onClick = { onUpdateStatus(BookingStatus.IN_PROGRESS) },
                            colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Service")
                        }
                    }
                    BookingStatus.IN_PROGRESS -> {
                        Button(
                            onClick = { onUpdateStatus(BookingStatus.COMPLETED) },
                            colors = ButtonDefaults.buttonColors(containerColor = BikeAccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Complete")
                        }
                    }
                    BookingStatus.COMPLETED -> {
                        AssistChip(
                            onClick = {},
                            label = { Text("Service Finished ✓") }
                        )
                    }
                    BookingStatus.CANCELLED -> {
                        AssistChip(
                            onClick = {},
                            label = { Text("Cancelled") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: BookingStatus) {
    val (color, text) = when (status) {
        BookingStatus.PENDING -> Pair(BikeSecondary, "Pending")
        BookingStatus.ACCEPTED -> Pair(BikePrimary, "Accepted")
        BookingStatus.IN_PROGRESS -> Pair(Color(0xFF8B5CF6), "In Progress")
        BookingStatus.COMPLETED -> Pair(BikeAccentGreen, "Completed")
        BookingStatus.CANCELLED -> Pair(BikeEmergencyRed, "Cancelled")
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
