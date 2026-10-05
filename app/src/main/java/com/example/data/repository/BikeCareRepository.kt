package com.example.data.repository

import com.example.data.local.BikeCareDao
import com.example.data.model.*
import com.example.data.auth.PasswordHasher
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class BikeCareRepository(
    private val dao: BikeCareDao,
    private val syncManager: CloudSyncManager,
    private val geminiService: com.example.data.gemini.GeminiChatService
) {
    val allBikes: Flow<List<Bike>> = dao.getAllBikes()
    val selectedBike: Flow<Bike?> = dao.getSelectedBike()
    val allServices: Flow<List<ServiceItem>> = dao.getAllServices()
    val allProviders: Flow<List<ServiceProvider>> = dao.getAllProviders()
    val approvedProviders: Flow<List<ServiceProvider>> = dao.getApprovedProviders()
    val allBookings: Flow<List<Booking>> = dao.getAllBookings()
    val userBookings: Flow<List<Booking>> = dao.getAllBookings()
    val allParts: Flow<List<SparePart>> = dao.getAllParts()
    val cartItems: Flow<List<CartItem>> = dao.getCartItems()
    val notifications: Flow<List<AppNotification>> = dao.getAllNotifications()
    val serviceRecords: Flow<List<ServiceRecord>> = dao.getAllServiceRecords()
    val maintenanceReminders: Flow<List<MaintenanceReminder>> = dao.getAllReminders()

    fun getUser(userId: String): Flow<User?> = dao.getUserById(userId)
    suspend fun findUser(emailOrPhone: String): User? = dao.findUserByEmailOrPhone(emailOrPhone.trim())

    suspend fun authenticateUser(emailOrPhone: String, password: String): User? {
        val user = dao.findUserByEmailOrPhone(emailOrPhone.trim()) ?: return null
        return when {
            PasswordHasher.isHashed(user.password) && PasswordHasher.matches(password, user.password) -> user
            !PasswordHasher.isHashed(user.password) && user.password == password -> {
                val upgraded = user.copy(password = PasswordHasher.hash(password))
                dao.insertUser(upgraded)
                upgraded
            }
            else -> null
        }
    }
    fun getBookingsForProvider(providerId: String): Flow<List<Booking>> = dao.getBookingsForProvider(providerId)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        // Seed default selected bike if empty
        val existingBikes = dao.getAllBikes().firstOrNull()
        if (existingBikes.isNullOrEmpty()) {
            dao.insertBike(
                Bike(
                    id = "bike_1",
                    userId = "user_default",
                    brand = "Yamaha",
                    model = "MT-15",
                    variant = "Version 2.0 Deluxe",
                    year = 2024,
                    fuelType = FuelType.PETROL,
                    registrationNumber = "KA-05-ER-4092",
                    isSelected = true,
                    lastServiceDate = "14 Aug 2026",
                    nextServiceDue = "14 Nov 2026",
                    insuranceExpiry = "18 Dec 2026",
                    pollutionExpiry = "28 Jan 2027",
                    currentOdometerKm = 5240
                )
            )
            dao.insertBike(
                Bike(
                    id = "bike_2",
                    userId = "user_default",
                    brand = "Ather",
                    model = "450X",
                    variant = "Gen 3.7",
                    year = 2025,
                    fuelType = FuelType.EV,
                    registrationNumber = "KA-01-EV-8821",
                    isSelected = false,
                    lastServiceDate = "02 Sep 2026",
                    nextServiceDue = "02 Dec 2026",
                    insuranceExpiry = "10 Jan 2027",
                    pollutionExpiry = "N/A (Zero Emission)",
                    currentOdometerKm = 3120
                )
            )
        }

        // Seed Services
        val existingServices = dao.getAllServices().firstOrNull()
        if (existingServices.isNullOrEmpty()) {
            dao.insertServices(
                listOf(
                    ServiceItem("srv_1", "General Service", "Full Bike Periodic Service", "Complete 40-point inspection, oil top-up, filter clean, brake tuning, washing and lube.", 999, 90, "build"),
                    ServiceItem("srv_2", "Engine Service", "Engine Diagnostics & Tuning", "Spark plug replacement, valve clearance check, tappet adjustment, carburetor/FI clean.", 1499, 120, "engineering"),
                    ServiceItem("srv_3", "Oil Change", "Engine Oil & Filter Replacement", "Motul/Castrol fully synthetic oil replacement + OEM oil filter replacement.", 499, 30, "oil_barrel"),
                    ServiceItem("srv_4", "Brake Service", "Front & Rear Brake Overhaul", "Pad cleaning/replacement, caliper greasing, brake fluid bleeding & tightening.", 399, 45, "settings"),
                    ServiceItem("srv_5", "Chain Service", "Chain Clean & Lubrication", "Degreaser deep cleaning, tension adjustment, slack alignment, high-viscosity Motul spray.", 249, 25, "link"),
                    ServiceItem("srv_6", "Tyre Service", "Tyre Inspection, Balance & Puncture", "Tread depth measurement, rim balance check, nitrogen filling, tube/tubeless check.", 299, 35, "tire_repair"),
                    ServiceItem("srv_7", "Battery Service", "Battery Health Check & Terminal Cleaning", "Voltage check under load, electrolyte top-up, anti-corrosion spray & charging.", 199, 20, "battery_charging_full"),
                    ServiceItem("srv_8", "Electrical Repair", "Wiring, Lights & Fuse Diagnostics", "Headlight/tail-lamp fix, horn repair, fuse inspection, alternator & stator testing.", 450, 45, "bolt"),
                    ServiceItem("srv_9", "Washing", "Foam Wash & Ceramic Polish", "High pressure underbody wash, dual active foam, hydrophobic spray polish & chain dry.", 299, 40, "local_car_wash"),
                    ServiceItem("srv_10", "Puncture Repair", "On-Spot Tubeless/Tube Puncture", "Emergency puncture plug repair, valve replacement, high pressure portable air fill.", 149, 15, "handyman"),
                    ServiceItem("srv_11", "Pickup & Drop", "Doorstep Bike Towing & Shuttle", "Safe hydraulic ramp pickup van, real-time live transport tracking to workshop.", 349, 45, "two_wheeler"),
                    ServiceItem("srv_12", "Customization", "Exhaust, Wrap & Ergonomic Tuning", "Slip-on exhaust fitting, crash guard install, auxiliary lighting, visor install.", 799, 90, "auto_fix_high")
                )
            )
        }

        // Seed Service Providers
        val existingProviders = dao.getAllProviders().firstOrNull()
        if (existingProviders.isNullOrEmpty()) {
            dao.insertProviders(
                listOf(
                    ServiceProvider(
                        id = "prov_1",
                        name = "Ramesh Kumar",
                        businessName = "Apex MotoCare & Tuning Studio",
                        phone = "+91 98451 22334",
                        email = "apex.motocare@gmail.com",
                        address = "4th Cross, 80 Feet Road, Koramangala 4th Block",
                        latitude = 12.9348,
                        longitude = 77.6254,
                        rating = 4.9,
                        reviewsCount = 142,
                        services = "General Service, Engine Tuning, Oil Change, Brake Overhaul, EV Diagnostics",
                        workingHours = "8:30 AM - 8:30 PM",
                        isOpen = true,
                        distanceKm = 1.2,
                        isApproved = true
                    ),
                    ServiceProvider(
                        id = "prov_2",
                        name = "Imran Khan",
                        businessName = "ProRider Pitstop & Towing 24/7",
                        phone = "+91 97410 88990",
                        email = "prorider.pitstop@gmail.com",
                        address = "Near Silk Board Flyover, Outer Ring Road",
                        latitude = 12.9174,
                        longitude = 77.6231,
                        rating = 4.8,
                        reviewsCount = 215,
                        services = "Puncture Repair, 24/7 Towing, Fuel Drop, Battery Jumpstart, Chain Service",
                        workingHours = "24 Hours (Emergency Available)",
                        isOpen = true,
                        distanceKm = 2.1,
                        isApproved = true
                    ),
                    ServiceProvider(
                        id = "prov_3",
                        name = "Suresh Reddy",
                        businessName = "VoltSpeed EV Center & Quick Charging",
                        phone = "+91 94480 33445",
                        email = "voltspeed.blore@gmail.com",
                        address = "HSR Layout Sector 1, 14th Main",
                        latitude = 12.9121,
                        longitude = 77.6446,
                        rating = 4.7,
                        reviewsCount = 89,
                        services = "EV Battery Diagnostics, Controller Repair, Fast Charging, Electrical, Motor Check",
                        workingHours = "9:00 AM - 9:00 PM",
                        isOpen = true,
                        distanceKm = 2.8,
                        isApproved = true
                    ),
                    ServiceProvider(
                        id = "prov_4",
                        name = "Gurpreet Singh",
                        businessName = "Classic Thumper & Bullet Specialists",
                        phone = "+91 98860 11223",
                        email = "classicthumpers@gmail.com",
                        address = "Indiranagar 100ft Road, Near Metro Station",
                        latitude = 12.9719,
                        longitude = 77.6412,
                        rating = 4.9,
                        reviewsCount = 188,
                        services = "Royal Enfield Specialist, Engine Rebuild, Custom Exhaust, Leather Seats",
                        workingHours = "9:30 AM - 7:30 PM",
                        isOpen = true,
                        distanceKm = 3.6,
                        isApproved = true
                    )
                )
            )
        }

        // Seed Spare Parts
        val existingParts = dao.getAllParts().firstOrNull()
        if (existingParts.isNullOrEmpty()) {
            dao.insertParts(
                listOf(
                    SparePart("p_1", "Motul 7100 10W40 Fully Synthetic Oil 1L", "Engine Parts", 849, "Universal, Yamaha MT-15, KTM Duke, Bajaj Pulsar", 4.9, true, 28, "100% Synthetic 4-Stroke motorcycle lubricant with ester technology for razor sharp throttle response."),
                    SparePart("p_2", "Ceramic Sintered Front Brake Pads", "Brake Parts", 599, "Yamaha MT-15, Yamaha R15 V3/V4", 4.8, true, 15, "High coefficient friction brake pads for superior stopping power and heat fade resistance."),
                    SparePart("p_3", "Michelin Pilot Street 2 Rear Tyre 140/70-17", "Tyres", 3899, "Yamaha MT-15, KTM Duke 200, Dominar 400", 4.9, true, 8, "Deep grooved sports compound tyre for outstanding wet road grip and longevity."),
                    SparePart("p_4", "Amaron Pro Rider 12V 5Ah Maintenance Free Battery", "Batteries", 1250, "Universal, Yamaha MT-15, Honda Hornet, TVS Apache", 4.7, true, 19, "Factory charged AGM valve-regulated lead acid battery with 24-month replacement warranty."),
                    SparePart("p_5", "Rolon Brass Chain & Sprocket Heavy Duty Kit", "Chain & Sprocket", 2150, "Yamaha MT-15, Yamaha R15 V3/V4", 4.9, true, 12, "Golden brass coated alloy steel link chain with high wear resistance O-rings."),
                    SparePart("p_6", "NightEye Super Bright LED Headlight Bulb 6500K", "Lights", 899, "Universal, H4 Socket", 4.6, true, 30, "Aviation aluminum heatsink with 36W 8000LM output and crisp white cutoff beam."),
                    SparePart("p_7", "CNC Billet Bar-End Convex Mirrors (Pair)", "Mirrors", 799, "Universal, 22mm / 7/8 inch Handlebars", 4.7, true, 22, "Anti-glare blue convex mirror glass with aircraft grade CNC aluminum stem."),
                    SparePart("p_8", "K&N High-Flow Washable Air Filter", "Filters", 4200, "Yamaha MT-15, Yamaha R15 V3/V4", 4.9, true, 6, "Designed to increase horsepower and acceleration while providing exceptional filtration."),
                    SparePart("p_9", "NGK Iridium Spark Plug CR9EIX", "Electrical Parts", 620, "Yamaha MT-15, KTM Duke, TVS Apache RR310", 4.8, true, 25, "Ultra fine 0.6mm laser welded iridium tip provides high durability and consistently stable spark."),
                    SparePart("p_10", "Heavy Duty Slider Crash Guard with Pucks", "Accessories", 2499, "Yamaha MT-15", 4.9, true, 10, "Reinforced seamless steel tubing with high-density Delrin sliders to protect engine and frame.")
                )
            )
        }

        // Seed Sample Notifications
        val existingNotifs = dao.getAllNotifications().firstOrNull()
        if (existingNotifs.isNullOrEmpty()) {
            dao.insertNotification(
                AppNotification(
                    id = "notif_1",
                    title = "Upcoming Service Reminder",
                    message = "Your Yamaha MT-15 is due for regular maintenance in 14 days (14 Nov 2026). Keep your engine in peak condition.",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    type = "MAINTENANCE"
                )
            )
            dao.insertNotification(
                AppNotification(
                    id = "notif_2",
                    title = "Insurance Renewal Alert",
                    message = "Your comprehensive two-wheeler policy expires on 18 Dec 2026. Avoid fines by renewing beforehand.",
                    timestamp = System.currentTimeMillis() - 172800000L,
                    type = "PROMO"
                )
            )

            // Seed Initial Service Records & Past Repairs
            dao.insertServiceRecords(
                listOf(
                    ServiceRecord(
                        id = "srv_rec_1",
                        bikeId = "bike_1",
                        bikeDetails = "Yamaha MT-15 V2 (2024)",
                        title = "10,000 KM Periodic Service & Synthetic Oil",
                        serviceType = "Engine & Oil",
                        date = "12 Sep 2026",
                        odometerKm = 10450,
                        workshopOrMechanic = "Apex MotoCare Studio",
                        cost = 2450,
                        partsReplaced = "Motul 7100 10W40 (1.1L), OEM Oil Filter, Crush Gasket",
                        notes = "Chain slack adjusted to 25mm. All brake calipers cleaned and bleed checked.",
                        invoiceNumber = "INV-2026-904"
                    ),
                    ServiceRecord(
                        id = "srv_rec_2",
                        bikeId = "bike_1",
                        bikeDetails = "Yamaha MT-15 V2 (2024)",
                        title = "Rear Brake Pad Replacement & Disc Deglaze",
                        serviceType = "Brakes",
                        date = "18 Jul 2026",
                        odometerKm = 8200,
                        workshopOrMechanic = "Brembo QuickStop Garage",
                        cost = 1150,
                        partsReplaced = "Ceramic Performance Brake Pads",
                        notes = "Replaced thin rear pads. Rotor surface in great shape.",
                        invoiceNumber = "INV-2026-681"
                    ),
                    ServiceRecord(
                        id = "srv_rec_3",
                        bikeId = "bike_1",
                        bikeDetails = "Yamaha MT-15 V2 (2024)",
                        title = "DIY Chain Clean, Slack Tension & Wax Lube",
                        serviceType = "Chain & Sprocket",
                        date = "22 Jun 2026",
                        odometerKm = 6800,
                        workshopOrMechanic = "Self DIY Garage",
                        cost = 450,
                        partsReplaced = "Motul C1 Clean & C2 Road Lube Spray",
                        notes = "Cleaned with grunge brush and applied high-tack chain wax.",
                        invoiceNumber = "DIY-SELF-01"
                    )
                )
            )

            // Seed Initial Maintenance Reminders
            dao.insertReminders(
                listOf(
                    MaintenanceReminder(
                        id = "rem_1",
                        bikeId = "bike_1",
                        title = "Engine Oil & Filter Replacement",
                        componentCategory = "Oil & Lubrication",
                        intervalKm = 5000,
                        intervalDays = 180,
                        lastServicedKm = 10450,
                        dueKm = 15450,
                        dueDate = "12 Mar 2027",
                        notes = "Recommended 10W40 Full Synthetic JASO MA2"
                    ),
                    MaintenanceReminder(
                        id = "rem_2",
                        bikeId = "bike_1",
                        title = "Drive Chain Cleaning & Lubrication",
                        componentCategory = "Drivetrain",
                        intervalKm = 500,
                        intervalDays = 21,
                        lastServicedKm = 11200,
                        dueKm = 11700,
                        dueDate = "Every 500 km or after heavy rain",
                        notes = "Maintain 20-30 mm chain play"
                    ),
                    MaintenanceReminder(
                        id = "rem_3",
                        bikeId = "bike_1",
                        title = "Spark Plug Check & Gap Inspection",
                        componentCategory = "Periodic Inspection",
                        intervalKm = 12000,
                        intervalDays = 365,
                        lastServicedKm = 0,
                        dueKm = 12000,
                        dueDate = "Due soon at 12,000 km",
                        notes = "Inspect electrode color for fuel mixture"
                    ),
                    MaintenanceReminder(
                        id = "rem_4",
                        bikeId = "bike_1",
                        title = "Brake Fluid Flush & Bleed (DOT 4)",
                        componentCategory = "Braking System",
                        intervalKm = 10000,
                        intervalDays = 365,
                        lastServicedKm = 8200,
                        dueKm = 18200,
                        dueDate = "18 Jul 2027",
                        notes = "Inspect master cylinder window for moisture"
                    ),
                    MaintenanceReminder(
                        id = "rem_5",
                        bikeId = "bike_1",
                        title = "Air Filter Cleaning / Replacement",
                        componentCategory = "Engine & Filters",
                        intervalKm = 8000,
                        intervalDays = 240,
                        lastServicedKm = 10450,
                        dueKm = 18450,
                        dueDate = "12 May 2027",
                        notes = "Paper element filter - replace, do not wash"
                    )
                )
            )
        }
    }

    // --- User & Role Actions ---
    suspend fun saveUser(user: User) {
        val safePassword = when {
            user.password.isBlank() -> user.password
            PasswordHasher.isHashed(user.password) -> user.password
            else -> PasswordHasher.hash(user.password)
        }
        dao.insertUser(user.copy(password = safePassword))
    }

    // --- Bike Actions ---
    suspend fun addBike(bike: Bike) {
        if (bike.isSelected) {
            dao.clearSelectedBikes(bike.userId)
        }
        dao.insertBike(bike)
        syncManager.triggerSync("Added bike: ${bike.brand} ${bike.model}")
    }

    suspend fun selectBike(bikeId: String, userId: String) {
        dao.clearSelectedBikes(userId)
        dao.selectBike(bikeId)
    }

    suspend fun deleteBike(bike: Bike) {
        dao.deleteBike(bike)
    }

    // --- Booking Actions ---
    suspend fun createBooking(booking: Booking) {
        dao.insertBooking(booking)
        dao.insertNotification(
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Booking Confirmed (#${booking.id.takeLast(6).uppercase()})",
                message = "Your appointment with ${booking.providerName} for ${booking.bikeDetails} is scheduled on ${booking.bookingDate} at ${booking.bookingTime}.",
                type = "BOOKING"
            )
        )
        syncManager.triggerSync("Created booking #${booking.id}")
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        dao.updateBookingStatus(bookingId, status)
        dao.insertNotification(
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Service Status Updated",
                message = "Booking #${bookingId.takeLast(6).uppercase()} is now marked as ${status.name.replace('_', ' ')}.",
                type = "BOOKING"
            )
        )
        syncManager.triggerSync("Booking #${bookingId} changed to ${status.name}")
    }

    // --- Cart Actions ---
    suspend fun addToCart(part: SparePart) {
        dao.insertCartItem(
            CartItem(
                id = UUID.randomUUID().toString(),
                partId = part.id,
                name = part.name,
                price = part.price,
                quantity = 1
            )
        )
    }

    suspend fun removeFromCart(cartItemId: String) {
        dao.removeCartItem(cartItemId)
    }

    suspend fun clearCart() {
        dao.clearCart()
    }

    // --- Provider Actions ---
    suspend fun updateProviderOpenStatus(providerId: String, isOpen: Boolean) {
        dao.updateProviderOpenStatus(providerId, isOpen)
    }

    suspend fun updateProviderApproval(providerId: String, approved: Boolean) {
        dao.updateProviderApproval(providerId, approved)
    }

    // --- Notification Actions ---
    suspend fun markNotificationRead(id: String) {
        dao.markNotificationAsRead(id)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsAsRead()
    }

    // --- Service Record & Past Repairs Actions ---
    suspend fun addServiceRecord(record: ServiceRecord) {
        dao.insertServiceRecord(record)
        syncManager.triggerSync("Logged service repair: ${record.title}")
    }

    suspend fun deleteServiceRecord(id: String) {
        dao.deleteServiceRecord(id)
    }

    // --- Maintenance Interval Reminder Actions ---
    suspend fun addMaintenanceReminder(reminder: MaintenanceReminder) {
        dao.insertReminder(reminder)
        syncManager.triggerSync("Set reminder: ${reminder.title}")
    }

    suspend fun updateMaintenanceReminder(reminder: MaintenanceReminder) {
        dao.updateReminder(reminder)
    }

    suspend fun deleteMaintenanceReminder(id: String) {
        dao.deleteReminder(id)
    }

    suspend fun markReminderServiced(
        reminder: MaintenanceReminder,
        completedAtKm: Int,
        dateStr: String,
        cost: Int = 0,
        mechanic: String = "Scheduled Service"
    ) {
        val nextDueKm = completedAtKm + reminder.intervalKm
        val updated = reminder.copy(
            lastServicedKm = completedAtKm,
            dueKm = nextDueKm,
            dueDate = "Due in ${reminder.intervalKm} km",
            isCompleted = false
        )
        dao.updateReminder(updated)

        // Automatically log to past repairs history
        val historyEntry = ServiceRecord(
            bikeId = reminder.bikeId,
            bikeDetails = "Yamaha MT-15 V2",
            title = reminder.title,
            serviceType = reminder.componentCategory,
            date = dateStr,
            odometerKm = completedAtKm,
            workshopOrMechanic = mechanic,
            cost = cost,
            partsReplaced = "${reminder.title} components",
            notes = "Completed scheduled interval checkup. Next due at $nextDueKm km."
        )
        dao.insertServiceRecord(historyEntry)

        // Also add an app notification
        dao.insertNotification(
            AppNotification(
                id = java.util.UUID.randomUUID().toString(),
                title = "Service Completed ✓",
                message = "${reminder.title} recorded at $completedAtKm km. Next interval scheduled for $nextDueKm km.",
                type = "MAINTENANCE"
            )
        )
        syncManager.triggerSync("Serviced reminder: ${reminder.title}")
    }

    // --- Cloud Sync ---
    suspend fun triggerManualSync(): Boolean {
        return syncManager.triggerSync("Manual backup of bikes, bookings & preferences")
    }

    fun getSyncManager(): CloudSyncManager = syncManager
    fun getGeminiService(): com.example.data.gemini.GeminiChatService = geminiService
}
