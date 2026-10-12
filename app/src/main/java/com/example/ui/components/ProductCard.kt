package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDiscountBg
import com.example.ui.theme.GreenShopDiscountRed
import com.example.ui.theme.GreenShopFastDelivery
import com.example.ui.theme.GreenShopFastDeliveryBg
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProductCard(
    product: Product,
    quantity: Int,
    isFavorite: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(18.dp))
            .clickable(onClick = onProductClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Visual Product Container with Delivery Badge & Favorite
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f)),
                contentAlignment = Alignment.Center
            ) {
                ProductPhoto(product = product, modifier = Modifier.fillMaxSize())

                // Top-left badge (e.g. "Bestseller", "40% OFF")
                if (product.badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (product.badge.contains("%")) GreenShopDiscountBg else GreenShopDarkGreen
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.badge.contains("%")) GreenShopDiscountRed else GreenShopWhite
                        )
                    }
                }

                // Top-right Favorite Heart
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .testTag("fav_button_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE53935) else GreenShopMutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Availability comes from the product model; do not invent a delivery ETA.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (product.inStock) GreenShopFastDeliveryBg else GreenShopDiscountBg)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product.inStock) "IN STOCK" else "OUT OF STOCK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.inStock) GreenShopFastDelivery else GreenShopDiscountRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Unit/Weight pill
            Text(
                text = product.unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenShopMutedText,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Product Title
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price Row + Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.originalPrice > product.price) {
                        Text(
                            text = "₹${product.originalPrice.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = GreenShopMutedText,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                // Green ADD Button or Quantity Counter
                AnimatedContent(
                    targetState = quantity,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "add_counter"
                ) { count ->
                    if (count == 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, GreenShopPrimary, RoundedCornerShape(8.dp))
                                .background(GreenShopMintSurface)
                                .clickable(enabled = product.inStock, onClick = onAdd)
                                .height(44.dp)
                                .padding(horizontal = 16.dp)
                                .testTag("product_add_button_${product.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (product.inStock) "ADD" else "OUT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenShopPrimary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenShopPrimary)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onRemove)
                                    .testTag("product_remove_button_${product.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = GreenShopWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "$count",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenShopWhite,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onAdd)
                                    .testTag("product_inc_button_${product.id}"),
                                contentAlignment = Alignment.Center
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
            }
        }
    }
}
