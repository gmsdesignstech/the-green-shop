package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleData
import com.example.data.catalog.CatalogRepository
import com.example.data.customer.CustomerRepository
import com.example.data.customer.CartStore
import com.example.model.AddressItem
import com.example.model.CartItem
import com.example.model.CategoryItem
import com.example.model.Order
import com.example.model.Product
import com.example.model.Promotion
import com.example.model.RecipeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    HOME,
    CATEGORIES,
    OFFERS,
    ORDER_AGAIN,
    RECIPES,
    PROFILE,
    SEARCH,
    ORDER_TRACKING
}

class GreenShopViewModel(
    private val catalogRepository: CatalogRepository = CatalogRepository(),
    private val customerRepository: CustomerRepository = CustomerRepository(),
    private val cartStore: CartStore? = null
) : ViewModel() {

    private val loadingCategory = com.example.model.GroceryCategory(
        id = "", name = "Loading", shortName = "Loading", emoji = "🧺",
        heroTagline = "Loading today’s catalog", bgTintHex = 0xFFE8F5E9,
        primaryColorHex = 0xFF1B5E20,
        subcategories = listOf(com.example.model.GrocerySubcategory("_all", "All", "🧺"))
    )
    private val _catalogProducts = MutableStateFlow<List<Product>>(emptyList())
    val catalogProducts: StateFlow<List<Product>> = _catalogProducts.asStateFlow()
    private val _catalogCategories = MutableStateFlow<List<com.example.model.GroceryCategory>>(emptyList())
    val catalogCategories: StateFlow<List<com.example.model.GroceryCategory>> = _catalogCategories.asStateFlow()
    private val _catalogPromotions = MutableStateFlow<List<Promotion>>(emptyList())
    val catalogPromotions: StateFlow<List<Promotion>> = _catalogPromotions.asStateFlow()
    private val _catalogLoading = MutableStateFlow(true)
    val catalogLoading: StateFlow<Boolean> = _catalogLoading.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()
    private val _ordersLoading = MutableStateFlow(false)
    val ordersLoading: StateFlow<Boolean> = _ordersLoading.asStateFlow()
    private val _ordersError = MutableStateFlow<String?>(null)
    val ordersError: StateFlow<String?> = _ordersError.asStateFlow()
    private val _checkoutLoading = MutableStateFlow(false)
    val checkoutLoading: StateFlow<Boolean> = _checkoutLoading.asStateFlow()
    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()
    private val _addressSaving = MutableStateFlow(false)
    val addressSaving: StateFlow<Boolean> = _addressSaving.asStateFlow()
    private val _addressError = MutableStateFlow<String?>(null)
    val addressError: StateFlow<String?> = _addressError.asStateFlow()
    private var checkoutKey = UUID.randomUUID().toString()
    private var pendingCartQuantities: Map<String, Int> = cartStore?.loadQuantities().orEmpty()
    private var catalogRefreshJob: Job? = null

    init { refreshCatalog() }

    fun refreshCatalog() {
        if (catalogRefreshJob?.isActive == true) return
        catalogRefreshJob = viewModelScope.launch {
            _catalogLoading.value = true
            _catalogError.value = null
            catalogRepository.load().fold(
                onSuccess = { snapshot ->
                    _catalogProducts.value = snapshot.products
                    _catalogCategories.value = snapshot.categories
                    _catalogPromotions.value = snapshot.promotions
                    if (pendingCartQuantities.isNotEmpty()) {
                        _cartItems.value = pendingCartQuantities.mapNotNull { (id, requested) ->
                            snapshot.products.firstOrNull { it.id == id && it.inStock }?.let { product ->
                                CartItem(product, requested.coerceAtMost(product.availableQuantity))
                            }
                        }
                        persistCart()
                        pendingCartQuantities = emptyMap()
                    }
                    snapshot.categories.firstOrNull()?.let(::selectGroceryCategory)
                },
                onFailure = { error ->
                    _catalogError.value = error.message ?: "The live catalog is temporarily unavailable."
                }
            )
            _catalogLoading.value = false
        }
    }

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
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartCount: StateFlow<Int> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val cartTotal: StateFlow<Double> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun addToCart(product: Product) {
        if (!product.inStock) return
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            if (current[index].quantity < product.availableQuantity) {
                current[index] = current[index].copy(quantity = current[index].quantity + 1)
            }
        } else {
            current.add(CartItem(product, 1))
        }
        _cartItems.value = current
        persistCart()
        checkoutKey = UUID.randomUUID().toString()
        _checkoutError.value = null
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
            persistCart()
            checkoutKey = UUID.randomUUID().toString()
            _checkoutError.value = null
        }
    }

    fun getProductQuantity(productId: String): Int {
        return _cartItems.value.firstOrNull { it.product.id == productId }?.quantity ?: 0
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        cartStore?.clear()
        checkoutKey = UUID.randomUUID().toString()
    }

    /** Clears account-adjacent in-memory state before another user can sign in. */
    fun clearCustomerSessionState() {
        _cartItems.value = emptyList()
        cartStore?.clear()
        _orders.value = emptyList()
        _activeOrder.value = null
        _favoriteIds.value = emptySet()
        selectedProductDetail.value = null
        screenHistory.clear()
        screenHistory.add(AppScreen.HOME)
        _currentScreen.value = AppScreen.HOME
    }

    // Orders State
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()

    fun refreshOrders(tokenProvider: suspend (Boolean) -> Result<String>) {
        if (_ordersLoading.value) return
        viewModelScope.launch {
            _ordersLoading.value = true
            _ordersError.value = null
            tokenProvider(false).fold(
                onSuccess = { token -> customerRepository.orders(token).fold(
                    onSuccess = { _orders.value = it },
                    onFailure = { _ordersError.value = it.message ?: "Could not load your orders." }
                ) },
                onFailure = { _ordersError.value = it.message ?: "Sign in again to load your orders." }
            )
            _ordersLoading.value = false
        }
    }

    fun refreshAddresses(tokenProvider: suspend (Boolean) -> Result<String>) {
        viewModelScope.launch {
            tokenProvider(false).onSuccess { token ->
                customerRepository.addresses(token).fold(
                    onSuccess = { loaded ->
                        _addresses.value = loaded
                        _selectedAddress.value = loaded.firstOrNull { it.isDefault } ?: loaded.firstOrNull() ?: emptyAddress
                        _addressError.value = null
                    },
                    onFailure = { _addressError.value = it.message ?: "Could not load saved addresses." }
                )
            }
        }
    }

    fun saveAddress(tokenProvider: suspend (Boolean) -> Result<String>, address: AddressItem, onSuccess: () -> Unit) {
        if (_addressSaving.value) return
        viewModelScope.launch {
            _addressSaving.value = true
            _addressError.value = null
            val result = tokenProvider(false).fold(
                onSuccess = { customerRepository.createAddress(it, address.copy(isDefault = _addresses.value.isEmpty())) },
                onFailure = { Result.failure(it) }
            )
            result.fold(
                onSuccess = { saved ->
                    _addresses.value = if (saved.isDefault) _addresses.value.map { it.copy(isDefault = false) } + saved else _addresses.value + saved
                    _selectedAddress.value = saved
                    onSuccess()
                },
                onFailure = { _addressError.value = it.message ?: "Could not save this address." }
            )
            _addressSaving.value = false
        }
    }

    fun checkoutCod(tokenProvider: suspend (Boolean) -> Result<String>, onSuccess: () -> Unit) {
        if (_checkoutLoading.value || _cartItems.value.isEmpty()) return
        val submittedItems = _cartItems.value
        val key = checkoutKey
        viewModelScope.launch {
            _checkoutLoading.value = true
            _checkoutError.value = null
            val result = tokenProvider(false).fold(
                onSuccess = { token -> customerRepository.checkoutCod(token, submittedItems, _selectedAddress.value, key) },
                onFailure = { Result.failure(it) }
            )
            result.fold(
                onSuccess = { checkout ->
                    _cartItems.value = emptyList()
                    cartStore?.clear()
                    checkoutKey = UUID.randomUUID().toString()
                    val token = tokenProvider(false).getOrNull()
                    if (token != null) {
                        customerRepository.orders(token).onSuccess { loaded ->
                            _orders.value = loaded
                            _activeOrder.value = loaded.firstOrNull { it.id == checkout.orderId }
                        }
                    }
                    navigateTo(AppScreen.ORDER_TRACKING)
                    onSuccess()
                },
                onFailure = { _checkoutError.value = it.message ?: "Checkout failed. Please retry." }
            )
            _checkoutLoading.value = false
        }
    }

    fun reorder(order: Order) {
        order.items.forEach { item ->
            val liveProduct = _catalogProducts.value.firstOrNull { it.id == item.product.id } ?: return@forEach
            val current = _cartItems.value.toMutableList()
            val index = current.indexOfFirst { it.product.id == liveProduct.id }
            if (index >= 0) {
                current[index] = current[index].copy(quantity = (current[index].quantity + item.quantity).coerceAtMost(liveProduct.availableQuantity))
            } else {
                current.add(CartItem(liveProduct, item.quantity.coerceAtMost(liveProduct.availableQuantity)))
            }
            _cartItems.value = current
            persistCart()
        }
        navigateTo(AppScreen.HOME)
    }

    fun addAllRecipeIngredients(recipe: RecipeItem) {
        val recipeProducts = _catalogProducts.value.filter { it.id in recipe.ingredientProductIds }
        recipeProducts.forEach { product ->
            addToCart(product)
        }
    }

    // Address State
    private val emptyAddress = AddressItem(id = "", title = "Address", addressLine = "", locality = "")
    private val _addresses = MutableStateFlow<List<AddressItem>>(emptyList())
    val addresses: StateFlow<List<AddressItem>> = _addresses.asStateFlow()

    private val _selectedAddress = MutableStateFlow(emptyAddress)
    val selectedAddress: StateFlow<AddressItem> = _selectedAddress.asStateFlow()

    fun selectAddress(address: AddressItem) {
        _selectedAddress.value = address
    }

    // Wishlist / Favorites
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
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
    val activeGroceryCategory = MutableStateFlow(loadingCategory)
    val activeGrocerySubcategory = MutableStateFlow(loadingCategory.subcategories.first())
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
        isOrganicOnlyFilter,
        _catalogProducts
    ) { category, subcategory, sortOption, organicOnly, products ->
        var list = products.filter { it.category == category.id }

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<List<Product>> = combine(
        searchQuery,
        selectedCategory,
        selectedSubcategory,
        _catalogProducts
    ) { query, cat, subcat, products ->
        var list = products
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // User Settings
    val hideSensitiveItems = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(false)
    private fun persistCart() {
        cartStore?.saveQuantities(_cartItems.value.associate { it.product.id to it.quantity })
    }
}
