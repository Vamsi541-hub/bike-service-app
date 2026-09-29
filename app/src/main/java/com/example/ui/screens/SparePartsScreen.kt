package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bike
import com.example.data.model.CartItem
import com.example.data.model.SparePart
import com.example.ui.theme.*

@Composable
fun SparePartsScreen(
    parts: List<SparePart>,
    cartItems: List<CartItem>,
    selectedBike: Bike?,
    onAddToCart: (SparePart) -> Unit,
    onViewCart: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var filterByCompatibleBikeOnly by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            "All",
            "Engine Parts",
            "Brake Parts",
            "Tyres",
            "Batteries",
            "Chain & Sprocket",
            "Lights",
            "Mirrors",
            "Filters",
            "Electrical Parts",
            "Accessories"
        )
    }

    val bikeModelName = selectedBike?.model ?: ""

    val filteredParts = remember(parts, searchQuery, selectedCategory, filterByCompatibleBikeOnly, bikeModelName) {
        parts.filter { part ->
            val matchesCategory = selectedCategory == "All" || part.category == selectedCategory
            val matchesSearch = part.name.contains(searchQuery, ignoreCase = true) ||
                    part.category.contains(searchQuery, ignoreCase = true) ||
                    part.compatibleBikes.contains(searchQuery, ignoreCase = true)
            val matchesBike = if (filterByCompatibleBikeOnly && bikeModelName.isNotBlank()) {
                part.compatibleBikes.contains("Universal", ignoreCase = true) ||
                        part.compatibleBikes.contains(bikeModelName, ignoreCase = true)
            } else {
                true
            }
            matchesCategory && matchesSearch && matchesBike
        }
    }

    val cartCount = cartItems.sumOf { it.quantity }
    val cartTotal = cartItems.sumOf { it.price * it.quantity }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = if (cartCount > 0) 140.dp else 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OEM Spare Parts Store",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Genuine replacement components & riding accessories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Cart icon
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = BikePrimary) {
                                    Text("$cartCount")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onViewCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = BikePrimary)
                        }
                    }
                }
            }

            // Bike Compatibility Quick Filter Toggle
            if (selectedBike != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (filterByCompatibleBikeOnly) BikePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (filterByCompatibleBikeOnly) androidx.compose.foundation.BorderStroke(1.dp, BikePrimary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = BikePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Filter for ${selectedBike.brand} ${selectedBike.model}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Display only parts guaranteed to fit your bike",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = filterByCompatibleBikeOnly,
                                onCheckedChange = { filterByCompatibleBikeOnly = it }
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search spark plugs, oils, tyres, brake pads...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Category Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Products List
            items(filteredParts) { part ->
                ProductPartCard(
                    part = part,
                    selectedBikeModel = selectedBike?.model,
                    onAddToCart = { onAddToCart(part) }
                )
            }
        }

        // Cart Floating Bottom Bar
        if (cartCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BikePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "$cartCount items in cart", fontWeight = FontWeight.Bold)
                            Text(text = "Total: ₹$cartTotal", color = BikePrimary, fontWeight = FontWeight.Black)
                        }
                    }

                    Button(
                        onClick = onViewCart,
                        colors = ButtonDefaults.buttonColors(containerColor = BikePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Cart", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPartCard(
    part: SparePart,
    selectedBikeModel: String?,
    onAddToCart: () -> Unit
) {
    val isDirectlyCompatible = remember(part, selectedBikeModel) {
        if (selectedBikeModel == null) false
        else part.compatibleBikes.contains("Universal", ignoreCase = true) ||
                part.compatibleBikes.contains(selectedBikeModel, ignoreCase = true)
    }

    var addedToCartRecently by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(BikePrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPartIcon(part.category),
                            contentDescription = part.name,
                            tint = BikePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = part.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = FuelYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${part.rating}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = part.category, style = MaterialTheme.typography.labelSmall, color = BikeSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compatibility Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDirectlyCompatible) BikeAccentGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDirectlyCompatible) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDirectlyCompatible) BikeAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDirectlyCompatible) "Directly fits your bike" else "Fits: ${part.compatibleBikes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDirectlyCompatible) BikeAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isDirectlyCompatible) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = part.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Price (inc. taxes)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₹${part.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = {
                        onAddToCart()
                        addedToCartRecently = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (addedToCartRecently) BikeAccentGreen else BikePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (addedToCartRecently) Icons.Default.Check else Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (addedToCartRecently) "Added!" else "Add to Cart")
                }
            }
        }
    }
}

fun getPartIcon(category: String): ImageVector {
    return when (category) {
        "Engine Parts" -> Icons.Default.Engineering
        "Brake Parts" -> Icons.Default.Settings
        "Tyres" -> Icons.Default.TireRepair
        "Batteries" -> Icons.Default.BatteryChargingFull
        "Chain & Sprocket" -> Icons.Default.Link
        "Lights" -> Icons.Default.Highlight
        "Mirrors" -> Icons.Default.Flip
        "Filters" -> Icons.Default.FilterVintage
        "Electrical Parts" -> Icons.Default.ElectricBolt
        "Accessories" -> Icons.Default.AutoFixHigh
        else -> Icons.Default.Build
    }
}
