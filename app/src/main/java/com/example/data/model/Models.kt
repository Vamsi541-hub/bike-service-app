package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    CUSTOMER,
    SERVICE_PROVIDER,
    ADMIN
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val password: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val location: String = "Mumbai, India",
    // Service provider specific
    val businessName: String = "",
    val servicesOffered: String = "",
    val workingHours: String = "9:00 AM - 8:00 PM",
    val isVerified: Boolean = true
)

enum class FuelType {
    PETROL,
    EV,
    HYBRID
}

@Entity(tableName = "bikes")
data class Bike(
    @PrimaryKey val id: String,
    val userId: String,
    val brand: String,
    val model: String,
    val variant: String,
    val year: Int,
    val fuelType: FuelType,
    val registrationNumber: String,
    val isSelected: Boolean = false,
    val lastServiceDate: String = "12 Aug 2026",
    val nextServiceDue: String = "12 Nov 2026",
    val insuranceExpiry: String = "24 Dec 2026",
    val pollutionExpiry: String = "15 Jan 2027",
    val currentOdometerKm: Int = 8450
)

@Entity(tableName = "services")
data class ServiceItem(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val description: String,
    val estimatedPrice: Int,
    val durationMinutes: Int,
    val iconKey: String
)

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey val id: String,
    val name: String,
    val businessName: String,
    val phone: String,
    val email: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val reviewsCount: Int,
    val services: String, // comma separated
    val workingHours: String,
    val isOpen: Boolean = true,
    val distanceKm: Double = 1.8,
    val isApproved: Boolean = true
)

enum class BookingStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey val id: String,
    val userId: String,
    val customerName: String,
    val customerPhone: String,
    val providerId: String,
    val providerName: String,
    val bikeDetails: String, // e.g. Yamaha MT-15 (Petrol, 2024)
    val serviceNames: String, // comma separated
    val totalCost: Int,
    val bookingDate: String,
    val bookingTime: String,
    val status: BookingStatus = BookingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "spare_parts")
data class SparePart(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val compatibleBikes: String, // comma-separated models or "Universal"
    val rating: Double = 4.8,
    val inStock: Boolean = true,
    val stockCount: Int = 14,
    val description: String = ""
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val id: String,
    val partId: String,
    val name: String,
    val price: Int,
    val quantity: Int = 1
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "BOOKING", // BOOKING, EMERGENCY, MAINTENANCE, PROMO
    val isRead: Boolean = false
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val quickReplies: List<String> = emptyList(),
    val imageUri: String? = null
)

data class CustomizationOption(
    val id: String,
    val category: String, // Mirrors, Exhaust, Seat, Graphics, Lights, Indicators, Hand grips, Crash guards, Windshield
    val name: String,
    val price: Int,
    val description: String,
    val visualType: String // e.g., "bar_end", "slip_on", "racing_camo"
)

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val bikeId: String,
    val bikeDetails: String,
    val title: String,
    val serviceType: String, // "Engine & Oil", "Brakes", "Tyres & Wheels", "Chain & Sprocket", "General Service", "Electrical & Battery", "DIY Repair"
    val date: String, // e.g., "18 Sep 2026"
    val odometerKm: Int,
    val workshopOrMechanic: String,
    val cost: Int,
    val partsReplaced: String = "",
    val notes: String = "",
    val invoiceNumber: String = ""
)

@Entity(tableName = "maintenance_reminders")
data class MaintenanceReminder(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val bikeId: String,
    val title: String,
    val componentCategory: String, // "Oil & Lubrication", "Drivetrain", "Braking System", "Engine & Filters", "Periodic Inspection", "Statutory & Insurance"
    val intervalKm: Int, // e.g., 5000 km
    val intervalDays: Int, // e.g., 180 days
    val lastServicedKm: Int, // e.g., 10000 km
    val dueKm: Int, // e.g., 15000 km
    val dueDate: String, // e.g., "15 Nov 2026"
    val isCompleted: Boolean = false,
    val notes: String = ""
)

