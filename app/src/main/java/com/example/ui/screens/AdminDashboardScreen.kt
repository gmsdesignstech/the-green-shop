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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.admin.AdminDashboard
import com.example.data.admin.AdminRepository
import com.example.data.admin.AdminRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class AdminDestination(val name: String, val icon: ImageVector, val available: Boolean = true)
private val destinations = listOf(
    AdminDestination("Dashboard", Icons.Rounded.Dashboard), AdminDestination("Orders", Icons.Rounded.ReceiptLong),
    AdminDestination("Products", Icons.Rounded.Inventory2), AdminDestination("Inventory", Icons.Rounded.Warehouse),
    AdminDestination("Delivery partners", Icons.Rounded.DeliveryDining), AdminDestination("Payments", Icons.Rounded.Payments),
    AdminDestination("Categories", Icons.Rounded.Category), AdminDestination("Promotions", Icons.Rounded.LocalOffer),
    AdminDestination("Reports", Icons.Rounded.Analytics), AdminDestination("Notifications", Icons.Rounded.Notifications),
    AdminDestination("Support", Icons.Rounded.SupportAgent), AdminDestination("Store settings", Icons.Rounded.Settings),
    AdminDestination("Audit logs", Icons.Rounded.Security), AdminDestination("Customers", Icons.Rounded.People, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(tokenProvider: suspend (Boolean) -> Result<String>, onSignOut: () -> Unit, repository: AdminRepository = remember { AdminRepository() }) {
    var selected by rememberSaveable { mutableStateOf("Dashboard") }
    var search by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dashboard by remember { mutableStateOf<AdminDashboard?>(null) }
    var rows by remember { mutableStateOf<List<AdminRow>>(emptyList()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = null
        val firstToken = tokenProvider(false).getOrElse { error = it.message; loading = false; return }
        suspend fun request(token: String) = if (selected == "Dashboard") repository.dashboard(token).map { dashboard = it; emptyList() }
        else repository.list(selected, token, search)
        var result = request(firstToken)
        if (result.exceptionOrNull() is com.example.data.admin.AdminApiException &&
            (result.exceptionOrNull() as com.example.data.admin.AdminApiException).statusCode == 401
        ) {
            val refreshed = tokenProvider(true)
            result = refreshed.getOrNull()?.let { request(it) } ?: Result.failure(refreshed.exceptionOrNull()!!)
        }
        result.fold(onSuccess = { if (selected != "Dashboard") rows = it }, onFailure = { error = it.message })
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
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)) { items(rows, key = { it.id }) { AdminRowCard(it) } }
                }
            }
        }
    }
    if (loading) LaunchedEffect(selected, search, loading) { load() }
}

@Composable private fun BrandHeader() = Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)) { Text("THE GREEN SHOP", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge); Text("Store command centre", color = MaterialTheme.colorScheme.onPrimary.copy(alpha=.8f)) }
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
        item { Text("Operations overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            data.metrics.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { metric ->
                        ElevatedCard(Modifier.weight(1f).padding(bottom = 10.dp), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Text(metric.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Text("Recent orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (data.recentOrders.isEmpty()) {
            item { Text("No orders in this period", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.recentOrders, key = { "order-${it.id}" }) { AdminRowCard(it) }
        }
        item { Text("Needs restocking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (data.lowStock.isEmpty()) {
            item { Text("No low-stock products", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(data.lowStock, key = { "stock-${it.id}" }) { AdminRowCard(it) }
        }
    }
}
@Composable private fun AdminRowCard(row:AdminRow)=ElevatedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(row.title,fontWeight=FontWeight.SemiBold);Text(row.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};row.status?.takeIf{it.isNotBlank()}?.let{AssistChip(onClick={},label={Text(it.replace('_',' ').uppercase(),style=MaterialTheme.typography.labelSmall)})}}}
@Composable private fun LoadingState()=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(12.dp));Text("Loading live operations…")}
@Composable private fun ErrorState(message:String,retry:()->Unit)=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.CloudOff,null,tint=MaterialTheme.colorScheme.error);Text(message,modifier=Modifier.padding(16.dp));Button(onClick=retry){Text("Retry")}}
@Composable private fun EmptyState(name:String)=Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.Inbox,null,tint=MaterialTheme.colorScheme.primary);Text("No ${name.lowercase()} found",modifier=Modifier.padding(12.dp),fontWeight=FontWeight.SemiBold);Text("New records will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
