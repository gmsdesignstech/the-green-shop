package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.admin.AdminApiException
import com.example.data.admin.AdminDashboard
import com.example.data.admin.AdminRepository
import com.example.data.admin.AdminRow
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopPrimary

private data class AdminDestination(val name: String, val icon: ImageVector, val available: Boolean = true)
private val destinations = listOf(
    AdminDestination("Dashboard", Icons.Rounded.Dashboard), AdminDestination("Orders", Icons.Rounded.ReceiptLong),
    AdminDestination("Products", Icons.Rounded.Inventory2), AdminDestination("Inventory", Icons.Rounded.Warehouse),
    AdminDestination("Delivery partners", Icons.Rounded.DeliveryDining), AdminDestination("Payments", Icons.Rounded.Payments),
    AdminDestination("Categories", Icons.Rounded.Category), AdminDestination("Promotions", Icons.Rounded.LocalOffer),
    AdminDestination("Reports", Icons.Rounded.Analytics), AdminDestination("Notifications", Icons.Rounded.Notifications),
    AdminDestination("Support", Icons.Rounded.SupportAgent), AdminDestination("Store settings", Icons.Rounded.Settings),
    AdminDestination("Audit logs", Icons.Rounded.Security), AdminDestination("Customers", Icons.Rounded.People)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(tokenProvider: suspend (Boolean) -> Result<String>, onSignOut: () -> Unit, repository: AdminRepository = remember { AdminRepository() }) {
    var selected by rememberSaveable { mutableStateOf("Dashboard") }
    var search by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dashboard by remember { mutableStateOf<AdminDashboard?>(null) }
    var rows by remember { mutableStateOf<List<AdminRow>>(emptyList()) }
    var customerAction by remember { mutableStateOf<AdminRow?>(null) }
    var partnerAction by remember { mutableStateOf<AdminRow?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = null
        val firstToken = tokenProvider(false).getOrElse { error = adminErrorMessage(it); loading = false; return }
        suspend fun request(token: String) = if (selected == "Dashboard") repository.dashboard(token).map { dashboard = it; emptyList() }
        else repository.list(selected, token, search)
        var result = request(firstToken)
        if (result.exceptionOrNull() is com.example.data.admin.AdminApiException &&
            (result.exceptionOrNull() as com.example.data.admin.AdminApiException).statusCode == 401
        ) {
            val refreshed = tokenProvider(true)
            result = refreshed.getOrNull()?.let { request(it) }
                ?: Result.failure(refreshed.exceptionOrNull() ?: IllegalStateException("Admin session refresh failed"))
        }
        result.fold(
            onSuccess = { if (selected != "Dashboard") rows = it },
            onFailure = { error = adminErrorMessage(it) }
        )
        loading = false
    }
    LaunchedEffect(selected) { search = ""; load() }
    LaunchedEffect(search) { if (selected != "Dashboard") { delay(350); load() } }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(modifier = Modifier.width(310.dp)) {
            BrandHeader()
            destinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Column { Text(destination.name); if (!destination.available) Text("Not configured", style = MaterialTheme.typography.labelSmall) } },
                    selected = selected == destination.name,
                    icon = { Icon(destination.icon, null) },
                    onClick = { if (destination.available) { selected = destination.name; scope.launch { drawerState.close() } } },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            HorizontalDivider()
            NavigationDrawerItem(label = { Text("Sign out") }, selected = false, icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) }, onClick = onSignOut)
        }
    }) {
        Scaffold(topBar = {
            TopAppBar(title = { Column { Text(selected, fontWeight = FontWeight.Bold); Text("Freshness At Your Doorstep", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Rounded.Menu, "Open admin navigation") } },
                actions = { IconButton(onClick = { loading = true }) { Icon(Icons.Rounded.Refresh, "Refresh") } })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)).padding(horizontal = 16.dp)) {
                if (selected != "Dashboard") OutlinedTextField(search, { search = it }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Search ${selected.lowercase()}") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(14.dp))
                when {
                    loading -> LoadingState()
                    error != null -> ErrorState(error!!) { loading = true }
                    selected == "Dashboard" -> DashboardContent(dashboard)
                    rows.isEmpty() -> EmptyState(selected)
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)) { items(rows, key = { it.id }) { row ->
                        val action = when (selected) {
                            "Customers" -> ({ customerAction = row })
                            "Delivery partners" -> ({ partnerAction = row })
                            else -> null
                        }
                        AdminRowCard(row, action)
                    } }
                }
            }
        }
    }
    if (loading) LaunchedEffect(selected, search, loading) { load() }
    customerAction?.let { customer ->
        CustomerStatusDialog(
            customer = customer,
            busy = busy,
            onDismiss = { customerAction = null },
            onConfirm = { nextStatus, reason ->
                scope.launch {
                    busy = true
                    val token = tokenProvider(false).getOrElse { error = adminErrorMessage(it); busy = false; return@launch }
                    repository.changeCustomerStatus(token, customer.id, nextStatus, reason).fold(
                        onSuccess = { customerAction = null; load() },
                        onFailure = { error = adminErrorMessage(it) }
                    )
                    busy = false
                }
            }
        )
    }
    partnerAction?.let { partner ->
        PartnerReviewDialog(
            partner = partner,
            busy = busy,
            onDismiss = { partnerAction = null },
            onConfirm = { status, reason ->
                scope.launch {
                    busy = true
                    val token = tokenProvider(false).getOrElse { error = adminErrorMessage(it); busy = false; return@launch }
                    repository.reviewDeliveryPartner(token, partner.id, status, reason).fold(
                        onSuccess = { partnerAction = null; load() },
                        onFailure = { error = adminErrorMessage(it) }
                    )
                    busy = false
                }
            }
        )
    }
}

