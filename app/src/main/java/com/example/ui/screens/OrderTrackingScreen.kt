package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.LocationPermissionRationaleCard
import com.example.ui.components.LocationPermissionRationaleDialog
import com.example.ui.components.LocationPrecisionBanner
import com.example.ui.components.rememberLocationPermissionState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OrderStatus
import com.example.ui.components.GreenDeliveryScooterGraphic
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.ui.theme.GreenShopYellowLight
import com.example.viewmodel.GreenShopViewModel

@Composable
fun OrderTrackingScreen(
    viewModel: GreenShopViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOrder by viewModel.activeOrder.collectAsState()
    val order = activeOrder ?: viewModel.orders.collectAsState().value.firstOrNull()

    val permissionState = rememberLocationPermissionState()
    var showRationaleDialog by remember { mutableStateOf(false) }
    var cardDismissed by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "scooter_bounce")
    val bounceOffset by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("tracking_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GreenShopDarkText
                    )
                }

                Text(
                    text = "Live Order Tracking",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showRationaleDialog = true },
                    modifier = Modifier.testTag("tracking_location_info_button")
                ) {
                    Icon(
                        imageVector = if (permissionState.hasAnyLocationPermission) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                        contentDescription = "Location Access Information",
                        tint = if (permissionState.hasAnyLocationPermission) GreenShopDarkGreen else GreenShopYellow
                    )
                }
            }
        }

        if (order == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active order currently placed.")
                }
            }
        } else {
            // Location Permission Rationale (Adhering to Google's permission best practices)
            if (!permissionState.hasAnyLocationPermission && !cardDismissed) {
                item {
                    LocationPermissionRationaleCard(
                        permissionState = permissionState,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onDismiss = { cardDismissed = true }
                    )
                }
            } else if (permissionState.hasAnyLocationPermission && !permissionState.isPrecise) {
                item {
                    LocationPrecisionBanner(
                        isPrecise = permissionState.isPrecise,
                        onRequestPrecise = { showRationaleDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Hero ETA & Scooter Animation Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GreenShopMintSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenShopPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "⚡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Arriving in ${order.etaMinutes} minutes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = GreenShopDarkGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Order #${order.id} • Fresh harvest on its way",
                            fontSize = 12.sp,
                            color = GreenShopMutedText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scooter courier animation
                        Box(
                            modifier = Modifier
                                .offset(y = bounceOffset.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                        ) {
                            GreenDeliveryScooterGraphic(size = 120.dp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Delivery OTP Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(GreenShopYellowLight)
                                .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Share OTP with rider: ",
                                fontSize = 12.sp,
                                color = GreenShopDarkText
                            )
                            Text(
                                text = order.deliveryOtp,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = GreenShopDarkGreen
                            )
                        }
                    }
                }
            }

            // Rider Contact Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GreenShopMintSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🛵", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = order.riderName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = order.riderVehicle,
                                    fontSize = 11.sp,
                                    color = GreenShopMutedText
                                )
                            }
                        }

                        Button(
                            onClick = { },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = GreenShopWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Delivery Status Timeline
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Delivery Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        val steps = listOf(
                            Triple("Order Placed", "Confirmed with Green Dark Store", true),
                            Triple("Packed Fresh", "Items checked & bagged with care", order.status != OrderStatus.PLACED),
                            Triple("Out for Delivery", "Ramesh is on his green electric scooter", order.status == OrderStatus.ON_THE_WAY || order.status == OrderStatus.DELIVERED),
                            Triple("Delivered", "Delivered to your doorstep", order.status == OrderStatus.DELIVERED)
                        )

                        steps.forEach { (title, desc, isDone) ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) GreenShopPrimary else Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = GreenShopWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDone) GreenShopDarkText else GreenShopMutedText
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = GreenShopMutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Items in this order
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Items in this order (${order.items.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            items(order.items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.ProductPhoto(item.product, Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${item.quantity}x ${item.product.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "₹${(item.product.price * item.quantity).toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showRationaleDialog) {
        LocationPermissionRationaleDialog(
            permissionState = permissionState,
            onDismiss = { showRationaleDialog = false }
        )
    }
}
