package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bike
import com.example.data.model.CustomizationOption
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    bike: Bike?,
    onBack: () -> Unit,
    onBookCustomFitting: (Int, List<String>) -> Unit
) {
    var showBeforeStockView by remember { mutableStateOf(false) }

    // Color accents
    val accentColors = listOf(
        Pair("Cobalt Blue", Color(0xFF0284C7)),
        Pair("Racing Orange", Color(0xFFF97316)),
        Pair("Stealth Black", Color(0xFF1E293B)),
        Pair("Neon Acid", Color(0xFF84CC16)),
        Pair("Crimson Red", Color(0xFFDC2626))
    )
    var selectedAccentColor by remember { mutableStateOf(accentColors.first().second) }

    val options = remember {
        listOf(
            CustomizationOption("c_1", "Exhaust", "Akrapovič Slip-On Carbon Canister", 18500, "Deep bass thumper tone with 2.8kg weight reduction", "exhaust"),
            CustomizationOption("c_2", "Mirrors", "CNC Stealth Bar-End Mirrors", 1499, "Convex anti-glare glass with aerodynamic billet aluminum stems", "mirrors"),
            CustomizationOption("c_3", "Seat", "Touring Ribbed Gel Ergonomic Saddle", 2800, "High density cooling memory foam with diamond cross-stitching", "seat"),
            CustomizationOption("c_4", "Graphics", "Matte Cyberpunk Track Wrap Kit", 3499, "3M laminated scratch-resistant full tank & fairing decal pack", "graphics"),
            CustomizationOption("c_5", "Windshield", "Double Bubble Smoke Tinted Visor", 1250, "Direct wind deflection reducing high-speed rider fatigue", "windshield"),
            CustomizationOption("c_6", "Crash Guards", "Engine Frame Sliders with Delrin Pucks", 2600, "Reinforced roll-cage chassis protection during slide/drop", "crash_guards"),
            CustomizationOption("c_7", "Lights", "Dual Projector Aux Fog Pods 60W", 2200, "Yellow fog penetration + 6500K bright white highway flood", "lights"),
            CustomizationOption("c_8", "Hand Grips", "Anti-Vibration Silicone Grips", 650, "Textured diamond grip minimizing handlebar vibration fatigue", "grips"),
            CustomizationOption("c_9", "Indicators", "Sequential Flowing LED Blinkers", 890, "Dynamic flowing arrow turn signals with flexible rubber stalks", "indicators")
        )
    }

    var selectedCustomizationIds by remember { mutableStateOf(setOf("c_1", "c_2", "c_4")) }

    val selectedItems = options.filter { selectedCustomizationIds.contains(it.id) }
    val totalCost = selectedItems.sumOf { it.price }

    val animatedAccent by animateColorAsState(targetValue = selectedAccentColor, label = "accentColor")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bike Customization Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${bike?.brand ?: "Custom"} ${bike?.model ?: "Bike"} Tuning",
                            style = MaterialTheme.typography.labelSmall,
                            color = BikePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showBeforeStockView = !showBeforeStockView }) {
                        Text(if (showBeforeStockView) "View Tuned" else "View Stock")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${selectedItems.size} custom mods",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹$totalCost",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = BikePrimary
                        )
                    }

                    Button(
                        onClick = { onBookCustomFitting(totalCost, selectedItems.map { it.name }) },
                        colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Book Workshop Fitting", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            // Visual Motorcycle Canvas Preview
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showBeforeStockView) "STOCK OEM PROFILE" else "CUSTOM TUNED PROFILE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (showBeforeStockView) MaterialTheme.colorScheme.onSurfaceVariant else BikeSecondary
                            )

                            AssistChip(
                                onClick = { showBeforeStockView = !showBeforeStockView },
                                label = { Text(if (showBeforeStockView) "Show Tuned" else "Compare Stock") },
                                leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Motorcycle Vector Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0F172A),
                                            Color(0xFF1E293B)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val scale = (w / 380f).coerceAtMost(h / 150f)

                                fun cx(x: Float) = (w - 340f * scale) / 2f + x * scale
                                fun cy(y: Float) = (h - 130f * scale) / 2f + y * scale

                                val activeColor = if (showBeforeStockView) Color(0xFF64748B) else animatedAccent

                                // Ground shadow
                                drawOval(
                                    color = Color(0x66000000),
                                    topLeft = Offset(cx(40f), cy(110f)),
                                    size = androidx.compose.ui.geometry.Size(260f * scale, 16f * scale)
                                )

                                // Rear Wheel
                                drawCircle(
                                    color = Color(0xFF334155),
                                    center = Offset(cx(80f), cy(85f)),
                                    radius = 32f * scale,
                                    style = Stroke(width = 8f * scale)
                                )
                                drawCircle(
                                    color = Color.White,
                                    center = Offset(cx(80f), cy(85f)),
                                    radius = 6f * scale
                                )

                                // Front Wheel
                                drawCircle(
                                    color = Color(0xFF334155),
                                    center = Offset(cx(260f), cy(85f)),
                                    radius = 32f * scale,
                                    style = Stroke(width = 8f * scale)
                                )
                                drawCircle(
                                    color = Color.White,
                                    center = Offset(cx(260f), cy(85f)),
                                    radius = 6f * scale
                                )

                                // Motorcycle Frame
                                val framePath = Path().apply {
                                    moveTo(cx(80f), cy(85f))
                                    lineTo(cx(130f), cy(65f))
                                    lineTo(cx(170f), cy(65f))
                                    lineTo(cx(210f), cy(45f))
                                    lineTo(cx(260f), cy(85f))
                                    lineTo(cx(230f), cy(40f))
                                    lineTo(cx(160f), cy(60f))
                                    close()
                                }
                                drawPath(framePath, color = Color(0xFFE2E8F0))

                                // Fuel Tank & Fairing with Selected Accent Color
                                val tankPath = Path().apply {
                                    moveTo(cx(140f), cy(55f))
                                    quadraticBezierTo(cx(180f), cy(30f), cx(220f), cy(45f))
                                    lineTo(cx(210f), cy(65f))
                                    lineTo(cx(150f), cy(65f))
                                    close()
                                }
                                drawPath(tankPath, color = activeColor)

                                // Seat (Touring or Stock)
                                val seatPath = Path().apply {
                                    moveTo(cx(105f), cy(55f))
                                    lineTo(cx(150f), cy(55f))
                                    lineTo(cx(145f), cy(62f))
                                    lineTo(cx(115f), cy(62f))
                                    close()
                                }
                                drawPath(
                                    seatPath,
                                    color = if (!showBeforeStockView && selectedCustomizationIds.contains("c_3")) Color(0xFF78350F) else Color(0xFF1E293B)
                                )

                                // Custom Exhaust Pipe
                                if (!showBeforeStockView && selectedCustomizationIds.contains("c_1")) {
                                    // Golden / Chrome Slip-on
                                    drawLine(
                                        color = BikeSecondary,
                                        start = Offset(cx(130f), cy(90f)),
                                        end = Offset(cx(50f), cy(75f)),
                                        strokeWidth = 10f * scale,
                                        cap = StrokeCap.Round
                                    )
                                } else {
                                    // Stock black muffler
                                    drawLine(
                                        color = Color(0xFF334155),
                                        start = Offset(cx(130f), cy(90f)),
                                        end = Offset(cx(60f), cy(85f)),
                                        strokeWidth = 8f * scale,
                                        cap = StrokeCap.Round
                                    )
                                }

                                // Custom Windshield / Visor
                                if (!showBeforeStockView && selectedCustomizationIds.contains("c_5")) {
                                    drawLine(
                                        color = Color(0x9938BDF8),
                                        start = Offset(cx(225f), cy(38f)),
                                        end = Offset(cx(215f), cy(20f)),
                                        strokeWidth = 6f * scale,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Theme Accent Swatches
                        Text(
                            text = "Fairing & Decal Accent Color:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            accentColors.forEach { (name, color) ->
                                val isSelected = selectedAccentColor == color
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAccentColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Customization Options List
            item {
                Text(
                    text = "Select Upgrades & Aesthetics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(options) { opt ->
                val isSelected = selectedCustomizationIds.contains(opt.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCustomizationIds = if (isSelected) {
                                selectedCustomizationIds - opt.id
                            } else {
                                selectedCustomizationIds + opt.id
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BikePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BikePrimary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                selectedCustomizationIds = if (isSelected) {
                                    selectedCustomizationIds - opt.id
                                } else {
                                    selectedCustomizationIds + opt.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = opt.category, style = MaterialTheme.typography.labelSmall, color = BikeSecondary, fontWeight = FontWeight.Bold)
                                Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = opt.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(text = opt.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "₹${opt.price}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = BikePrimary
                        )
                    }
                }
            }
        }
    }
}
