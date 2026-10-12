package com.example.data.customer

import com.example.BuildConfig
import com.example.model.AddressItem
import com.example.model.CartItem
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CheckoutResult(val attemptId: String, val orderId: String, val status: String, val amountMinor: Long)
class CustomerApiException(val statusCode: Int, message: String) : IllegalStateException(message)

class CustomerRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()) {
    private val baseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')

    suspend fun orders(token: String): Result<List<Order>> = request("customer/orders?limit=50", token) { json ->
        json.optJSONArray("items").objects().map(::parseOrder)
    }

    suspend fun addresses(token: String): Result<List<AddressItem>> = request("customer/addresses", token) { json ->
        json.optJSONArray("items").objects().map(::parseAddress)
    }

    suspend fun createAddress(token: String, address: AddressItem): Result<AddressItem> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("label", address.title)
                .put("addressLine", address.addressLine)
                .put("locality", address.locality)
                .put("isDefault", address.isDefault)
                .apply {
                    address.latitude?.let { put("latitude", it) }
                    address.longitude?.let { put("longitude", it) }
                }
            parseAddress(execute("customer/addresses", token, "POST", body))
        }
    }

    suspend fun checkoutCod(
        token: String,
        items: List<CartItem>,
        address: AddressItem,
        idempotencyKey: String
    ): Result<CheckoutResult> = withContext(Dispatchers.IO) {
        runCatching {
            val checkoutItems = JSONArray()
            items.forEach { item ->
                val productId = item.product.id.toLongOrNull()
                    ?: throw IllegalStateException("The cart contains an invalid product. Refresh the catalog and try again.")
                checkoutItems.put(JSONObject().put("productId", productId).put("quantity", item.quantity))
            }
            val deliveryAddress = JSONObject()
                .put("title", address.title)
                .put("addressLine", address.addressLine)
                .put("locality", address.locality)
                .apply {
                    address.latitude?.let { put("latitude", it) }
                    address.longitude?.let { put("longitude", it) }
                }
            val body = JSONObject()
                .put("items", checkoutItems)
                .put("method", "cod")
                .put("idempotencyKey", idempotencyKey)
                .put("deliveryAddress", deliveryAddress)
            val json = execute("payments/checkout", token, "POST", body)
            CheckoutResult(
                attemptId = json.getString("attemptId"),
                orderId = json.get("orderId").toString(),
                status = json.getString("status"),
                amountMinor = json.getLong("amountMinor")
            )
        }
    }

    private suspend fun <T> request(path: String, token: String, parser: (JSONObject) -> T): Result<T> =
        withContext(Dispatchers.IO) { runCatching { parser(execute(path, token, "GET")) } }

    private fun execute(path: String, token: String, method: String, body: JSONObject? = null): JSONObject {
        require(baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://"))) {
            "The store API is not configured for this build."
        }
        val builder = Request.Builder().url("$baseUrl/$path").header("Authorization", "Bearer $token")
        val request = if (method == "POST") builder.post(
            requireNotNull(body).toString().toRequestBody("application/json".toMediaType())
        ).build() else builder.get().build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    val value = JSONObject(raw).opt("message")
                    if (value is JSONArray) value.optString(0) else value?.toString().orEmpty()
                }.getOrNull().orEmpty()
                throw CustomerApiException(response.code, message.ifBlank { "Request failed (${response.code})." })
            }
            return JSONObject(raw)
        }
    }

    private fun parseOrder(item: JSONObject): Order {
        val address = item.optJSONObject("delivery_address") ?: JSONObject()
        val orderItems = item.optJSONArray("items").objects().map { row ->
            val price = row.optDouble("unit_price", 0.0)
            CartItem(
                product = Product(
                    id = row.get("product_id").toString(), name = row.optString("name", "Product"),
                    category = row.optString("category_id"), subcategory = "", price = price,
                    originalPrice = price,
                    unit = listOf(row.optString("package_size"), row.optString("unit")).filter(String::isNotBlank).joinToString(" • "),
                    imageUrl = row.optString("image_path").takeIf { it.startsWith("https://") }
                ),
                quantity = row.optInt("quantity", 1)
            )
        }
        return Order(
            id = item.getString("id"), items = orderItems, totalAmount = item.optDouble("total", 0.0),
            placedTime = item.optString("created_at"), status = status(item.optString("status")),
            deliveryAddress = listOf(address.optString("addressLine"), address.optString("locality"))
                .filter(String::isNotBlank).joinToString(", ").ifBlank { "Address unavailable" }
        )
    }

    private fun parseAddress(item: JSONObject): AddressItem = AddressItem(
        id = item.get("id").toString(),
        title = item.optString("label", "Address"),
        addressLine = item.optString("address_line"),
        locality = item.optString("locality"),
        isDefault = item.optBoolean("is_default"),
        latitude = item.optDouble("latitude").takeUnless(Double::isNaN),
        longitude = item.optDouble("longitude").takeUnless(Double::isNaN)
    )

    private fun status(value: String): OrderStatus = when (value) {
        "confirmed" -> OrderStatus.CONFIRMED
        "preparing" -> OrderStatus.PREPARING
        "rider_assigned" -> OrderStatus.PACKED
        "out_for_delivery" -> OrderStatus.ON_THE_WAY
        "delivered" -> OrderStatus.DELIVERED
        "cancelled" -> OrderStatus.CANCELLED
        "refunded" -> OrderStatus.REFUNDED
        else -> OrderStatus.PLACED
    }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
