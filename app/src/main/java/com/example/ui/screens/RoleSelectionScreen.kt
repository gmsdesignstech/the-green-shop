package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.GreenShopLogoBadge

enum class UserRole { CUSTOMER, ADMIN, DELIVERY_PARTNER }

@Composable
fun RoleSelectionScreen(onSelectRole: (UserRole) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .padding(24.dp).testTag("role_selection_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GreenShopLogoBadge(size = 82.dp)
        Spacer(Modifier.height(14.dp))
        Text("THE GREEN SHOP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Select your secure access type", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        AccessButton(Icons.Rounded.ShoppingBasket, "Customer", "Shop fresh fruits and vegetables", "customer_role_card") { onSelectRole(UserRole.CUSTOMER) }
        Spacer(Modifier.height(12.dp))
        AccessButton(Icons.Rounded.AdminPanelSettings, "Administrator", "Authorized store operations only", "admin_role_card") { onSelectRole(UserRole.ADMIN) }
        Spacer(Modifier.height(12.dp))
        AccessButton(Icons.Rounded.DeliveryDining, "Delivery partner", "Approved delivery accounts only", "delivery_role_card") { onSelectRole(UserRole.DELIVERY_PARTNER) }
        Spacer(Modifier.height(20.dp))
        Text("Role permissions are verified by the server after phone authentication.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AccessButton(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp).testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
