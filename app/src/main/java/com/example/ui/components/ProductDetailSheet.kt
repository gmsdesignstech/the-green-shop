package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopDiscountBg
import com.example.ui.theme.GreenShopDiscountRed
import com.example.ui.theme.GreenShopFastDelivery
import com.example.ui.theme.GreenShopFastDeliveryBg
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow

@Composable
fun ProductDetailSheet(
    product: Product,
    currentQuantity: Int,
    isFavorite: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onViewCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Action Buttons (Breadcrumb, Favorite, and Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Breadcrumb Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenShopMintSurface)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product.category == "cat_fruits") "🍎 Fresh Fruits" else "🥦 Fresh Vegetables",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopDarkGreen
                    )
                    Text(
                        text = " • ${product.subcategory}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenShopDarkGreen.copy(alpha = 0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Favorite Toggle Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F3F4))
                            .testTag("detail_fav_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFE53935) else GreenShopMutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F3F4))
                            .testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GreenShopDarkText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ========================================================
            // LARGE HERO IMAGE / PRODUCE ILLUSTRATION CONTAINER
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFE8F5E9),
                                Color(0xFFC8E6C9).copy(alpha = 0.6f),
                                GreenShopMintSurface
                            )
                        )
                    )
                    .border(1.dp, GreenShopLightBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle circular aura
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f))
                )

                ProductPhoto(
                    product = product,
                    modifier = Modifier.fillMaxSize().testTag("detail_large_image")
                )

                // Top-Left Badges (e.g. "Bestseller", "100% Organic", "Farm Fresh")
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (product.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (product.badge.contains("%")) GreenShopDiscountBg else GreenShopDarkGreen
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = product.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (product.badge.contains("%")) GreenShopDiscountRed else GreenShopWhite
                            )
                        }
                    }

                    if (product.isOrganic) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00796B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌱 100% Organic",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenShopWhite
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF388E3C))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌿 Farm Direct",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenShopWhite
                            )
                        }
                    }
                }

                // Bottom-Right Lightning Delivery Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenShopFastDeliveryBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GreenShopFastDelivery,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${product.deliveryMinutes} Mins Delivery",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopFastDelivery
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pack Size / Weight Unit Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pack: ${product.unit}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenShopDarkGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "In stock",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Produce Name
            Text(
                text = product.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp,
                modifier = Modifier.testTag("detail_product_name")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price Row & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = GreenShopDarkText
                    )
                    if (product.originalPrice > product.price) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "₹${product.originalPrice.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = GreenShopMutedText,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GreenShopDiscountBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${product.discountPercent}% OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenShopDiscountRed
                            )
                        }
                    }
                }

                // Star Rating pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF9C4))
                        .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = GreenShopYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${product.rating} (${product.ratingCount})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopDarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Produce Quality Tags Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProduceFeatureChip(icon = "💧", label = "Hydro-Washed")
                ProduceFeatureChip(icon = "🧪", label = "Pesticide Tested")
                ProduceFeatureChip(icon = "❄️", label = "Chilled Storage")
                ProduceFeatureChip(icon = "🥗", label = "Ready to Cook")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================================
            // PRODUCT DESCRIPTION
            // ========================================================
            Text(
                text = "Product Description",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.description,
                fontSize = 13.sp,
                color = GreenShopDarkText.copy(alpha = 0.85f),
                lineHeight = 19.sp,
                modifier = Modifier.testTag("detail_description_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Storage & Freshness Advice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE775))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Storage & Freshness Tips",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF33691E)
                        )
                        Text(
                            text = "Store in a cool dry place or vegetable crisper tray at 4-7°C. Best enjoyed within 3-4 days of delivery.",
                            fontSize = 11.sp,
                            color = GreenShopDarkText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Green Shop Guarantee Card
            Text(
                text = "THE GREEN SHOP Guarantee",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenShopMintSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopPrimary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuaranteeItem("🌅 Morning Harvest", "Picked at 5 AM today from certified partner orchards & farms")
                    GuaranteeItem("✓ Product details", "Review the product information and pack size before adding")
                    GuaranteeItem("❄️ Cold Chain Kept", "Packed chilled so produce arrives fresh, crunchy & nutrient rich")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        HorizontalDivider(color = GreenShopLightBorder)

        // ========================================================
        // ACTION BOTTOM BAR (Quantity Stepper & Add to Cart Button)
        // ========================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quantity Stepper (Minus - Count - Plus)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentQuantity > 0) GreenShopPrimary else Color(0xFFF1F3F4))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onRemove,
                        enabled = currentQuantity > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("detail_decrement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (currentQuantity > 0) GreenShopWhite else GreenShopMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$currentQuantity",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentQuantity > 0) GreenShopWhite else GreenShopDarkText,
                        modifier = Modifier.testTag("detail_quantity_text")
                    )

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("detail_increment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (currentQuantity > 0) GreenShopWhite else GreenShopDarkText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Primary Add to Cart Button
                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_add_to_cart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = GreenShopWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentQuantity == 0) {
                            "Add to Cart • ₹${product.price.toInt()}"
                        } else {
                            "Add More (${currentQuantity} in Cart) • +₹${product.price.toInt()}"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopWhite
                    )
                }
            }

            // Optional "View Cart / Checkout" shortcut when items already in basket
            if (currentQuantity > 0 && onViewCart != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onViewCart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("detail_view_cart_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenShopPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopPrimary)
                ) {
                    Text(
                        text = "View Cart • Total ₹${(product.price * currentQuantity).toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Cart",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProduceFeatureChip(icon: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F8E9))
            .border(1.dp, Color(0xFFDCEDC8), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = GreenShopDarkGreen
        )
    }
}

@Composable
private fun GuaranteeItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GreenShopDarkGreen,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenShopDarkGreen)
            Text(text = desc, fontSize = 11.sp, color = GreenShopDarkText.copy(alpha = 0.8f))
        }
    }
}
