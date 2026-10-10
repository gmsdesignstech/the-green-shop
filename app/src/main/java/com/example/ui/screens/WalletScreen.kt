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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.ui.theme.GreenShopYellowBright
import com.example.ui.theme.GreenShopYellowLight
import com.example.viewmodel.GreenShopViewModel

@Composable
fun WalletScreen(
    viewModel: GreenShopViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val walletBalance by viewModel.walletBalance.collectAsState()
    val transactions by viewModel.walletTransactions.collectAsState()

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showGiftCardDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("500") }
    var giftCardCodeText by remember { mutableStateOf("") }
    var giftCardSuccessMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GreenShopYellowBright,
                        GreenShopYellowLight,
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 700f
                )
            ),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GreenShopWhite)
                        .clickable(onClick = onBack)
                        .testTag("wallet_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GreenShopDarkText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GreenShopWhite)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = GreenShopDarkText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Header: Wallet 3D Graphic & Title (matching Screenshot Image 2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big 3D Wallet Emblem with Rupee sign
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(GreenShopPrimary, GreenShopDarkGreen)
                            )
                        )
                        .border(3.dp, GreenShopYellow, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = GreenShopYellowBright
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "green shop",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenShopDarkText
                )
                Text(
                    text = "MONEY",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = GreenShopDarkText
                )

                // Current Available Balance
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenShopWhite)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Balance: ",
                        fontSize = 13.sp,
                        color = GreenShopMutedText
                    )
                    Text(
                        text = "₹${"%.2f".format(walletBalance)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenShopDarkGreen
                    )
                }
            }
        }

        // 3 Feature Cards (exact text from Screenshot Image 2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WalletFeatureCard(
                    icon = Icons.Default.TouchApp,
                    title = "Single tap payments",
                    description = "Enjoy seamless payments without the wait for OTPs"
                )
                WalletFeatureCard(
                    icon = Icons.Default.Security,
                    title = "Zero failures",
                    description = "Zero payment failures ensure you never miss an order"
                )
                WalletFeatureCard(
                    icon = Icons.Default.ElectricBolt,
                    title = "Real-time refunds",
                    description = "No need to wait for refunds. THE GREEN SHOP Money refunds are instant!"
                )
            }
        }

        // Primary Action: Add Money Button
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Button(
                    onClick = { showAddMoneyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("wallet_add_money_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = "Rupee",
                        tint = GreenShopWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Money",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopWhite
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Claim Gift Card Card (matching screenshot)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showGiftCardDialog = true }
                        .testTag("claim_gift_card_button"),
                    colors = CardDefaults.cardColors(containerColor = GreenShopWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GreenShopMintSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Gift Card",
                                    tint = GreenShopDarkGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Claim Gift Card",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenShopDarkText
                                )
                                Text(
                                    text = "Enter gift card details to claim your gift card",
                                    fontSize = 12.sp,
                                    color = GreenShopMutedText
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Claim",
                            tint = GreenShopMutedText
                        )
                    }
                }
            }
        }

        // Transactions History Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(transactions) { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = GreenShopWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopDarkText
                        )
                        Text(
                            text = "${tx.timeAgo} • ${tx.note}",
                            fontSize = 11.sp,
                            color = GreenShopMutedText
                        )
                    }

                    Text(
                        text = "${if (tx.isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (tx.isCredit) GreenShopDarkGreen else Color(0xFFD32F2F)
                    )
                }
            }
        }

        // Watermark Footer (matching Screenshot Image 2)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enjoy seamless",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC7CBD1).copy(alpha = 0.6f)
                )
                Text(
                    text = "one tap payments",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC7CBD1).copy(alpha = 0.6f)
                )
            }
        }
    }

    // Add Money Dialog
    if (showAddMoneyDialog) {
        AlertDialog(
            onDismissRequest = { showAddMoneyDialog = false },
            title = {
                Text(
                    text = "Add Money to THE GREEN SHOP Money",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Instant 1-tap balance for faster deliveries.",
                        fontSize = 13.sp,
                        color = GreenShopMutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount in ₹") },
                        prefix = { Text("₹ ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(200, 500, 1000).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GreenShopMintSurface)
                                    .clickable { customAmountText = "$preset" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+₹$preset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenShopDarkGreen
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = customAmountText.toDoubleOrNull() ?: 500.0
                        viewModel.addMoneyToWallet(amt)
                        showAddMoneyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                ) {
                    Text("Top Up Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMoneyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Claim Gift Card Dialog
    if (showGiftCardDialog) {
        AlertDialog(
            onDismissRequest = {
                showGiftCardDialog = false
                giftCardSuccessMsg = null
            },
            title = { Text("Claim Gift Card", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter voucher code (e.g. GREEN100) to get ₹100 instant balance.",
                        fontSize = 13.sp,
                        color = GreenShopMutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = giftCardCodeText,
                        onValueChange = { giftCardCodeText = it.uppercase() },
                        label = { Text("Voucher / Gift Card Code") },
                        singleLine = true,
                        placeholder = { Text("GREEN100") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (giftCardSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = giftCardSuccessMsg!!,
                            color = GreenShopDarkGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.claimGiftCard(giftCardCodeText.ifBlank { "GREEN100" })
                        if (success) {
                            giftCardSuccessMsg = "🎉 ₹100 Added to your THE GREEN SHOP Money!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
                ) {
                    Text("Redeem", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiftCardDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun WalletFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEA)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFECB3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenShopWhite)
                    .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = GreenShopDarkGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenShopDarkText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = GreenShopDarkText.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
