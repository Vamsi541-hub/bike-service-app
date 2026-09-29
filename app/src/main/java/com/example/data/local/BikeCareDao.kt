package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeCareDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // --- Bikes ---
    @Query("SELECT * FROM bikes WHERE userId = :userId")
    fun getBikesForUser(userId: String): Flow<List<Bike>>

    @Query("SELECT * FROM bikes WHERE isSelected = 1 LIMIT 1")
    fun getSelectedBike(): Flow<Bike?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBike(bike: Bike)

    @Query("UPDATE bikes SET isSelected = 0 WHERE userId = :userId")
    suspend fun clearSelectedBikes(userId: String)

    @Query("UPDATE bikes SET isSelected = 1 WHERE id = :bikeId")
    suspend fun selectBike(bikeId: String)

    @Delete
    suspend fun deleteBike(bike: Bike)

    // --- Service Items ---
    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<ServiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceItem>)

    // --- Service Providers ---
    @Query("SELECT * FROM service_providers ORDER BY rating DESC")
    fun getAllProviders(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE isApproved = 1")
    fun getApprovedProviders(): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ServiceProvider>)

    @Query("UPDATE service_providers SET isApproved = :approved WHERE id = :providerId")
    suspend fun updateProviderApproval(providerId: String, approved: Boolean)

    @Query("UPDATE service_providers SET isOpen = :isOpen WHERE id = :providerId")
    suspend fun updateProviderOpenStatus(providerId: String, isOpen: Boolean)

    // --- Bookings ---
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookingsForUser(userId: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getBookingsForProvider(providerId: String): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)

    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus)

    // --- Spare Parts ---
    @Query("SELECT * FROM spare_parts")
    fun getAllParts(): Flow<List<SparePart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<SparePart>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: SparePart)

    @Delete
    suspend fun deletePart(part: SparePart)

    // --- Cart ---
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun removeCartItem(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()
}
