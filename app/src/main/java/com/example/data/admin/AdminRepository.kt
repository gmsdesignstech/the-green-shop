package com.example.data.admin

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AdminMetric(val label: String, val value: String)
data class AdminRow(val id: String, val title: String, val subtitle: String, val status: String? = null)
data class AdminDashboard(
    val metrics: List<AdminMetric>,
    val recentOrders: List<AdminRow>,
    val lowStock: List<AdminRow>,
    val unavailableSections: Set<String> = emptySet()
)
class AdminApiException(val statusCode: Int, message: String) : IllegalStateException(message)

class AdminRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()) {
    private val baseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')
    val isConfigured = (baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://"))) &&
        !baseUrl.contains("YOUR_API_HOST")

    suspend fun dashboard(token: String): Result<AdminDashboard> =
        get("admin/dashboard", token, ::parseAdminDashboard)

    suspend fun list(section: String, token: String, search: String = ""): Result<List<AdminRow>> {
        val path = when (section) {
            "Orders" -> "admin/orders"
            "Products" -> "admin/products"
            "Categories" -> "admin/categories"
            "Inventory" -> "admin/inventory"
            "Promotions" -> "admin/promotions"
            "Payments" -> "admin/payments"
            "Reports" -> "admin/reports"
            "Notifications" -> "admin/notifications"
            "Support" -> "admin/support"
            "Store settings" -> "admin/store-settings"
            "Audit logs" -> "admin/audit-logs"
            "Delivery partners" -> "admin/applications/delivery_partner"
            else -> return Result.success(emptyList())
        }
        val url = "$baseUrl/$path".toHttpUrl().newBuilder().apply {
            if (search.isNotBlank()) addQueryParameter("search", search.trim())
        }.build()
        return request(url.toString(), token) { json ->
            val array = json.optJSONArray("items") ?: JSONArray().put(json)
            array.toRows { item -> mapRow(section, item) }
        }
    }

    private fun mapRow(section: String, item: JSONObject): AdminRow = when (section) {
        "Orders" -> AdminRow(item.optString("id"), "Order #${item.optString("id")}", "₹${item.optString("total")} • ${item.optString("payment_status")}", item.optString("status"))
        "Products" -> AdminRow(item.optString("id"), item.optString("name"), "₹${item.optString("price")} • ${item.optString("unit")}", if (item.optBoolean("is_archived")) "ARCHIVED" else "ACTIVE")
        "Categories" -> AdminRow(item.optString("id"), item.optString("name"), "${item.optInt("product_count")} products • order ${item.optInt("sort_order")}", if (item.optBoolean("is_visible")) "VISIBLE" else "HIDDEN")
        "Inventory" -> AdminRow(item.optString("product_id"), item.optString("name"), "${item.optInt("available")} available • ${item.optInt("reserved")} reserved", if (item.optInt("available") <= item.optInt("low_stock_threshold")) "LOW STOCK" else "HEALTHY")
        "Promotions" -> AdminRow(item.optString("id"), item.optString("title"), "${item.optString("discount_value")} ${item.optString("discount_type")} • ${item.optString("kind")}", item.optString("status"))
        "Payments" -> AdminRow(item.optString("id"), "Order #${item.optString("order_id")}", "₹${item.optString("amount")} • ${item.optString("method")}", item.optString("status"))
        "Reports" -> AdminRow("report", "Store performance", "${item.optInt("orders")} orders • ₹${item.optString("gross_sales")} gross sales", "LIVE")
        "Support" -> AdminRow(item.optString("id"), item.optString("category"), item.optString("priority"), item.optString("status"))
        "Store settings" -> AdminRow("settings", item.optString("store_name"), "Minimum order ₹${item.optString("minimum_order_value")} • delivery ₹${item.optString("delivery_fee")}", if (item.optBoolean("delivery_enabled")) "DELIVERY ON" else "DELIVERY OFF")
        "Delivery partners" -> AdminRow(item.optString("id"), item.optString("full_name"), "${item.optString("service_area")} • ${item.optString("vehicle_type")}", item.optString("status"))
        "Audit logs" -> AdminRow(item.optString("id"), item.optString("action").replace('_', ' '), "${item.optString("target_type")} #${item.optString("target_id")}")
        else -> AdminRow(item.optString("id"), item.optString("title"), item.optString("message"), if (item.isNull("read_at")) "UNREAD" else "READ")
    }

    private suspend fun <T> get(path: String, token: String, parse: (JSONObject) -> T) = request("$baseUrl/$path", token, parse)
    private suspend fun <T> request(url: String, token: String, parse: (JSONObject) -> T): Result<T> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Set API_BASE_URL to the deployed HTTPS backend."))
        runCatching {
            val request = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                    throw AdminApiException(response.code, message.ifBlank { when (response.code) { 401 -> "Your secure session could not be refreshed. Sign in again."; 403 -> "Administrator access is required."; else -> "Request failed (${response.code})." } })
                }
                parse(JSONObject(body))
            }
        }
    }
}

internal fun parseAdminDashboard(json: JSONObject): AdminDashboard {
    val metrics = json.optJSONObject("metrics") ?: JSONObject()
    val unavailable = json.optJSONArray("unavailable").toStringSet()
    fun metric(label: String, key: String, section: String = "metrics"): AdminMetric {
        val value = if (section in unavailable) "—" else {
            metrics.optString(key, "0").takeIf { it.isNotBlank() && it != "null" } ?: "0"
        }
        return AdminMetric(label, if (key == "gross_sales" && value != "—") "₹$value" else value)
    }
    return AdminDashboard(
        metrics = listOf(
            metric("Today’s orders", "today_orders"),
            metric("Pending", "pending_orders"),
            metric("Completed", "completed_orders"),
            metric("Gross sales", "gross_sales"),
            metric("Payment failures", "payment_failures"),
            metric("Active deliveries", "active_deliveries"),
            metric("Low stock", "low_stock_products", "lowStock"),
            metric("Partner applications", "pending_partner_applications", "pendingPartnerApplications")
        ),
        recentOrders = json.optJSONArray("recentOrders").toRows { item ->
            AdminRow(item.optString("id"), "Order #${item.optString("id")}", "₹${item.optString("total")} • ${item.optString("payment_status")}", item.optString("status"))
        },
        lowStock = json.optJSONArray("lowStock").toRows { item ->
            AdminRow(item.optString("id"), item.optString("name"), "${item.optInt("available")} available • threshold ${item.optInt("low_stock_threshold")}", "LOW STOCK")
        },
        unavailableSections = unavailable
    )
}

private fun JSONArray?.toRows(mapper: (JSONObject) -> AdminRow): List<AdminRow> {
    if (this == null) return emptyList()
    return (0 until length()).map { mapper(getJSONObject(it)) }
}

private fun JSONArray?.toStringSet(): Set<String> {
    if (this == null) return emptySet()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf(String::isNotBlank) }.toSet()
}
