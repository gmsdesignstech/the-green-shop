package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GreenShopBrandBanner
import com.example.ui.components.ProductPhotoCarousel
import com.example.ui.components.ProductCard
import com.example.ui.components.TopDeliveryBar
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPaleGreen
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.ui.theme.GreenShopYellowBright
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GreenShopViewModel

@Composable
fun HomeScreen(
    viewModel: GreenShopViewModel,
    onViewCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val products by viewModel.catalogProducts.collectAsState()
    val categories by viewModel.catalogCategories.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()
    val catalogError by viewModel.catalogError.collectAsState()

    val categoryTabs = listOf("All") + categories.map { it.name }
    val freshPicks = products.take(10)
    val leafyProducts = products.filter {
        it.name.contains("leaf", true) || it.name.contains("spinach", true) ||
            it.subcategory.contains("leaf", true) || it.subcategory.contains("herb", true)
    }
    val fruitCategoryId = categories.firstOrNull { it.name.contains("fruit", true) }?.id
    val fruitProducts = if (fruitCategoryId == null) emptyList() else products.filter { it.category == fruitCategoryId }
    val vegetableProducts = products.filter {
        it.subcategory.contains("root", true) || it.subcategory.contains("tomato", true) ||
            it.subcategory.contains("cucumber", true)
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    var homeSearchQuery by remember { mutableStateOf("") }
    var homeSearchCategoryScope by remember { mutableStateOf("ALL") }

    val matchingProducts = remember(homeSearchQuery, homeSearchCategoryScope, products) {
        if (homeSearchQuery.isBlank()) emptyList()
        else {
            val q = homeSearchQuery.trim().lowercase()
            products.filter { prod ->
                val matchesText = prod.name.lowercase().contains(q) ||
                        prod.subcategory.lowercase().contains(q) ||
                        prod.description.lowercase().contains(q)
                val matchesScope = homeSearchCategoryScope == "ALL" || prod.category == homeSearchCategoryScope
                matchesText && matchesScope
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Sticky/Top Delivery Bar with Live Search Input
        item {
            TopDeliveryBar(
                currentAddress = selectedAddress,
                onAddressClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                onProfileClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                searchQuery = homeSearchQuery,
                onQueryChange = { homeSearchQuery = it },
                onClearQuery = { homeSearchQuery = "" },
                selectedCategoryScope = homeSearchCategoryScope,
                onCategoryScopeChange = { homeSearchCategoryScope = it }
            )
        }

        // ========================================================
        // LIVE SEARCH RESULTS MODE (When user types a search query)
        // ========================================================
        if (homeSearchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fresh Produce Results",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Found ${matchingProducts.size} item${if (matchingProducts.size == 1) "" else "s"} for \"$homeSearchQuery\"",
                            fontSize = 12.sp,
                            color = GreenShopMutedText
                        )
                    }

                    Text(
                        text = "Clear",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier
                            .clickable { homeSearchQuery = "" }
                            .padding(4.dp)
                    )
                }
            }

            if (matchingProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🌱", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No produce matching \"$homeSearchQuery\"",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for these fresh items:",
                                fontSize = 12.sp,
                                color = GreenShopMutedText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                listOf("Tomatoes", "Bananas", "Palak", "Apples").forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(GreenShopMintSurface)
                                            .clickable { homeSearchQuery = suggestion }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenShopDarkGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val chunkedProducts = matchingProducts.chunked(2)
                items(chunkedProducts) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { product ->
                            val qty = viewModel.getProductQuantity(product.id)
                            val isFav = favoriteIds.contains(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                isFavorite = isFav,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                onProductClick = { viewModel.showProductDetail(product) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // ========================================================
            // NORMAL FRUITS & VEGETABLES HOME EXPERIENCE
            // ========================================================

            if (catalogError != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Live catalog unavailable", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(catalogError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(onClick = viewModel::refreshCatalog, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp)); Text("Retry")
                            }
                        }
                    }
                }
            } else if (catalogLoading) {
                item { Text("Loading today’s fresh catalog…", modifier = Modifier.padding(20.dp), color = GreenShopMutedText) }
            } else if (products.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No products are available right now", fontWeight = FontWeight.Bold)
                        Text("Pull back later or tap Refresh to check the live catalog again.", color = GreenShopMutedText)
                        Button(onClick = viewModel::refreshCatalog, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp)); Text("Refresh")
                        }
                    }
                }
            }

            // Category shortcuts come from the live backend and open the functional category view.
            if (categories.isNotEmpty()) item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = GreenShopPrimary,
                    divider = {},
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    categoryTabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedTabIndex = index
                                categories.getOrNull(index - 1)?.let {
                                    viewModel.selectGroceryCategory(it)
                                    viewModel.navigateTo(AppScreen.CATEGORIES)
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) GreenShopPrimary else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GreenShopPrimary else GreenShopLightBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("home_category_tab_$index")
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GreenShopWhite else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (products.isNotEmpty()) {
            item {
                ProductPhotoCarousel(
                    products = products.take(8),
                    onProductClick = viewModel::showProductDetail,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Hero Farm Fresh Harvest Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("home_hero_promo_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GreenShopYellow)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FRESH PICKS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GreenShopDarkText
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Selected for your basket",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenShopYellowBright
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Vegetables & fruits in one place",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenShopWhite
                            )
                            Text(
                                text = "Browse the current catalog and choose what works for you",
                                fontSize = 12.sp,
                                color = GreenShopWhite.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GreenShopYellow)
                                    .clickable {
                                        categories.firstOrNull()?.let(viewModel::selectGroceryCategory)
                                        viewModel.navigateTo(AppScreen.CATEGORIES)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Browse fresh picks",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenShopDarkText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Explore",
                                    tint = GreenShopDarkText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Featured Categories Highlight Cards
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Fresh Departments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val featuredBanners = listOf(
                            Triple("ORGANIC GREENS", "Hydroponic Spinach & Herbs", Color(0xFFC8E6C9)),
                            Triple("ORCHARD FRUITS", "Shimla Apples & Bananas", Color(0xFFFFE0B2)),
                            Triple("SALAD BOXES", "Daily Cucumber & Tomato Kits", Color(0xFFFFF9C4)),
                            Triple("DAILY ROOTS", "Fresh Onions & Ooty Carrots", Color(0xFFE0F2F1))
                        )
                        items(featuredBanners) { (tag, title, bg) ->
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.navigateTo(AppScreen.CATEGORIES)
                                    },
                                colors = CardDefaults.cardColors(containerColor = bg)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GreenShopDarkGreen)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenShopWhite
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenShopDarkText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Shop Now →",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GreenShopPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // "Reordering will be easy" Hero Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("reordering_easy_card"),
                    colors = CardDefaults.cardColors(containerColor = GreenShopPaleGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopMintSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(GreenShopWhite)
                                .border(2.dp, GreenShopMintSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🥦", fontSize = 42.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Reordering Fresh Produce is Easy",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopDarkText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily vegetables and fruits you order appear here so you can reorder with 1 click",
                            fontSize = 12.sp,
                            color = GreenShopMutedText,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (orders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.ORDER_AGAIN) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("order_again_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Order Again",
                                    tint = GreenShopWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reorder Recent Produce", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                if (freshPicks.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fresh Picks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopPrimary,
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(AppScreen.CATEGORIES) }
                                .padding(8.dp),
                            maxLines = 1
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(freshPicks, key = { it.id }) { product ->
                            val qty = viewModel.getProductQuantity(product.id)
                            val isFav = favoriteIds.contains(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                isFavorite = isFav,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                onProductClick = { viewModel.showProductDetail(product) }
                            )
                        }
                    }
                }
                }
            }

            // Section 2: "Fresh Leafy Greens & Herbs"
            item {
                if (leafyProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Leafy Greens & Herbs",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available leafy greens and herbs",
                                fontSize = 12.sp,
                                color = GreenShopMutedText
                            )
                        }
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopPrimary,
                            modifier = Modifier
                                .clickable {
                                    val vegCat = categories.firstOrNull { it.name.contains("veget", true) }
                                    if (vegCat != null) viewModel.selectGroceryCategory(vegCat)
                                    viewModel.navigateTo(AppScreen.CATEGORIES)
                                }
                                .padding(8.dp),
                            maxLines = 1
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(leafyProducts, key = { it.id }) { product ->
                            val qty = viewModel.getProductQuantity(product.id)
                            val isFav = favoriteIds.contains(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                isFavorite = isFav,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                onProductClick = { viewModel.showProductDetail(product) }
                            )
                        }
                    }
                }
                }
            }

            // Section 3: "Sweet Orchard Fruits"
            item {
                if (fruitProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Sweet Orchard Fruits",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available products from the fruit catalog",
                                fontSize = 12.sp,
                                color = GreenShopMutedText
                            )
                        }
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopPrimary,
                            modifier = Modifier
                                .clickable {
                                    val fruitCat = categories.firstOrNull { it.name.contains("fruit", true) }
                                    if (fruitCat != null) viewModel.selectGroceryCategory(fruitCat)
                                    viewModel.navigateTo(AppScreen.CATEGORIES)
                                }
                                .padding(8.dp),
                            maxLines = 1
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(fruitProducts, key = { it.id }) { product ->
                            val qty = viewModel.getProductQuantity(product.id)
                            val isFav = favoriteIds.contains(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                isFavorite = isFav,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                onProductClick = { viewModel.showProductDetail(product) }
                            )
                        }
                    }
                }
                }
            }

            // Section 4: "Daily Cooking Vegetables & Roots"
            item {
                if (vegetableProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Daily Kitchen Vegetables",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available vegetables for everyday cooking",
                                fontSize = 12.sp,
                                color = GreenShopMutedText
                            )
                        }
                        Text(
                            text = "See all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenShopPrimary,
                            modifier = Modifier
                                .clickable {
                                    val vegCat = categories.firstOrNull { it.name.contains("veget", true) }
                                    if (vegCat != null) viewModel.selectGroceryCategory(vegCat)
                                    viewModel.navigateTo(AppScreen.CATEGORIES)
                                }
                                .padding(8.dp),
                            maxLines = 1
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vegetableProducts, key = { it.id }) { product ->
                            val qty = viewModel.getProductQuantity(product.id)
                            val isFav = favoriteIds.contains(product.id)
                            ProductCard(
                                product = product,
                                quantity = qty,
                                isFavorite = isFav,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                                onProductClick = { viewModel.showProductDetail(product) }
                            )
                        }
                    }
                }
                }
            }

            // Section 5: Trust & 100% Farm Fresh Guarantee Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GreenShopMintSurface)
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GreenShopBrandBanner(showTagline = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Live prices, stock availability, and product details come directly from the store catalog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GreenShopMutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            }
        }
    }
}
