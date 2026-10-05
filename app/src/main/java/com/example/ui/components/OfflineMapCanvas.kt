package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FuelType
import com.example.data.location.NearbyPlace
import com.example.data.location.UserLocation
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

data class MapPoi(
    val id: String,
    val title: String,
    val category: String, // "MECHANIC", "PETROL", "EV", "PUNCTURE"
    val x: Float, // relative coords (0..1000)
    val y: Float,
    val distanceStr: String,
    val phone: String,
    val isOpen: Boolean = true
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun OfflineMapCanvas(
    userFuelType: FuelType = FuelType.PETROL,
    targetPoiId: String? = null,
    userLocation: UserLocation? = null,
    nearbyPlaces: List<NearbyPlace> = emptyList(),
    onCallRequested: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    // Live OpenStreetMap POIs projected around the real device location.
    val allPois = remember(nearbyPlaces, userLocation, userFuelType) {
        nearbyPlaces.map { place ->
            val meters = (place.distanceKm * 1000.0).coerceAtMost(3500.0)
            val results = FloatArray(2)
            if (userLocation != null) {
                android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    place.latitude, place.longitude, results
                )
            }
            val distanceMeters = if (userLocation != null) results[0].toDouble() else meters
            val bearing = if (userLocation != null) results[1].toDouble() else 0.0
            val radius = (distanceMeters / 8.5).toFloat().coerceIn(70f, 420f)
            val angle = Math.toRadians(bearing)
            MapPoi(
                id = place.id,
                title = place.title,
                category = place.category,
                x = 500f + (sin(angle) * radius).toFloat(),
                y = 500f - (cos(angle) * radius).toFloat(),
                distanceStr = String.format(Locale.US, "%.1f km", place.distanceKm),
                phone = place.phone ?: "",
                isOpen = place.isOpen
            )
        }.ifEmpty {
            listOf(
                MapPoi("no_live_results", "No live places found", "MECHANIC", 500f, 500f, "—", "", false)
            )
        }
    }
