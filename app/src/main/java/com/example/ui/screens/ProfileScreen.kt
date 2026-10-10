package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.auth.AuthenticatedUser
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GreenShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GreenShopViewModel,
    authenticatedUser: AuthenticatedUser?,
    onRefreshIdentity: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMode by viewModel.isDarkMode.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }
    val name = authenticatedUser?.displayName?.takeIf { it.isNotBlank() } ?: "Green Shop customer"
    val phone = authenticatedUser?.verifiedPhone?.let(::formatPhoneForDisplay)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.trim().firstOrNull()?.uppercase() ?: "G", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            phone ?: "Verified phone unavailable — sign in again to refresh",
                            color = if (phone == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("profile_verified_phone")
                        )
                        if (phone == null) {
                            TextButton(onClick = onRefreshIdentity, contentPadding = PaddingValues(0.dp)) {
                                Text("Refresh secure profile")
                            }
                        }
                    }
                    if (phone != null) Icon(Icons.Default.Verified, "Verified phone", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { SectionLabel("YOUR INFORMATION") }
        item {
            ProfileItem(Icons.Default.ShoppingBag, "Your orders", "Order history and tracking") { viewModel.navigateTo(AppScreen.ORDER_AGAIN) }
            ProfileItem(Icons.Default.FavoriteBorder, "Your wishlist", "Saved products on this device") { viewModel.navigateTo(AppScreen.HOME) }
            ProfileItem(Icons.Default.LocationOn, "Delivery address", "Manage the checkout delivery location") { viewModel.navigateTo(AppScreen.HOME) }
        }
        item { SectionLabel("APP PREFERENCES") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dark appearance", fontWeight = FontWeight.SemiBold)
                    Text("Apply across this installation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = darkMode, onCheckedChange = { viewModel.isDarkMode.value = it })
            }
        }
        item { SectionLabel("THE GREEN SHOP") }
        item {
            ProfileItem(Icons.Default.Info, "About", "Version ${BuildConfig.VERSION_NAME}") { }
            ProfileItem(Icons.Default.Logout, "Log out", "Securely clear this account from the app") { confirmSignOut = true }
        }
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text("Log out?") },
        text = { Text("You will need to sign in again to access your account.") },
        confirmButton = { Button(onClick = { confirmSignOut = false; onSignOut() }) { Text("Log out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
    )
}

@Composable private fun SectionLabel(value: String) = Text(
    value, Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp),
    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
)

@Composable private fun ProfileItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun formatPhoneForDisplay(phone: String): String {
    val normalized = phone.trim().replace(Regex("[\\s()-]"), "")
    if (!normalized.matches(Regex("^\\+[1-9]\\d{7,14}$"))) return phone
    if (normalized.startsWith("+91") && normalized.length == 13) {
        val local = normalized.drop(3)
        return "+91 ${local.take(5)} ${local.drop(5)}"
    }
    return normalized
}