@Composable private fun BrandHeader() = Column(
    Modifier.fillMaxWidth().background(
        Brush.linearGradient(listOf(GreenShopDarkGreen, GreenShopPrimary))
    ).padding(horizontal = 24.dp, vertical = 28.dp)
) {
    Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(10.dp)) {
        Text("GS", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(14.dp))
    Text("THE GREEN SHOP", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
    Text("Operations command centre", color = Color.White.copy(alpha=.78f), style = MaterialTheme.typography.bodySmall)
}
@Composable
private fun DashboardContent(data: AdminDashboard?) {
    if (data == null) {
        EmptyState("dashboard data")
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
                    Brush.linearGradient(listOf(GreenShopDarkGreen, GreenShopPrimary))
                ).padding(22.dp)
            ) {
                Column {
                    Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(50)) {
                        Text("LIVE OPERATIONS", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Business at a glance", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Orders, customers and inventory from the secure backend.", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (data.unavailableSections.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Some dashboard sections could not be loaded. Check the API/database logs and retry.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            data.metrics.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { metric ->
                        Card(
                            Modifier.weight(1f).padding(bottom = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(GreenShopMintSurface), contentAlignment = Alignment.Center) {
                                    Icon(metricIcon(metric.label), null, tint = GreenShopPrimary, modifier = Modifier.size(19.dp))
                                }
                                Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                Text(metric.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Text("Recent orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if ("recentOrders" in data.unavailableSections) {
            item { Text("Recent orders are unavailable. Retry after checking the API/database connection.", color = MaterialTheme.colorScheme.error) }
        } else if (data.recentOrders.isEmpty()) {
            item { Text("No orders in this period", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.recentOrders, key = { "order-${it.id}" }) { AdminRowCard(it) }
        }
        item { Text("Needs restocking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if ("lowStock" in data.unavailableSections) {
            item { Text("Low-stock products are unavailable. Retry after checking the API/database connection.", color = MaterialTheme.colorScheme.error) }
        } else if (data.lowStock.isEmpty()) {
            item { Text("No low-stock products", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.lowStock, key = { "stock-${it.id}" }) { AdminRowCard(it) }
        }
    }
}

private fun metricIcon(label: String): ImageVector = when {
    label.contains("order", true) -> Icons.Rounded.ReceiptLong
    label.contains("customer", true) -> Icons.Rounded.People
    label.contains("partner", true) || label.contains("delivery", true) -> Icons.Rounded.DeliveryDining
    label.contains("revenue", true) || label.contains("sales", true) || label.contains("payment", true) -> Icons.Rounded.CurrencyRupee
    label.contains("stock", true) || label.contains("product", true) -> Icons.Rounded.Inventory2
    else -> Icons.Rounded.Analytics
}

private fun adminErrorMessage(error: Throwable): String = when (error) {
    is AdminApiException -> when (error.statusCode) {
        401 -> "Your admin session expired. Sign in again."
        403 -> "Administrator access is required."
        500 -> "The admin API failed while loading the dashboard. Check NestJS logs and database TLS configuration, then retry."
        503 -> "The admin service is temporarily unavailable. Check database health, then retry."
        else -> error.message ?: "The admin request failed (${error.statusCode})."
    }
    is IOException -> "Couldn't reach the admin API. Check the device network and API_BASE_URL, then retry."
    else -> error.message ?: "The admin request failed. Retry."
}
@Composable private fun AdminRowCard(row:AdminRow,onClick:(()->Unit)?=null)=ElevatedCard(Modifier.fillMaxWidth().then(if(onClick!=null)Modifier.clickable(onClick=onClick) else Modifier),shape=RoundedCornerShape(16.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(row.title,fontWeight=FontWeight.SemiBold);Text(row.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};row.status?.takeIf{it.isNotBlank()}?.let{AssistChip(onClick={onClick?.invoke()},label={Text(it.replace('_',' ').uppercase(),style=MaterialTheme.typography.labelSmall)})}}}
@Composable private fun CustomerStatusDialog(customer:AdminRow,busy:Boolean,onDismiss:()->Unit,onConfirm:(String,String)->Unit){
    val next=if(customer.status.equals("suspended",true))"active" else "suspended"
    var reason by remember(customer.id){mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(next=="active")"Reactivate customer" else "Suspend customer")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("${customer.title} will be marked $next. This administrative action is recorded in the audit log.");OutlinedTextField(reason,{reason=it.take(500)},label={Text("Reason")},minLines=2,modifier=Modifier.fillMaxWidth())}},confirmButton={Button(onClick={onConfirm(next,reason.trim())},enabled=!busy&&reason.trim().length>=3){Text(if(next=="active")"Reactivate" else "Suspend")}},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancel")}})
}
@Composable private fun PartnerReviewDialog(partner:AdminRow,busy:Boolean,onDismiss:()->Unit,onConfirm:(String,String)->Unit){
    val statuses = listOf("under_review", "approved", "rejected", "suspended")
    var selected by remember(partner.id){mutableStateOf(when(partner.status?.lowercase()){"approved"->"suspended";"suspended"->"approved";else->"approved"})}
    var reason by remember(partner.id){mutableStateOf("")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Review delivery partner")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(partner.title,fontWeight=FontWeight.Bold)
            Text(partner.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            statuses.forEach{status->Row(Modifier.fillMaxWidth().clickable{selected=status}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected==status,{selected=status});Text(status.replace('_',' ').replaceFirstChar(Char::uppercase))}}
            OutlinedTextField(reason,{reason=it.take(1000)},label={Text("Decision reason")},minLines=2,modifier=Modifier.fillMaxWidth())
            Text("The decision and administrator identity are stored in the audit log.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }},
        confirmButton={Button(onClick={onConfirm(selected,reason.trim())},enabled=!busy&&reason.trim().length>=3){Text("Save decision")}},
        dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancel")}}
    )
}
@Composable private fun LoadingState()=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(12.dp));Text("Loading live operations…")}
@Composable private fun ErrorState(message:String,retry:()->Unit)=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CloudOff,null,tint=MaterialTheme.colorScheme.error);Text(message,modifier=Modifier.padding(16.dp));Button(onClick=retry){Text("Retry")}}
@Composable private fun EmptyState(name:String)=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Inbox,null,tint=MaterialTheme.colorScheme.primary);Text("No ${name.lowercase()} found",modifier=Modifier.padding(12.dp),fontWeight=FontWeight.SemiBold);Text("New records will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
