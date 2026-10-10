package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleData
import com.example.model.AddressItem
import com.example.model.CartItem
import com.example.model.CategoryItem
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.RecipeItem
import com.example.model.WalletTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    ORDER_AGAIN,
    CATEGORIES,
    RECIPES,
    WALLET,
    PROFILE,
    SEARCH,
    ORDER_TRACKING
}

class GreenShopViewModel : ViewModel() {

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Screen navigation stack for back handling
    private val screenHistory = mutableListOf(AppScreen.HOME)

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenHistory.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = screenHistory.last()
            return true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(
        // Initial cart has 1 item as shown in screenshots ("View cart 1 Item")
        listOf(
            CartItem(SampleData.products.first { it.id == "p_tomato" }, 1)
        )
    )
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartCount: StateFlow<Int> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val cartTotal: StateFlow<Double> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 32.0)

    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
        } else {
            current.add(CartItem(product, 1))
        }
        _cartItems.value = current
    }

    fun removeFromCart(productId: String) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (current[index].quantity > 1) {
                current[index] = current[index].copy(quantity = current[index].quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    fun getProductQuantity(productId: String): Int {
        return _cartItems.value.firstOrNull { it.product.id == productId }?.quantity ?: 0
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    /** Clears account-adjacent in-memory state before another user can sign in. */
    fun clearCustomerSessionState() {
        _cartItems.value = emptyList()
        _orders.value = emptyList()
        _activeOrder.value = null
        _favoriteIds.value = emptySet()
        selectedProductDetail.value = null
        screenHistory.clear()
        screenHistory.add(AppScreen.HOME)
        _currentScreen.value = AppScreen.HOME
    }

    // Wallet State
    private val _walletBalance = MutableStateFlow(250.0)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(SampleData.walletTransactions)
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    fun addMoneyToWallet(amount: Double) {
        _walletBalance.value += amount
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "Added to THE GREEN SHOP Money",
            amount = amount,
            isCredit = true,
            timeAgo = "Just now",
            note = "Instant UPI Top-up"
        )
        _walletTransactions.value = listOf(newTx) + _walletTransactions.value
    }

    // Orders State
    private val _orders = MutableStateFlow<List<Order>>(SampleData.sampleOrders)
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()

    fun placeOrder(tipAmount: Double = 0.0, useWallet: Boolean = false): Order {
        val items = _cartItems.value
        val subtotal = items.sumOf { it.product.price * it.quantity }
        val deliveryFee = if (subtotal > 199) 0.0 else 15.0
        val handlingFee = 4.0
        val bagFee = 5.0
        val discount = if (subtotal > 100) 25.0 else 0.0
        val finalAmount = (subtotal + deliveryFee + handlingFee + bagFee + tipAmount - discount).coerceAtLeast(0.0)

        if (useWallet && _walletBalance.value > 0) {
            val amountFromWallet = _walletBalance.value.coerceAtMost(finalAmount)
            _walletBalance.value -= amountFromWallet
            val newTx = WalletTransaction(
                id = "tx_${System.currentTimeMillis()}",
                title = "Paid for Order",
                amount = amountFromWallet,
                isCredit = false,
                timeAgo = "Just now",
                note = "Order Checkout"
            )
            _walletTransactions.value = listOf(newTx) + _walletTransactions.value
        }

        val order = Order(
            id = "GS-${(10000..99999).random()}",
            items = items,
            totalAmount = finalAmount,
            placedTime = "Just now",
            status = OrderStatus.PLACED,
            deliveryAddress = _selectedAddress.value.addressLine,
            etaMinutes = 11,
            deliveryOtp = "${(1000..9999).random()}",
            riderName = "Ramesh Kumar",
            riderPhone = "+91 98765 01234",
            riderVehicle = "Electric Scooter • KA-05-EV-1904"
        )

        _orders.value = listOf(order) + _orders.value
        _activeOrder.value = order
        clearCart()
        navigateTo(AppScreen.ORDER_TRACKING)

        // Simulate rider progress updates in coroutine
        viewModelScope.launch {
            delay(4000)
            _activeOrder.value = _activeOrder.value?.copy(status = OrderStatus.PACKED)
            delay(5000)
            _activeOrder.value = _activeOrder.value?.copy(status = OrderStatus.ON_THE_WAY, etaMinutes = 8)
        }

        return order
    }

    fun reorder(order: Order) {
        order.items.forEach { item ->
            val current = _cartItems.value.toMutableList()
            val index = current.indexOfFirst { it.product.id == item.product.id }
            if (index >= 0) {
                current[index] = current[index].copy(quantity = current[index].quantity + item.quantity)
            } else {
                current.add(item)
            }
            _cartItems.value = current
        }
        navigateTo(AppScreen.HOME)
    }

    fun addAllRecipeIngredients(recipe: RecipeItem) {
        val recipeProducts = SampleData.products.filter { it.id in recipe.ingredientProductIds }
        recipeProducts.forEach { product ->
            addToCart(product)
        }
    }

    // Address State
    private val _addresses = MutableStateFlow<List<AddressItem>>(SampleData.addresses)
    val addresses: StateFlow<List<AddressItem>> = _addresses.asStateFlow()

    private val _selectedAddress = MutableStateFlow<AddressItem>(SampleData.addresses.first())
    val selectedAddress: StateFlow<AddressItem> = _selectedAddress.asStateFlow()

    fun selectAddress(address: AddressItem) {
        _selectedAddress.value = address
    }

    fun addNewAddress(
        title: String,
        line: String,
        locality: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val newAddr = AddressItem(
            id = "addr_${System.currentTimeMillis()}",
            title = title.uppercase(),
            addressLine = line,
            locality = locality,
            isDefault = false,
            latitude = latitude,
            longitude = longitude
        )
        _addresses.value = _addresses.value + newAddr
        _selectedAddress.value = newAddr
    }

    // Wishlist / Favorites
    private val _favoriteIds = MutableStateFlow<Set<String>>(setOf("p_curd", "p_coriander", "p_paneer"))
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun toggleFavorite(productId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(productId)) {
            current.remove(productId)
        } else {
            current.add(productId)
        }
        _favoriteIds.value = current
    }

    // Search and Filtering
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val selectedSubcategory = MutableStateFlow("All")

    // Selected Product for Detail Bottom Sheet
    val selectedProductDetail = MutableStateFlow<Product?>(null)

    fun showProductDetail(product: Product) {
        selectedProductDetail.value = product
    }

    fun hideProductDetail() {
        selectedProductDetail.value = null
    }

    // Enhanced Grocery Category Navigation State
    val activeGroceryCategory = MutableStateFlow(SampleData.groceryCategories.first())
    val activeGrocerySubcategory = MutableStateFlow(SampleData.groceryCategories.first().subcategories.first())
    val activeSortOption = MutableStateFlow(com.example.model.CategorySortOption.POPULAR)
    val isOrganicOnlyFilter = MutableStateFlow(false)

    fun selectGroceryCategory(
        category: com.example.model.GroceryCategory,
        subcategory: com.example.model.GrocerySubcategory? = null
    ) {
        activeGroceryCategory.value = category
        activeGrocerySubcategory.value = subcategory ?: category.subcategories.first()
    }

    val categoryProducts: StateFlow<List<Product>> = combine(
        activeGroceryCategory,
        activeGrocerySubcategory,
        activeSortOption,
        isOrganicOnlyFilter
    ) { category, subcategory, sortOption, organicOnly ->
        var list = SampleData.products.filter { it.category == category.id }

        if (!subcategory.id.endsWith("_all")) {
            list = list.filter { it.subcategory == subcategory.name }
        }

        if (organicOnly) {
            list = list.filter { it.isOrganic }
        }

        when (sortOption) {
            com.example.model.CategorySortOption.POPULAR -> list.sortedByDescending { it.ratingCount }
            com.example.model.CategorySortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
            com.example.model.CategorySortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
            com.example.model.CategorySortOption.RATING -> list.sortedByDescending { it.rating }
            com.example.model.CategorySortOption.FASTEST -> list.sortedBy { it.deliveryMinutes }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.products.filter { it.category == "cat_veg" })

    val searchResults: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory,
        selectedSubcategory
    ) { query, cat, subcat ->
        var list = SampleData.products
        if (cat != null) {
            list = list.filter { it.category == cat.id }
        }
        if (subcat != "All") {
            list = list.filter { it.subcategory == subcat }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.subcategory.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.products)

    // User Settings
    val hideSensitiveItems = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(false)
    val isGiftCardClaimed = MutableStateFlow(false)

    fun claimGiftCard(code: String): Boolean {
        if (code.isNotBlank()) {
            addMoneyToWallet(100.0)
            isGiftCardClaimed.value = true
            return true
        }
        return false
    }
}
