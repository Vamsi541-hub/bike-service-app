package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*

class BikeCareTypeConverters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (_: Exception) {
        UserRole.CUSTOMER
    }

    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType = try {
        FuelType.valueOf(value)
    } catch (_: Exception) {
        FuelType.PETROL
    }

    @TypeConverter
    fun fromBookingStatus(value: BookingStatus): String = value.name

    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus = try {
        BookingStatus.valueOf(value)
    } catch (_: Exception) {
        BookingStatus.PENDING
    }
}

@Database(
    entities = [
        User::class,
        Bike::class,
        ServiceItem::class,
        ServiceProvider::class,
        Booking::class,
        SparePart::class,
        CartItem::class,
        AppNotification::class,
        ServiceRecord::class,
        MaintenanceReminder::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(BikeCareTypeConverters::class)
abstract class BikeCareDatabase : RoomDatabase() {
    abstract fun dao(): BikeCareDao

    companion object {
        @Volatile
        private var INSTANCE: BikeCareDatabase? = null

        fun getInstance(context: Context): BikeCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BikeCareDatabase::class.java,
                    "bikecare_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
