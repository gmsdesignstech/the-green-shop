package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GreenShopViewModel

@Composable
fun OrderAgainScreen(
    viewModel: GreenShopViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    val loading by viewModel.ordersLoading.collectAsState()
    val error by viewModel.ordersError.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Order Again",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Quickly reorder your past baskets with 1 tap",
                    fontSize = 13.sp,
                    color = GreenShopMutedText
                )
            }
        }

        if (loading) {
            item { Text("Loading your orders…", color = GreenShopMutedText, modifier = Modifier.padding(vertical = 24.dp)) }
        } else if (error != null) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Text("Return to Home and reopen Orders to retry.", color = GreenShopMutedText, fontSize = 12.sp)
                }
            }
        } else if (orders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No previous orders yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Your completed orders will appear here for fast reordering",
                            fontSize = 12.sp,
                            color = GreenShopMutedText
                        )
                    }
                }
            }
        } else {
            items(orders) { order ->
                OrderHistoryCard(
                    order = order,
                    onReorder = { viewModel.reorder(order) },
                    onTrack = { viewModel.navigateTo(AppScreen.ORDER_TRACKING) },
                    onProductClick = { viewModel.showProductDetail(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onReorder: () -> Unit,
    onTrack: () -> Unit,
    onProductClick: (com.example.model.Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("order_history_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order.placedTime} • ${order.items.size} items",
                        fontSize = 12.sp,
                        color = GreenShopMutedText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenShopMintSurface)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items list thumbnails
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                order.items.take(4).forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F8E9))
                            .clickable { onProductClick(item.product) },
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.ProductPhoto(item.product, Modifier.fillMaxSize())
                    }
                }
                if (order.items.size > 4) {
                    Text(
                        text = "+${order.items.size - 4} more",
                        fontSize = 11.sp,
                        color = GreenShopMutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ₹${order.totalAmount.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = GreenShopDarkText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReorder,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reorder_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reorder",
                            tint = GreenShopWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reorder All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
