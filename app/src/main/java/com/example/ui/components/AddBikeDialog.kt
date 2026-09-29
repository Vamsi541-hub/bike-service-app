package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Bike
import com.example.data.model.FuelType
import com.example.ui.theme.BikePrimary
import com.example.ui.theme.BikeSecondary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBikeBottomSheet(
    onDismiss: () -> Unit,
    onBikeAdded: (Bike) -> Unit
) {
    // Brand -> Models catalog
    val brandModels = remember {
        mapOf(
            "Yamaha" to listOf("MT-15", "R15 V4", "FZ-S", "Aerox 155", "Fascino 125"),
            "Royal Enfield" to listOf("Classic 350", "Hunter 350", "Himalayan 450", "Meteor 350", "Interceptor 650"),
            "KTM" to listOf("Duke 200", "Duke 390", "RC 200", "390 Adventure"),
            "Honda" to listOf("Activa 6G", "Shine 125", "CB350 H'ness", "Hornet 2.0", "CBR650R"),
            "Bajaj" to listOf("Pulsar NS200", "Pulsar 150", "Dominar 400", "Chetak EV"),
            "Ather" to listOf("450X", "450S", "Rizta"),
            "Ola Electric" to listOf("S1 Pro", "S1 X", "Roadster EV"),
            "TVS" to listOf("Apache RTR 160 4V", "Apache RR 310", "Jupiter 125", "iQube EV")
        )
    }

    var selectedBrand by remember { mutableStateOf("Yamaha") }
    val availableModels = brandModels[selectedBrand] ?: listOf("Standard")
    var selectedModel by remember { mutableStateOf(availableModels.first()) }

    // Update selected model when brand changes
    LaunchedEffect(selectedBrand) {
        selectedModel = brandModels[selectedBrand]?.firstOrNull() ?: "Standard"
    }

    val variants = remember { listOf("Standard", "Deluxe", "Sport Edition", "ABS Dual Channel", "Racing Edition") }
    var selectedVariant by remember { mutableStateOf("Standard") }

    val years = remember { (2018..2026).toList().reversed() }
    var selectedYear by remember { mutableIntStateOf(2024) }

    var selectedFuelType by remember {
        mutableStateOf(
            if (selectedBrand == "Ather" || selectedBrand == "Ola Electric" || selectedModel.contains("EV"))
                FuelType.EV
            else
                FuelType.PETROL
        )
    }

    // Auto-switch fuel type if EV brand selected
    LaunchedEffect(selectedBrand, selectedModel) {
        if (selectedBrand == "Ather" || selectedBrand == "Ola Electric" || selectedModel.contains("EV") || selectedModel.contains("Chetak")) {
            selectedFuelType = FuelType.EV
        } else {
            selectedFuelType = FuelType.PETROL
        }
    }

    var registrationPlate by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Add Your Two-Wheeler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your bike specs without typing complicated details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Brand Selection
            Text(
                text = "1. Select Brand",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(brandModels.keys.toList()) { brand ->
                    val isSelected = brand == selectedBrand
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBrand = brand },
                        label = { Text(brand) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Model Selection
            Text(
                text = "2. Select Model",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableModels) { model ->
                    val isSelected = model == selectedModel
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedModel = model },
                        label = { Text(model) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BikePrimary.copy(alpha = 0.2f),
                            selectedLabelColor = BikePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Variant & Fuel Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. Variant",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var expandedVariant by remember { mutableStateOf(false) }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedVariant = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedVariant, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedVariant,
                        onDismissRequest = { expandedVariant = false }
                    ) {
                        variants.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v) },
                                onClick = {
                                    selectedVariant = v
                                    expandedVariant = false
                                }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4. Year",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var expandedYear by remember { mutableStateOf(false) }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedYear = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$selectedYear", style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text("$y") },
                                onClick = {
                                    selectedYear = y
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Fuel Type Selector
            Text(
                text = "5. Fuel / Power Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple(FuelType.PETROL, "⛽ Petrol", Icons.Default.LocalGasStation),
                    Triple(FuelType.EV, "⚡ Electric (EV)", Icons.Default.ElectricBike),
                    Triple(FuelType.HYBRID, "🌱 Hybrid", Icons.Default.Eco)
                ).forEach { (type, label, icon) ->
                    val isSelected = selectedFuelType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BikePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) BikePrimary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedFuelType = type }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) BikePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BikePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Registration Plate (Optional)
            OutlinedTextField(
                value = registrationPlate,
                onValueChange = { registrationPlate = it.uppercase() },
                label = { Text("Registration Number (e.g. KA-05-ER-4092)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add Bike Confirm Button
            Button(
                onClick = {
                    val finalPlate = registrationPlate.ifBlank { "MH-02-BK-${(1000..9999).random()}" }
                    val newBike = Bike(
                        id = "bike_${UUID.randomUUID().toString().take(8)}",
                        userId = "current_user",
                        brand = selectedBrand,
                        model = selectedModel,
                        variant = selectedVariant,
                        year = selectedYear,
                        fuelType = selectedFuelType,
                        registrationNumber = finalPlate,
                        isSelected = true,
                        lastServiceDate = "Recent Inspection",
                        nextServiceDue = "In 90 Days",
                        insuranceExpiry = "Valid for 1 Year",
                        pollutionExpiry = if (selectedFuelType == FuelType.EV) "N/A (Zero Emission)" else "Valid 6 Months"
                    )
                    onBikeAdded(newBike)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Set as Active Bike", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
