package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CartItem
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopWhite

@Composable
fun FloatingCartBar(
    cartItems: List<CartItem>,
    totalCount: Int,
    totalPrice: Double,
    onViewCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = totalCount > 0,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(GreenShopDarkGreen)
                    .clickable(onClick = onViewCartClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("floating_cart_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Cart items thumbnail preview & count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview item icons
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GreenShopWhite.copy(alpha = .12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cartItems.take(2).forEach { item ->
                            ProductPhoto(item.product, Modifier.padding(horizontal = 1.dp).size(22.dp).clip(CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "View basket",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopWhite
                        )
                        Text(
                            text = "$totalCount ${if (totalCount == 1) "Item" else "Items"} • ₹${totalPrice.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenShopWhite.copy(alpha = 0.9f)
                        )
                    }
                }

                // Right: Green Circle with right arrow
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(com.example.ui.theme.GreenShopYellow)
                        .border(1.dp, GreenShopWhite.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Cart",
                        tint = GreenShopDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
