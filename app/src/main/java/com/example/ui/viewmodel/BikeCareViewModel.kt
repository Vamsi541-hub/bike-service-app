package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.location.DeviceLocationService
import com.example.data.location.NearbyPlace
import com.example.data.location.NearbyPlacesService
import com.example.data.auth.SessionStore
import com.example.data.repository.BikeCareRepository
import com.example.data.sync.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AppScreen {
    object Main : AppScreen()
    object Auth : AppScreen()
    object MapNavigation : AppScreen()
    data class BookingFlow(val preselectedService: ServiceItem? = null, val preselectedProvider: ServiceProvider? = null) : AppScreen()
    data class BookingConfirmation(val booking: Booking) : AppScreen()
    object CustomizationStudio : AppScreen()
    object AssistantChat : AppScreen()
    object ProviderDashboard : AppScreen()
    object AdminDashboard : AppScreen()
    object ServiceHistory : AppScreen()
    object Notifications : AppScreen()
    object CartCheckout : AppScreen()
}

class BikeCareViewModel(
    private val repository: BikeCareRepository,
    private val locationService: DeviceLocationService? = null,
    private val nearbyPlacesService: NearbyPlacesService? = null,
    private val sessionStore: SessionStore? = null
) : ViewModel() {

    // --- Active Screen State ---
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Auth)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // --- Active Bottom Nav Tab ---
    private val _currentTab = MutableStateFlow(com.example.ui.components.AppTab.HOME)
    val currentTab: StateFlow<com.example.ui.components.AppTab> = _currentTab.asStateFlow()

    // --- Theme Mode ---
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // --- Auth & User State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionStore?.getUserId()
            if (!userId.isNullOrBlank()) {
                repository.getUser(userId)?.let {
                    _currentUser.value = it
                    _currentScreen.value = AppScreen.Main
                }
            }
        }
    }

    // --- Bikes ---
    val allBikes: StateFlow<List<Bike>> = repository.allBikes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedBike: StateFlow<Bike?> = repository.selectedBike
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Services ---
    val allServices: StateFlow<List<ServiceItem>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Providers ---
    val approvedProviders: StateFlow<List<ServiceProvider>> = repository.approvedProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Bookings ---
    val userBookings: StateFlow<List<Booking>> = repository.userBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookings: StateFlow<List<Booking>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Spare Parts & Cart ---
    val allParts: StateFlow<List<SparePart>> = repository.allParts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Notifications ---
    val notifications: StateFlow<List<AppNotification>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifsCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Service Records & Maintenance Reminders ---
    val serviceRecords: StateFlow<List<ServiceRecord>> = repository.serviceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceReminders: StateFlow<List<MaintenanceReminder>> = repository.maintenanceReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cloud Sync ---
    val syncState: StateFlow<SyncState> = repository.getSyncManager().syncState

    // --- Real device location / nearby places ---
    private val _userLocation = MutableStateFlow<com.example.data.location.UserLocation?>(null)
    val userLocation: StateFlow<com.example.data.location.UserLocation?> = _userLocation.asStateFlow()

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces.asStateFlow()

    private val _locationLoading = MutableStateFlow(false)
    val locationLoading: StateFlow<Boolean> = _locationLoading.asStateFlow()

    fun refreshNearbyPlaces() {
        val locator = locationService ?: return
        val places = nearbyPlacesService ?: return
        viewModelScope.launch {
            _locationLoading.value = true
            val location = locator.getLastKnownLocation()
            if (location != null) {
                _userLocation.value = location
                val fuelType = selectedBike.value?.fuelType ?: FuelType.PETROL
                _nearbyPlaces.value = places.findNearby(location, fuelType)
            }
            _locationLoading.value = false
        }
    }

    // --- Active Dialogs ---
    var callDialogProvider by mutableStateOf<Pair<String, String>?>(null)
    var showAddBikeSheet by mutableStateOf(false)
    var selectedMapPoiId by mutableStateOf<String?>(null)

    // --- Multi-select services in Services Screen ---
    private val _selectedServiceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedServiceIds: StateFlow<Set<String>> = _selectedServiceIds.asStateFlow()

    // --- Chatbot Conversation State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "assistant",
                text = "Hello! I am your BikeCare Assistant 🏍️. How can I help you with your bike today? Ask me about noises, maintenance, oil change intervals, or emergency diagnostics.",
                quickReplies = listOf(
                    "Why is my bike not starting?",
                    "My bike is making a strange noise",
                    "When should I change engine oil?",
                    "My brakes are making noise",
                    "My tyre is flat"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    val isChatbotTyping = MutableStateFlow(false)

    // --- Navigation Controls ---
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (_currentScreen.value !is AppScreen.Main) {
            _currentScreen.value = AppScreen.Main
        }
    }

    fun selectTab(tab: com.example.ui.components.AppTab) {
        _currentTab.value = tab
        if (_currentScreen.value !is AppScreen.Main) {
            _currentScreen.value = AppScreen.Main
        }
    }

    fun logout() {
        sessionStore?.clear()
        _currentUser.value = null
        _currentScreen.value = AppScreen.Auth
    }

    suspend fun checkExistingUser(emailOrPhone: String): User? {
        return repository.findUser(emailOrPhone)
    }

    suspend fun authenticateUser(emailOrPhone: String, password: String): User? {
        return repository.authenticateUser(emailOrPhone, password)
    }

    fun loginUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
            sessionStore?.saveUserId(user.id)
            _currentUser.value = user
            _currentScreen.value = AppScreen.Main
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // --- Bike Actions ---
    fun addBike(bike: Bike) {
        viewModelScope.launch {
            repository.addBike(bike)
            showAddBikeSheet = false
        }
    }

    fun selectBike(bikeId: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: "current_user"
            repository.selectBike(bikeId, userId)
        }
    }

    fun deleteBike(bike: Bike) {
        viewModelScope.launch {
            repository.deleteBike(bike)
        }
    }

    // --- Service Selection ---
    fun toggleServiceSelection(serviceId: String) {
        val current = _selectedServiceIds.value.toMutableSet()
        if (current.contains(serviceId)) {
            current.remove(serviceId)
        } else {
            current.add(serviceId)
        }
        _selectedServiceIds.value = current
    }

    fun clearServiceSelection() {
        _selectedServiceIds.value = emptySet()
    }

    // --- Booking Actions ---
    fun submitBooking(
        provider: ServiceProvider,
        bike: Bike,
        services: List<ServiceItem>,
        date: String,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            val totalCost = services.sumOf { it.estimatedPrice }
            val newBooking = Booking(
                id = "BK-${UUID.randomUUID().toString().take(6).uppercase()}",
                userId = currentUser.value?.id ?: return@launch,
                customerName = currentUser.value?.name ?: "Customer",
                customerPhone = currentUser.value?.phone ?: return@launch,
                providerId = provider.id,
                providerName = provider.businessName,
                bikeDetails = "${bike.brand} ${bike.model} (${bike.fuelType.name}, ${bike.year})",
                serviceNames = services.joinToString(", ") { it.name },
                totalCost = totalCost,
                bookingDate = date,
                bookingTime = time,
                status = BookingStatus.PENDING,
                notes = notes
            )
            repository.createBooking(newBooking)
            clearServiceSelection()
            _currentScreen.value = AppScreen.BookingConfirmation(newBooking)
        }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, status)
        }
    }

    // --- Provider Actions ---
    fun toggleProviderOpenStatus(providerId: String, currentOpen: Boolean) {
        viewModelScope.launch {
            repository.updateProviderOpenStatus(providerId, !currentOpen)
        }
    }

    fun approveProvider(providerId: String, approve: Boolean) {
        viewModelScope.launch {
            repository.updateProviderApproval(providerId, approve)
        }
    }

    // --- Cart Actions ---
    fun addToCart(part: SparePart) {
        viewModelScope.launch {
            repository.addToCart(part)
        }
    }

    fun removeFromCart(itemId: String) {
        viewModelScope.launch {
            repository.removeFromCart(itemId)
        }
    }

    fun checkoutCart() {
        viewModelScope.launch {
            val currentItems = cartItems.value
            val total = currentItems.sumOf { it.price * it.quantity }
            repository.clearCart()
            repository.createBooking(
                Booking(
                    id = "ORDER-${UUID.randomUUID().toString().take(6).uppercase()}",
                    userId = currentUser.value?.id ?: return@launch,
                    customerName = currentUser.value?.name ?: "Customer",
                    customerPhone = currentUser.value?.phone ?: "+91 98765 43210",
                    providerId = "prov_1",
                    providerName = "Apex MotoCare Parts Fulfillment",
                    bikeDetails = selectedBike.value?.let { "${it.brand} ${it.model}" } ?: "Universal Two-Wheeler",
                    serviceNames = currentItems.joinToString(", ") { "${it.name} (x${it.quantity})" },
                    totalCost = total,
                    bookingDate = "Immediate Dispatch",
                    bookingTime = "Doorstep Delivery (48 hrs)",
                    status = BookingStatus.ACCEPTED
                )
            )
        }
    }

    // --- Service Record & Past Repairs Actions ---
    fun addServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            repository.addServiceRecord(record)
        }
    }

    fun deleteServiceRecord(id: String) {
        viewModelScope.launch {
            repository.deleteServiceRecord(id)
        }
    }

    // --- Maintenance Interval Reminder Actions ---
    fun addMaintenanceReminder(reminder: MaintenanceReminder) {
        viewModelScope.launch {
            repository.addMaintenanceReminder(reminder)
        }
    }

    fun deleteMaintenanceReminder(id: String) {
        viewModelScope.launch {
            repository.deleteMaintenanceReminder(id)
        }
    }

    fun markReminderServiced(
        reminder: MaintenanceReminder,
        completedAtKm: Int,
        dateStr: String,
        cost: Int = 0,
        mechanic: String = "Scheduled Service"
    ) {
        viewModelScope.launch {
            repository.markReminderServiced(reminder, completedAtKm, dateStr, cost, mechanic)
        }
    }

    // --- Notifications ---
    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    // --- Cloud Sync ---
    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.triggerManualSync()
        }
    }

    // --- Switch User Role (Demo Switcher for instant testing) ---
    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            val updated = when (role) {
                UserRole.CUSTOMER -> User(
                    id = "current_user",
                    name = "Rahul Sharma",
                    email = "rahul.sharma@example.com",
                    phone = "+91 98765 43210",
                    role = UserRole.CUSTOMER,
                    location = "Koramangala, Bengaluru"
                )
                UserRole.SERVICE_PROVIDER -> User(
                    id = "current_user",
                    name = "Ramesh Kumar (Owner)",
                    email = "apex.motocare@gmail.com",
                    phone = "+91 98451 22334",
                    role = UserRole.SERVICE_PROVIDER,
                    businessName = "Apex MotoCare & Tuning Studio",
                    servicesOffered = "General Service, Engine Tuning, Oil Change, Brake Overhaul",
                    location = "Koramangala 4th Block, Bengaluru"
                )
                UserRole.ADMIN -> User(
                    id = "current_user",
                    name = "Super Admin (BikeCare HQ)",
                    email = "admin@bikecare.io",
                    phone = "+91 80010 00001",
                    role = UserRole.ADMIN,
                    location = "HQ Command Center"
                )
            }
            repository.saveUser(updated)
        }
    }

    // --- BikeCare Assistant Chat Engine (Gemini Powered + Multimodal) ---
    fun sendChatMessage(userText: String, imageUri: android.net.Uri? = null) {
        val effectiveText = userText.ifBlank {
            if (imageUri != null) "Please inspect this bike part in the photo and tell me if there are any issues, wear, or needed maintenance." else ""
        }
        if (effectiveText.isBlank()) return

        val userMsg = ChatMessage(
            sender = "user",
            text = effectiveText,
            imageUri = imageUri?.toString()
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            isChatbotTyping.value = true

            val bikeInfo = selectedBike.value?.let { "${it.brand} ${it.model} (${it.fuelType.name}, ${it.year})" }
            val geminiReply = repository.getGeminiService().generateMultiTurnReply(
                history = _chatMessages.value.dropLast(1),
                latestUserPrompt = effectiveText,
                imageUri = imageUri,
                selectedBikeDetails = bikeInfo
            )

            val smartReplies = when {
                imageUri != null -> listOf("Book Diagnostic Inspection", "Find Nearest Workshop", "Is it safe to ride?")
                effectiveText.contains("noise", ignoreCase = true) -> listOf("From the engine", "When applying brakes", "Chain noise", "Book Workshop")
                effectiveText.contains("oil", ignoreCase = true) -> listOf("Order Motul Synthetic Oil", "Book Oil Change", "How to check dipstick?")
                effectiveText.contains("brake", ignoreCase = true) -> listOf("Book Brake Pad Replacement", "Check Brake Fluid")
                effectiveText.contains("start", ignoreCase = true) -> listOf("Request Jumpstart", "Nearest Mechanic", "Battery Health Check")
                else -> listOf("Find Nearby Mechanic", "Book Service", "View Compatible Parts")
            }

            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = "assistant",
                text = geminiReply,
                quickReplies = smartReplies
            )
            isChatbotTyping.value = false
        }
    }
}
