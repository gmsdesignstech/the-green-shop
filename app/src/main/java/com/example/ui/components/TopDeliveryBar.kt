package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AddressItem
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellowBright

@Composable
fun TopDeliveryBar(
    currentAddress: AddressItem,
    walletBalance: Double,
    onAddressClick: () -> Unit,
    onWalletClick: () -> Unit,
    onProfileClick: () -> Unit,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    onClearQuery: () -> Unit = {},
    selectedCategoryScope: String = "ALL",
    onCategoryScopeChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GreenShopDarkGreen)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Row 1: Brand Delivery Time & Address + Right Action Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Green Shop in 10-15 minutes & Address
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAddressClick)
                    .testTag("address_selector_bar")
            ) {
                Text(
                    text = "THE GREEN SHOP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenShopYellowBright,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Delivery time confirmed at checkout",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenShopWhite,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentAddress.title.lowercase().replaceFirstChar { it.uppercase() }}  •  ${currentAddress.addressLine}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenShopWhite.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Address",
                        tint = GreenShopWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Action Icons: Wallet pill + Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wallet balance pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenShopWhite)
                        .border(1.dp, GreenShopWhite.copy(alpha = .18f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onWalletClick)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("top_bar_wallet_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = GreenShopPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹${walletBalance.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopDarkText
                    )
                }

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GreenShopWhite)
                        .clickable(onClick = onProfileClick)
                        .testTag("top_bar_profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.green_shop_logo),
                        contentDescription = "Open profile",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Row 2: Interactive Top Grocery Search Bar Component
        TopGrocerySearchBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            selectedCategoryFilter = selectedCategoryScope,
            onCategoryFilterChange = onCategoryScopeChange
        )
    }
}
