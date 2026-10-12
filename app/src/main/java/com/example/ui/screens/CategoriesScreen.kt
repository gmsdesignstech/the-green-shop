package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategorySortOption
import com.example.model.GroceryCategory
import com.example.model.GrocerySubcategory
import com.example.ui.components.ProductCard
import com.example.ui.components.TopDeliveryBar
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopDiscountBg
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GreenShopViewModel

enum class CategoryLayoutMode {
    SPLIT_NAVIGATOR,  // Quick-Commerce Left Rail + Right Content Pane (optimized for rapid navigation)
    CATALOG_GRID      // Visual category overview grid with subcategory shortcuts
}

@Composable
fun CategoriesScreen(
    viewModel: GreenShopViewModel,
    onViewCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val activeCategory by viewModel.activeGroceryCategory.collectAsState()
    val activeSubcategory by viewModel.activeGrocerySubcategory.collectAsState()
    val categoryProducts by viewModel.categoryProducts.collectAsState()
    val activeSortOption by viewModel.activeSortOption.collectAsState()
    val isOrganicOnly by viewModel.isOrganicOnlyFilter.collectAsState()
    val categories by viewModel.catalogCategories.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()
    val catalogError by viewModel.catalogError.collectAsState()

    var layoutMode by remember { mutableStateOf(CategoryLayoutMode.SPLIT_NAVIGATOR) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val searchQuery by viewModel.searchQuery.collectAsState()

        // Top Delivery and Search Bar
        TopDeliveryBar(
            currentAddress = selectedAddress,
            onAddressClick = { viewModel.navigateTo(AppScreen.PROFILE) },
            onProfileClick = { viewModel.navigateTo(AppScreen.PROFILE) },
            searchQuery = searchQuery,
            onQueryChange = {
                viewModel.searchQuery.value = it
                if (it.isNotEmpty()) viewModel.navigateTo(AppScreen.SEARCH)
            },
            onClearQuery = { viewModel.searchQuery.value = "" }
        )

        // Navigation Mode Switcher & Quick Category Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, GreenShopLightBorder)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Produce Categories",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "100% Farm Fresh Fruits & Vegetables",
                    fontSize = 11.sp,
                    color = GreenShopMutedText
                )
            }

            // Mode Toggle Chips (Split Rail vs Grid Hub)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF1F3F4))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (layoutMode == CategoryLayoutMode.SPLIT_NAVIGATOR) GreenShopPrimary else Color.Transparent)
                        .clickable { layoutMode = CategoryLayoutMode.SPLIT_NAVIGATOR }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("mode_split_navigator"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Side Navigator",
                            tint = if (layoutMode == CategoryLayoutMode.SPLIT_NAVIGATOR) GreenShopWhite else GreenShopDarkText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Navigator",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (layoutMode == CategoryLayoutMode.SPLIT_NAVIGATOR) GreenShopWhite else GreenShopDarkText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (layoutMode == CategoryLayoutMode.CATALOG_GRID) GreenShopPrimary else Color.Transparent)
                        .clickable { layoutMode = CategoryLayoutMode.CATALOG_GRID }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("mode_catalog_grid"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid Catalog",
                            tint = if (layoutMode == CategoryLayoutMode.CATALOG_GRID) GreenShopWhite else GreenShopDarkText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Overview",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (layoutMode == CategoryLayoutMode.CATALOG_GRID) GreenShopWhite else GreenShopDarkText
                        )
                    }
                }
            }
        }

        // Render based on selected Navigation Layout Mode
        if (layoutMode == CategoryLayoutMode.SPLIT_NAVIGATOR) {
            // ==========================================
            // LAYOUT 1: SPLIT-PANE QUICK-COMMERCE NAVIGATOR
            // Left Rail: Categories (Veggies, Fruits, Dairy, Staples, Snacks)
            // Right Pane: Subcategories, Sorting & Product Grid
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                // Left Vertical Category Sidebar Rail (Width: 92.dp)
                LazyColumn(
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF7F8F9))
                        .border(1.dp, GreenShopLightBorder),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = activeCategory.id == category.id
                        CategorySidebarRailItem(
                            category = category,
                            isSelected = isSelected,
                            onClick = {
                                viewModel.selectGroceryCategory(category)
                            }
                        )
                    }
                }

                // Right Pane: Subcategories Chips, Filter & Product Grid
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Category Hero Tagline Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(activeCategory.bgTintHex),
                                        Color(activeCategory.bgTintHex).copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = activeCategory.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = activeCategory.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(activeCategory.primaryColorHex)
                                    )
                                    Text(
                                        text = activeCategory.heroTagline,
                                        fontSize = 10.sp,
                                        color = GreenShopMutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (activeCategory.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(activeCategory.primaryColorHex))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeCategory.badge!!,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenShopWhite
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal Scrollable Subcategory Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeCategory.subcategories.forEach { subcategory ->
                            val isSubSelected = activeSubcategory.id == subcategory.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSubSelected) GreenShopPrimary else Color(0xFFF1F3F4))
                                    .border(
                                        1.dp,
                                        if (isSubSelected) GreenShopPrimary else GreenShopLightBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.activeGrocerySubcategory.value = subcategory
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("subcategory_chip_${subcategory.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = subcategory.icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = subcategory.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSubSelected) GreenShopWhite else GreenShopDarkText
                                    )
                                    if (subcategory.tag != null && !isSubSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(GreenShopMintSurface)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = subcategory.tag,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenShopDarkGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sort & Filter Controls Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort Dropdown Button
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, GreenShopLightBorder, RoundedCornerShape(8.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("category_sort_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Sort",
                                    tint = GreenShopDarkGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeSortOption.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GreenShopDarkText
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = GreenShopMutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                CategorySortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (activeSortOption == option) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (activeSortOption == option) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = GreenShopPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.activeSortOption.value = option
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Organic Only Toggle Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isOrganicOnly) GreenShopMintSurface else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (isOrganicOnly) GreenShopPrimary else GreenShopLightBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.isOrganicOnlyFilter.value = !isOrganicOnly
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("organic_filter_chip"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🌱", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Organic Only",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOrganicOnly) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOrganicOnly) GreenShopDarkGreen else GreenShopDarkText
                                )
                            }
                        }
                    }

                    // Product Grid in Content Pane
                    if (categoryProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🛒", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No items match current filters",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try switching subcategories or disabling filters",
                                    fontSize = 11.sp,
                                    color = GreenShopMutedText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(148.dp),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(categoryProducts) { product ->
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // LAYOUT 2: CATALOG OVERVIEW GRID
            // Visual big cards with 1-click subcategory shortcuts
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Browse by Fresh Department",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(categories) { category ->
                    GroceryDepartmentCard(
                        category = category,
                        onCategoryClick = {
                            viewModel.selectGroceryCategory(category)
                            layoutMode = CategoryLayoutMode.SPLIT_NAVIGATOR
                        },
                        onSubcategoryClick = { subcategory ->
                            viewModel.selectGroceryCategory(category, subcategory)
                            layoutMode = CategoryLayoutMode.SPLIT_NAVIGATOR
                        }
                    )
                }
            }
        }
    }
}

