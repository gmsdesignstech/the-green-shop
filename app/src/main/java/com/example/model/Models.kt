package com.example.model

enum class OrderStatus(val label: String, val icon: String) {
    PLACED("Order Placed", "📝"),
    CONFIRMED("Confirmed", "✅"),
    PREPARING("Being prepared", "🧺"),
    PACKED("Packed at Green Dark Store", "📦"),
    ON_THE_WAY("Out for Delivery", "🛵"),
    DELIVERED("Delivered", "✅"),
    CANCELLED("Cancelled", "✕"),
    REFUNDED("Refunded", "↩")
}

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val subcategory: String,
    val price: Double,
    val originalPrice: Double,
    val unit: String,
    val deliveryMinutes: Int = Int.MAX_VALUE,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val discountPercent: Int = 0,
    val emoji: String = "🥬",
    val description: String = "",
    val badge: String? = null,
    val isOrganic: Boolean = false,
    val inStock: Boolean = true,
    val availableQuantity: Int = if (inStock) Int.MAX_VALUE else 0,
    val imageUrl: String? = null
)

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val code: String?,
    val discountType: String,
    val discountValue: Double,
    val minimumOrderValue: Double?,
    val maximumDiscount: Double?,
    val expiresAt: String?,
    val terms: String?,
    val festivalName: String?,
    val firstOrderOnly: Boolean
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class GrocerySubcategory(
    val id: String,
    val name: String,
    val icon: String,
    val tag: String? = null
)

data class GroceryCategory(
    val id: String,
    val name: String,
    val shortName: String,
    val emoji: String,
    val badge: String? = null,
    val heroTagline: String,
    val bgTintHex: Long,
    val primaryColorHex: Long,
    val subcategories: List<GrocerySubcategory>
)

enum class CategorySortOption(val title: String) {
    POPULAR("Popular"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    RATING("Top Rated"),
    FASTEST("⚡ Fastest Delivery")
}

data class CategoryItem(
    val id: String,
    val name: String,
    val section: String,
    val subcategories: List<String>,
    val emoji: String,
    val bgTint: Long = 0xFFE8F5E9
)

data class Order(
    val id: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val placedTime: String,
    val status: OrderStatus,
    val deliveryAddress: String,
    val etaMinutes: Int? = null,
    val deliveryOtp: String? = null,
    val riderName: String? = null,
    val riderPhone: String? = null,
    val riderVehicle: String? = null
)

data class AddressItem(
    val id: String,
    val title: String,
    val addressLine: String,
    val locality: String,
    val isDefault: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RecipeItem(
    val id: String,
    val name: String,
    val prepTime: String,
    val calories: String,
    val difficulty: String,
    val rating: Float,
    val emoji: String,
    val ingredientProductIds: List<String>,
    val instructions: List<String>
)
