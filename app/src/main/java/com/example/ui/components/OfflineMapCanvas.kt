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
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

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
    onCallRequested: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    // List of predefined offline POIs in local sector
    val allPois = remember {
        listOf(
            MapPoi("prov_1", "Apex MotoCare Studio", "MECHANIC", 620f, 420f, "1.2 km", "+91 98451 22334"),
            MapPoi("prov_2", "ProRider Pitstop 24/7", "MECHANIC", 380f, 680f, "2.1 km", "+91 97410 88990"),
            MapPoi("prov_3", "VoltSpeed EV Fast Charger", "EV", 740f, 260f, "1.8 km", "+91 94480 33445"),
            MapPoi("fuel_1", "IndianOil Smart Petrol Pump", "PETROL", 290f, 340f, "800 m", "+91 80255 12345"),
            MapPoi("fuel_2", "HP AutoCare Fuel & Air", "PETROL", 810f, 600f, "2.4 km", "+91 80255 67890"),
            MapPoi("ev_1", "Ather Grid Supercharger", "EV", 480f, 210f, "1.1 km", "+91 80010 99887"),
            MapPoi("punc_1", "QuickFix 24/7 Puncture Station", "PUNCTURE", 440f, 520f, "500 m", "+91 98800 44321")
        )
    }

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedPoi by remember {
        mutableStateOf(allPois.find { it.id == targetPoiId } ?: allPois.first())
    }

    // Interactive Pan & Zoom
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1.0f) }

    // Navigation Simulation State
    var isNavigating by remember { mutableStateOf(targetPoiId != null) }
    var navProgress by remember { mutableFloatStateOf(0f) }
    val currentStepIndex by remember {
        derivedStateOf {
            when {
                navProgress < 0.35f -> 0
                navProgress < 0.75f -> 1
                navProgress < 0.98f -> 2
                else -> 3
            }
        }
    }

    val navSteps = remember(selectedPoi) {
        listOf(
            "Head north on 100ft Ring Road for 400m",
            "Turn right onto 80ft Tech Avenue (in 150m)",
            "Continue straight towards ${selectedPoi.title}",
            "You have arrived at ${selectedPoi.title} on your left"
        )
    }

    // Pulse animation for GPS dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // User GPS origin
    val userOrigin = Offset(500f, 500f)

    // Simulated animated user position along route when navigating
    val currentNavUserPos = remember(navProgress, selectedPoi) {
        val dest = Offset(selectedPoi.x, selectedPoi.y)
        Offset(
            x = userOrigin.x + (dest.x - userOrigin.x) * navProgress,
            y = userOrigin.y + (dest.y - userOrigin.y) * navProgress
        )
    }

    val isDark = MaterialTheme.colorScheme.background == SlateBackgroundDark
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
    ) {
        // --- Vector Map Canvas ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Center mapping (1000x1000 coordinate system mapped to canvas)
            val scaleFactor = (canvasWidth.coerceAtMost(canvasHeight) / 1000f) * scale
            val originX = (canvasWidth - 1000f * scaleFactor) / 2f + offsetX
            val originY = (canvasHeight - 1000f * scaleFactor) / 2f + offsetY

            fun mapX(x: Float) = originX + x * scaleFactor
            fun mapY(y: Float) = originY + y * scaleFactor

            // 1. Draw Grid Roads & Highway Blocks
            val roadColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
            val highwayColor = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
            val arterialColor = if (isDark) Color(0xFF475569) else Color(0xFF64748B)

            // Local block backgrounds
            drawRect(
                color = if (isDark) Color(0xFF131D31) else Color(0xFFF1F5F9),
                topLeft = Offset(mapX(50f), mapY(50f)),
                size = androidx.compose.ui.geometry.Size(900f * scaleFactor, 900f * scaleFactor)
            )

            // Horizontal & Vertical Grid Streets
            listOf(150f, 300f, 450f, 600f, 750f, 900f).forEach { pos ->
                // Horizontal
                drawLine(
                    color = roadColor,
                    start = Offset(mapX(50f), mapY(pos)),
                    end = Offset(mapX(950f), mapY(pos)),
                    strokeWidth = 14f * scaleFactor
                )
                // Vertical
                drawLine(
                    color = roadColor,
                    start = Offset(mapX(pos), mapY(50f)),
                    end = Offset(mapX(pos), mapY(950f)),
                    strokeWidth = 14f * scaleFactor
                )
            }

            // Diagonal Arterial Ring Roads
            drawLine(
                color = highwayColor,
                start = Offset(mapX(50f), mapY(500f)),
                end = Offset(mapX(950f), mapY(500f)),
                strokeWidth = 26f * scaleFactor
            )
            drawLine(
                color = highwayColor,
                start = Offset(mapX(500f), mapY(50f)),
                end = Offset(mapX(500f), mapY(950f)),
                strokeWidth = 26f * scaleFactor
            )
            // Ring Expressway
            drawCircle(
                color = arterialColor,
                center = Offset(mapX(500f), mapY(500f)),
                radius = 320f * scaleFactor,
                style = Stroke(width = 18f * scaleFactor)
            )

            // Road Labels
            drawText(
                textMeasurer = textMeasurer,
                text = "OUTER RING EXPRESSWAY",
                topLeft = Offset(mapX(520f), mapY(505f)),
                style = TextStyle(
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontSize = (10 * scaleFactor).coerceAtLeast(8f).sp,
                    letterSpacing = 2.sp
                )
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "100 FT ROAD",
                topLeft = Offset(mapX(510f), mapY(220f)),
                style = TextStyle(
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontSize = (9 * scaleFactor).coerceAtLeast(8f).sp,
                    letterSpacing = 1.sp
                )
            )

            // 2. Active Route Polyline if navigating or POI selected
            val userScreenPos = Offset(mapX(currentNavUserPos.x), mapY(currentNavUserPos.y))
            val poiScreenPos = Offset(mapX(selectedPoi.x), mapY(selectedPoi.y))

            // Waypoints route path (L-shaped or waypoint through grid)
            val cornerPoint = Offset(mapX(selectedPoi.x), mapY(currentNavUserPos.y))
            val routePath = Path().apply {
                moveTo(userScreenPos.x, userScreenPos.y)
                lineTo(cornerPoint.x, cornerPoint.y)
                lineTo(poiScreenPos.x, poiScreenPos.y)
            }

            // Route casing & glow
            drawPath(
                path = routePath,
                color = BikePrimary.copy(alpha = 0.4f),
                style = Stroke(width = 16f * scaleFactor, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = routePath,
                color = BikePrimaryDark,
                style = Stroke(
                    width = 8f * scaleFactor,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                )
            )

            // 3. Draw POI Pins
            val filteredPois = allPois.filter {
                when (selectedCategoryFilter) {
                    "MECHANIC" -> it.category == "MECHANIC"
                    "PETROL" -> it.category == "PETROL"
                    "EV" -> it.category == "EV"
                    "PUNCTURE" -> it.category == "PUNCTURE"
                    else -> true
                }
            }

            filteredPois.forEach { poi ->
                val px = mapX(poi.x)
                val py = mapY(poi.y)
                val isTarget = poi.id == selectedPoi.id

                val pinColor = when (poi.category) {
                    "MECHANIC" -> BikePrimary
                    "PETROL" -> FuelYellow
                    "EV" -> EvCyan
                    "PUNCTURE" -> BikeSecondary
                    else -> BikeAccentGreen
                }

                // Selection glow
                if (isTarget) {
                    drawCircle(
                        color = pinColor.copy(alpha = 0.35f),
                        center = Offset(px, py),
                        radius = 28f * scaleFactor
                    )
                }

                // Pin circle
                drawCircle(
                    color = pinColor,
                    center = Offset(px, py),
                    radius = if (isTarget) 18f * scaleFactor else 14f * scaleFactor
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(px, py),
                    radius = if (isTarget) 7f * scaleFactor else 5f * scaleFactor
                )

                // Label
                drawText(
                    textMeasurer = textMeasurer,
                    text = poi.title,
                    topLeft = Offset(px - 60f * scaleFactor, py + 18f * scaleFactor),
                    style = TextStyle(
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = (11 * scaleFactor).coerceIn(9f, 13f).sp,
                        background = if (isDark) Color(0xCC0F172A) else Color(0xCCFFFFFF)
                    )
                )
            }

            // 4. Draw Current User GPS Dot with animated Pulse
            drawCircle(
                color = BikePrimaryDark.copy(alpha = pulseAlpha),
                center = userScreenPos,
                radius = pulseRadius * scaleFactor
            )
            drawCircle(
                color = Color.White,
                center = userScreenPos,
                radius = 12f * scaleFactor
            )
            drawCircle(
                color = BikePrimaryDark,
                center = userScreenPos,
                radius = 8f * scaleFactor
            )
            // Direction heading cone
            drawArc(
                color = BikePrimaryDark.copy(alpha = 0.3f),
                startAngle = 230f,
                sweepAngle = 80f,
                useCenter = true,
                topLeft = Offset(userScreenPos.x - 30f * scaleFactor, userScreenPos.y - 30f * scaleFactor),
                size = androidx.compose.ui.geometry.Size(60f * scaleFactor, 60f * scaleFactor)
            )
        }

        // --- Top Controls: Header + Filter Chips ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // App Header Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Map & Navigation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(BikeAccentGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sector 4 Offline Cached (0 KB Data)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Recenter GPS
                    IconButton(onClick = {
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1.0f
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = BikePrimaryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == "ALL",
                    onClick = { selectedCategoryFilter = "ALL" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "MECHANIC",
                    onClick = { selectedCategoryFilter = "MECHANIC" },
                    label = { Text("Mechanics") }
                )
                // Fuel vs EV prioritized
                if (userFuelType == FuelType.PETROL || userFuelType == FuelType.HYBRID) {
                    FilterChip(
                        selected = selectedCategoryFilter == "PETROL",
                        onClick = { selectedCategoryFilter = "PETROL" },
                        label = { Text("Petrol") }
                    )
                }
                if (userFuelType == FuelType.EV) {
                    FilterChip(
                        selected = selectedCategoryFilter == "EV",
                        onClick = { selectedCategoryFilter = "EV" },
                        label = { Text("EV Fast Charge") }
                    )
                }
                FilterChip(
                    selected = selectedCategoryFilter == "PUNCTURE",
                    onClick = { selectedCategoryFilter = "PUNCTURE" },
                    label = { Text("Puncture") }
                )
            }
        }

        // --- Bottom Navigation / POI Detail Sheet ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            if (isNavigating) {
                // Turn-by-Turn Real-Time Navigation HUD
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(BikePrimaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentStepIndex == 0) Icons.Default.TurnRight else Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = BikePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = navSteps[currentStepIndex],
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Destination: ${selectedPoi.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { navProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = BikePrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = BikeSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (navProgress < 0.98f) "38 km/h" else "Arrived",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = BikeAccentGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${((1f - navProgress) * 5).toInt().coerceAtLeast(1)} min remaining",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Row {
                                Button(
                                    onClick = {
                                        if (navProgress < 0.95f) {
                                            navProgress = (navProgress + 0.33f).coerceAtMost(1f)
                                        } else {
                                            navProgress = 0f
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BikeSecondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (navProgress < 0.95f) "Step Ride" else "Restart")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { isNavigating = false },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Exit")
                                }
                            }
                        }
                    }
                }
            } else {
                // POI Info Card with Directions & Call
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedPoi.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${selectedPoi.category} • ${selectedPoi.distanceStr} away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AssistChip(
                                onClick = {},
                                label = { Text("Open Now") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(BikeAccentGreen, CircleShape)
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    isNavigating = true
                                    navProgress = 0f
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate")
                            }

                            OutlinedButton(
                                onClick = { onCallRequested(selectedPoi.title, selectedPoi.phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = BikeAccentGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call")
                            }
                        }
                    }
                }
            }
        }
    }
}

