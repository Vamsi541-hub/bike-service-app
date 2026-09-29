package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.BikeCareDatabase
import com.example.data.model.FuelType
import com.example.data.repository.BikeCareRepository
import com.example.data.sync.CloudSyncManager
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.BikeCareTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BikeCareViewModel
import com.example.ui.viewmodel.BikeCareViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = BikeCareDatabase.getInstance(applicationContext)
        val syncManager = CloudSyncManager(applicationContext)
        val geminiService = com.example.data.gemini.GeminiChatService(applicationContext)
        val repository = BikeCareRepository(database.dao(), syncManager, geminiService)

        setContent {
            val viewModel: BikeCareViewModel = viewModel(
                factory = BikeCareViewModelFactory(repository)
            )

            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val selectedBike by viewModel.selectedBike.collectAsStateWithLifecycle()
            val allBikes by viewModel.allBikes.collectAsStateWithLifecycle()
            val allServices by viewModel.allServices.collectAsStateWithLifecycle()
            val approvedProviders by viewModel.approvedProviders.collectAsStateWithLifecycle()
            val allProviders by viewModel.allProviders.collectAsStateWithLifecycle()
            val userBookings by viewModel.userBookings.collectAsStateWithLifecycle()
            val allBookings by viewModel.allBookings.collectAsStateWithLifecycle()
            val allParts by viewModel.allParts.collectAsStateWithLifecycle()
            val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val unreadCount by viewModel.unreadNotifsCount.collectAsStateWithLifecycle()
            val syncState by viewModel.syncState.collectAsStateWithLifecycle()
            val selectedServiceIds by viewModel.selectedServiceIds.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val isChatTyping by viewModel.isChatbotTyping.collectAsStateWithLifecycle()

            // Back button handling
            BackHandler(enabled = currentScreen !is AppScreen.Main || currentTab != AppTab.HOME) {
                if (currentScreen !is AppScreen.Main) {
                    viewModel.navigateBack()
                } else if (currentTab != AppTab.HOME) {
                    viewModel.selectTab(AppTab.HOME)
                }
            }

            BikeCareTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is AppScreen.Main -> {
                            Scaffold(
                                topBar = {
                                    BikeCareTopBar(
                                        title = "BikeCare",
                                        tagline = "Your Bike. Your Care.",
                                        unreadNotificationsCount = unreadCount,
                                        onNotificationsClick = { viewModel.navigateTo(AppScreen.Notifications) },
                                        onChatbotClick = { viewModel.navigateTo(AppScreen.AssistantChat) },
                                        onSyncClick = { viewModel.triggerCloudSync() }
                                    )
                                },
                                bottomBar = {
                                    BikeCareBottomBar(
                                        currentTab = currentTab,
                                        onTabSelected = { viewModel.selectTab(it) }
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentTab) {
                                        AppTab.HOME -> {
                                            HomeScreen(
                                                user = currentUser,
                                                selectedBike = selectedBike,
                                                allBikes = allBikes,
                                                providers = approvedProviders,
                                                onAddBikeClick = { viewModel.showAddBikeSheet = true },
                                                onSelectBikeClick = { viewModel.selectBike(it.id) },
                                                onEmergencyOptionClick = { tag ->
                                                    viewModel.selectedMapPoiId = null
                                                    viewModel.navigateTo(AppScreen.MapNavigation)
                                                },
                                                onQuickServiceClick = { srvName ->
                                                    val matched = allServices.find { it.name.contains(srvName, ignoreCase = true) }
                                                    viewModel.navigateTo(AppScreen.BookingFlow(preselectedService = matched))
                                                },
                                                onProviderCall = { name, phone ->
                                                    viewModel.callDialogProvider = Pair(name, phone)
                                                },
                                                onProviderDirections = { provId ->
                                                    viewModel.selectedMapPoiId = provId
                                                    viewModel.navigateTo(AppScreen.MapNavigation)
                                                },
                                                onProviderBook = { prov ->
                                                    viewModel.navigateTo(AppScreen.BookingFlow(preselectedProvider = prov))
                                                },
                                                onViewAllServices = { viewModel.selectTab(AppTab.SERVICES) },
                                                onOpenChatbot = { viewModel.navigateTo(AppScreen.AssistantChat) }
                                            )
                                        }

                                        AppTab.SERVICES -> {
                                            ServicesScreen(
                                                services = allServices,
                                                selectedServiceIds = selectedServiceIds,
                                                onToggleService = { viewModel.toggleServiceSelection(it) },
                                                onBookSingleService = { service ->
                                                    viewModel.navigateTo(AppScreen.BookingFlow(preselectedService = service))
                                                },
                                                onBookSelectedServices = {
                                                    val chosen = allServices.filter { selectedServiceIds.contains(it.id) }
                                                    viewModel.navigateTo(AppScreen.BookingFlow(preselectedService = chosen.firstOrNull()))
                                                }
                                            )
                                        }

                                        AppTab.EMERGENCY -> {
                                            EmergencyScreen(
                                                userFuelType = selectedBike?.fuelType ?: FuelType.PETROL,
                                                providers = approvedProviders,
                                                onCallRequested = { name, phone ->
                                                    viewModel.callDialogProvider = Pair(name, phone)
                                                },
                                                onOpenMapWithFilter = { filter ->
                                                    viewModel.selectedMapPoiId = null
                                                    viewModel.navigateTo(AppScreen.MapNavigation)
                                                }
                                            )
                                        }

                                        AppTab.PARTS -> {
                                            SparePartsScreen(
                                                parts = allParts,
                                                cartItems = cartItems,
                                                selectedBike = selectedBike,
                                                onAddToCart = { viewModel.addToCart(it) },
                                                onViewCart = { viewModel.navigateTo(AppScreen.CartCheckout) }
                                            )
                                        }

                                        AppTab.PROFILE -> {
                                            ProfileScreen(
                                                user = currentUser,
                                                selectedBike = selectedBike,
                                                isDarkMode = isDarkMode,
                                                syncState = syncState,
                                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                                onTriggerSync = { viewModel.triggerCloudSync() },
                                                onViewBookings = { viewModel.navigateTo(AppScreen.ServiceHistory) },
                                                onViewServiceHistory = { viewModel.navigateTo(AppScreen.ServiceHistory) },
                                                onViewCustomization = { viewModel.navigateTo(AppScreen.CustomizationStudio) },
                                                onSwitchRole = { viewModel.switchRole(it) },
                                                onOpenProviderDashboard = { viewModel.navigateTo(AppScreen.ProviderDashboard) },
                                                onOpenAdminDashboard = { viewModel.navigateTo(AppScreen.AdminDashboard) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is AppScreen.MapNavigation -> {
                            OfflineMapCanvas(
                                userFuelType = selectedBike?.fuelType ?: FuelType.PETROL,
                                targetPoiId = viewModel.selectedMapPoiId,
                                onCallRequested = { name, phone ->
                                    viewModel.callDialogProvider = Pair(name, phone)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.BookingFlow -> {
                            val preselectedList = screen.preselectedService?.let { listOf(it) }
                                ?: allServices.filter { selectedServiceIds.contains(it.id) }

                            BookingFlowScreen(
                                initialService = preselectedList.firstOrNull(),
                                initialProvider = screen.preselectedProvider,
                                allServices = allServices,
                                allBikes = allBikes,
                                providers = approvedProviders,
                                selectedBike = selectedBike,
                                onBookingConfirmed = { prov, bike, services, date, time, notes ->
                                    viewModel.submitBooking(prov, bike, services, date, time, notes)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.BookingConfirmation -> {
                            BookingConfirmationScreen(
                                booking = screen.booking,
                                onViewBookings = { viewModel.navigateTo(AppScreen.ServiceHistory) },
                                onDone = { viewModel.navigateTo(AppScreen.Main) }
                            )
                        }

                        is AppScreen.CustomizationStudio -> {
                            CustomizationScreen(
                                bike = selectedBike,
                                onBack = { viewModel.navigateBack() },
                                onBookCustomFitting = { cost, mods ->
                                    val customService = allServices.find { it.category == "Customization" }
                                    viewModel.navigateTo(AppScreen.BookingFlow(preselectedService = customService))
                                }
                            )
                        }

                        is AppScreen.AssistantChat -> {
                            ChatbotScreen(
                                messages = chatMessages,
                                isTyping = isChatTyping,
                                onSendMessage = { text, imageUri -> viewModel.sendChatMessage(text, imageUri) },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.ProviderDashboard -> {
                            ProviderDashboardScreen(
                                provider = approvedProviders.firstOrNull(),
                                bookings = allBookings,
                                onUpdateStatus = { id, status -> viewModel.updateBookingStatus(id, status) },
                                onToggleAvailability = { current ->
                                    approvedProviders.firstOrNull()?.let {
                                        viewModel.toggleProviderOpenStatus(it.id, current)
                                    }
                                },
                                onCallCustomer = { name, phone ->
                                    viewModel.callDialogProvider = Pair(name, phone)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.AdminDashboard -> {
                            AdminDashboardScreen(
                                providers = allProviders,
                                bookings = allBookings,
                                onApproveProvider = { id, approve -> viewModel.approveProvider(id, approve) },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.ServiceHistory -> {
                            ServiceHistoryScreen(
                                bookings = userBookings,
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.Notifications -> {
                            NotificationsScreen(
                                notifications = notifications,
                                onMarkRead = { viewModel.markNotificationRead(it) },
                                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                onBack = { viewModel.navigateBack() }
                            )
                        }

                        is AppScreen.CartCheckout -> {
                            CartScreen(
                                items = cartItems,
                                onRemoveItem = { viewModel.removeFromCart(it) },
                                onCheckout = {
                                    viewModel.checkoutCart()
                                    viewModel.navigateTo(AppScreen.ServiceHistory)
                                },
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                    }

                    // Global Call Confirmation Dialog
                    val currentCall = viewModel.callDialogProvider
                    if (currentCall != null) {
                        EmergencyCallDialog(
                            providerName = currentCall.first,
                            phoneNumber = currentCall.second,
                            onDismiss = { viewModel.callDialogProvider = null }
                        )
                    }

                    // Global Add Bike Sheet
                    if (viewModel.showAddBikeSheet) {
                        AddBikeBottomSheet(
                            onDismiss = { viewModel.showAddBikeSheet = false },
                            onBikeAdded = { newBike ->
                                viewModel.addBike(newBike)
                            }
                        )
                    }
                }
            }
        }
    }
}
