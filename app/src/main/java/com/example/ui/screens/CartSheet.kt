package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CartItem
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
fun CartSheet(
    viewModel: GreenShopViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()

    var selectedTip by remember { mutableStateOf(0.0) }
    var useWallet by remember { mutableStateOf(true) }
    var showAddressPicker by remember { mutableStateOf(false) }

    if (showAddressPicker) {
        AddressMapPicker(
            initialLatitude = selectedAddress.latitude ?: 12.9716,
            initialLongitude = selectedAddress.longitude ?: 77.5946,
            onBack = { showAddressPicker = false },
            onConfirm = { addressLine, locality, latitude, longitude ->
                viewModel.addNewAddress(
                    title = "Home",
                    line = addressLine,
                    locality = locality,
                    latitude = latitude,
                    longitude = longitude
                )
                showAddressPicker = false
            }
        )
        return
    }

    // Only the item subtotal is authoritative while server-side checkout is unavailable.
    // Delivery fees, discounts and the final payable amount must come from a checkout quote.
    val displayedSubtotal = cartTotal + selectedTip

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GreenShopMintSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Cart",
                        tint = GreenShopDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Your Green Basket",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Delivery estimate shown at checkout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenShopPrimary
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = GreenShopDarkText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🛒", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your cart is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add fresh groceries and produce to get started!",
                        fontSize = 12.sp,
                        color = GreenShopMutedText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Address Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddressPicker = true }
                            .testTag("checkout_address_map_picker"),
                        colors = CardDefaults.cardColors(containerColor = GreenShopMintSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Delivery Address",
                                tint = GreenShopDarkGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Delivering to ${selectedAddress.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GreenShopDarkGreen
                                )
                                Text(
                                    text = "${selectedAddress.addressLine}, ${selectedAddress.locality}",
                                    fontSize = 11.sp,
                                    color = GreenShopDarkText
                                )
                            }
                        }
                    }
                }

                // Item Rows
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onAdd = { viewModel.addToCart(item.product) },
                        onRemove = { viewModel.removeFromCart(item.product.id) }
                    )
                }

                // Tip Rider Section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tip your delivery partner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "100% of the tip goes to your delivery rider",
                        fontSize = 11.sp,
                        color = GreenShopMutedText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0 to "None", 10.0 to "₹10", 20.0 to "₹20", 30.0 to "₹30").forEach { (amt, label) ->
                            val isSelected = selectedTip == amt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GreenShopMintSurface else Color(0xFFF5F5F5))
                                    .border(
                                        1.dp,
                                        if (isSelected) GreenShopPrimary else GreenShopLightBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTip = amt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) GreenShopPrimary else GreenShopDarkText
                                )
                            }
                        }
                    }
                }

                // Pay using Green Shop Money Balance Option
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenShopYellowLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { useWallet = !useWallet }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = useWallet && walletBalance > 0,
                                onCheckedChange = { useWallet = it },
                                colors = CheckboxDefaults.colors(checkedColor = GreenShopPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pay with THE GREEN SHOP Money",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GreenShopDarkText
                                )
                                Text(
                                    text = "Balance: ₹${walletBalance.toInt()} • Instant 1-tap checkout",
                                    fontSize = 11.sp,
                                    color = GreenShopMutedText
                                )
                            }
                            Text(
                                text = "⚡",
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // Bill Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Bill Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            BillRow("Items Total", "₹${cartTotal.toInt()}")
                            if (selectedTip > 0) {
                                BillRow("Rider Tip", "₹${selectedTip.toInt()}")
                            }
                            Text(
                                text = "Delivery fees, discounts and taxes are calculated securely by the server at checkout.",
                                fontSize = 11.sp,
                                color = GreenShopMutedText
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Current subtotal",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "₹${displayedSubtotal.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = GreenShopDarkGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Place Order Button
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_place_order_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${displayedSubtotal.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GreenShopWhite
                        )
                        Text(
                            text = "TOTAL",
                            fontSize = 9.sp,
                            color = GreenShopWhite.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Checkout needs server connection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GreenShopWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenShopMintSurface),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.components.ProductPhoto(item.product, Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = "${item.product.unit} • ₹${item.product.price.toInt()}",
                    fontSize = 11.sp,
                    color = GreenShopMutedText
                )
            }
        }

        // Qty Counter
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GreenShopPrimary)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = GreenShopWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "${item.quantity}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = GreenShopWhite
            )
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = GreenShopWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun BillRow(label: String, value: String, isGreen: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = GreenShopMutedText)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isGreen) GreenShopDarkGreen else GreenShopDarkText
        )
    }
}
