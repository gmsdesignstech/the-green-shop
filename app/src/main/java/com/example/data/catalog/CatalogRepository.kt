package com.example.data.catalog

import com.example.BuildConfig
import com.example.model.GroceryCategory
import com.example.model.GrocerySubcategory
import com.example.model.Product
import com.example.model.Promotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class CatalogSnapshot(
    val products: List<Product>,
    val categories: List<GroceryCategory>,
    val promotions: List<Promotion>
)

sealed class CatalogException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class Network(cause: Throwable) : CatalogException("Couldn't reach the store. Check your connection and retry.", cause)
    class Timeout(cause: Throwable) : CatalogException("The store is taking too long to respond. Please retry shortly.", cause)
    class Http(val statusCode: Int, message: String) : CatalogException(message)
    class Malformed(cause: Throwable? = null) : CatalogException("The store returned an unexpected response. Please retry shortly.", cause)
}

class CatalogRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(12, TimeUnit.SECONDS)
    .callTimeout(15, TimeUnit.SECONDS)
    .build()) {
    private val baseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')

    suspend fun load(): Result<CatalogSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://"))) {
                "The store API is not configured for this build."
            }
            coroutineScope {
                val categories = async { getItemsWithRetry("catalog/categories").map(::category) }
                val products = async { getItemsWithRetry("catalog/products?limit=100").map(::product) }
                val promotions = async { getItemsWithRetry("catalog/promotions").map(::promotion) }
                CatalogSnapshot(products.await(), categories.await(), promotions.await())
            }
        }
    }

    private suspend fun getItemsWithRetry(path: String): List<JSONObject> {
        repeat(2) { attempt ->
            try {
                return getItems(path)
            } catch (error: SocketTimeoutException) {
                if (attempt == 1) throw CatalogException.Timeout(error)
            } catch (error: IOException) {
                if (error is CatalogException.Http || error is CatalogException.Malformed) throw error
                if (attempt == 1) throw CatalogException.Network(error)
            }
            delay(400L)
        }
        error("Unreachable catalog retry state")
    }

    private fun getItems(path: String): List<JSONObject> {
        val request = Request.Builder().url("$baseUrl/$path").get().build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()
                val safeMessage = when (response.code) {
                    404 -> "The live catalog is not available on this server version."
                    in 500..599 -> "The store service is temporarily unavailable. Please retry shortly."
                    else -> detail.ifBlank { "The store request failed (${response.code})." }
                }
                throw CatalogException.Http(response.code, safeMessage)
            }
            val items = try {
                val payload = JSONObject(raw)
                if (!payload.has("items") || payload.optJSONArray("items") == null) throw CatalogException.Malformed()
                payload.getJSONArray("items")
            } catch (error: CatalogException.Malformed) {
                throw error
            } catch (error: Exception) {
                throw CatalogException.Malformed(error)
            }
            return (0 until items.length()).map { items.getJSONObject(it) }
        }
    }

    private fun product(item: JSONObject): Product {
        val price = item.optDouble("price", 0.0)
        val original = item.optDouble("compare_at_price", price).takeIf { !it.isNaN() && it >= price } ?: price
        val available = item.optInt("available_quantity", 0).coerceAtLeast(0)
        return Product(
            id = item.getString("id"),
            name = item.getString("name"),
            category = item.optString("category_id"),
            subcategory = item.optString("category_name", "Fresh groceries"),
            price = price,
            originalPrice = original,
            unit = listOf(item.optString("package_size"), item.optString("unit")).filter(String::isNotBlank).joinToString(" • "),
            discountPercent = if (original > price) (((original - price) / original) * 100).roundToInt() else 0,
            description = item.optString("description"),
            inStock = available > 0,
            availableQuantity = available,
            imageUrl = item.optString("image_path").takeIf { it.startsWith("https://") }
        )
    }

    private fun category(item: JSONObject): GroceryCategory {
        val id = item.getString("id")
        val name = item.getString("name")
        val icon = categoryIcon(name)
        return GroceryCategory(
            id = id,
            name = name,
            shortName = name,
            emoji = icon,
            badge = item.optInt("product_count").takeIf { it > 0 }?.let { "$it items" },
            heroTagline = item.optString("description"),
            bgTintHex = 0xFFE8F5E9,
            primaryColorHex = 0xFF1B5E20,
            subcategories = listOf(GrocerySubcategory("${id}_all", "All", icon))
        )
    }

    private fun promotion(item: JSONObject) = Promotion(
        id = item.getString("id"),
        title = item.getString("title"),
        description = item.optString("description"),
        code = item.optString("code").takeIf(String::isNotBlank),
        discountType = item.optString("discount_type"),
        discountValue = item.optDouble("discount_value", 0.0),
        minimumOrderValue = item.optDouble("minimum_order_value").takeUnless(Double::isNaN),
        maximumDiscount = item.optDouble("maximum_discount").takeUnless(Double::isNaN),
        expiresAt = item.optString("expires_at").takeIf(String::isNotBlank),
        terms = item.optString("terms").takeIf(String::isNotBlank),
        festivalName = item.optString("festival_name").takeIf(String::isNotBlank),
        firstOrderOnly = item.optBoolean("first_order_only")
    )

    private fun categoryIcon(name: String): String = when {
        name.contains("fruit", true) -> "🍎"
        name.contains("veget", true) -> "🥦"
        name.contains("dairy", true) -> "🥛"
        name.contains("staple", true) -> "🌾"
        name.contains("snack", true) -> "🥜"
        else -> "🧺"
    }
}
