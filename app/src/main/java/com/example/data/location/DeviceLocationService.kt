package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

class DeviceLocationService(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): UserLocation? =
        suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location: Location? ->
                    continuation.resume(location?.let { UserLocation(it.latitude, it.longitude) })
                }
                .addOnFailureListener { continuation.resume(null) }
        }
}