/**
 * Vertical Category Sidebar Rail Item (Left navigation panel)
 */
@Composable
fun CategorySidebarRailItem(
    category: GroceryCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) GreenShopPrimary else Color.Transparent,
        label = "indicator_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("rail_category_${category.id}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Selection Bar indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Category Icon & Label
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(category.bgTintHex) else Color(0xFFEEEEEE)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(category.primaryColorHex).copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.emoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = category.shortName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(category.primaryColorHex) else GreenShopDarkText,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Visual Department Card for Catalog Overview layout
 */
@Composable
fun GroceryDepartmentCard(
    category: GroceryCategory,
    onCategoryClick: () -> Unit,
    onSubcategoryClick: (GrocerySubcategory) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCategoryClick)
            .testTag("department_card_${category.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(category.bgTintHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = category.emoji, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = category.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = category.heroTagline,
                            fontSize = 11.sp,
                            color = GreenShopMutedText
                        )
                    }
                }

                Text(
                    text = "View All →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(category.primaryColorHex)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subcategory direct tap chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                category.subcategories.drop(1).forEach { subcat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(category.bgTintHex))
                            .clickable { onSubcategoryClick(subcat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = subcat.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subcat.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(category.primaryColorHex)
                            )
                        }
                    }
                }
            }
        }
    }
}
