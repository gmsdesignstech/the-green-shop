package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Promotion
import com.example.ui.components.ProductCard
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.viewmodel.GreenShopViewModel

@Composable
fun OffersScreen(viewModel: GreenShopViewModel) {
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val products by viewModel.catalogProducts.collectAsState()
    val promotions by viewModel.catalogPromotions.collectAsState()
    val loading by viewModel.catalogLoading.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val discounted = products.filter { it.inStock && it.originalPrice > it.price && it.discountPercent > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Text("Offers", style = MaterialTheme.typography.headlineSmall)
                Text("Active savings and eligible discounted products", color = GreenShopMutedText)
            }
        }
        if (error != null && promotions.isEmpty() && discounted.isEmpty()) {
            item { OfferMessage(error.orEmpty(), "Retry", viewModel::refreshCatalog) }
        } else if (loading && promotions.isEmpty() && discounted.isEmpty()) {
            item { OfferMessage("Loading active offers…") }
        } else if (promotions.isEmpty() && discounted.isEmpty()) {
            item { OfferMessage("There are no active offers right now. Live prices remain available in the catalog.") }
        }
        if (promotions.isNotEmpty()) {
            item { SectionTitle("Active promotions") }
            items(promotions, key = { "promotion_${it.id}" }) { PromotionCard(it) }
        }
        if (discounted.isNotEmpty()) {
            item { SectionTitle("Products with current savings") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(discounted, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            quantity = viewModel.getProductQuantity(product.id),
                            isFavorite = product.id in favoriteIds,
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
}

@Composable
private fun PromotionCard(promotion: Promotion) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = GreenShopMintSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(promotion.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (promotion.description.isNotBlank()) Text(promotion.description, style = MaterialTheme.typography.bodyMedium)
            promotion.code?.let { Text("Code: $it", fontWeight = FontWeight.Bold) }
            promotion.festivalName?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            promotion.terms?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = GreenShopMutedText) }
            promotion.expiresAt?.let { Text("Expires: $it", style = MaterialTheme.typography.bodySmall, color = GreenShopMutedText) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge)

@Composable
private fun OfferMessage(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(message, color = GreenShopMutedText)
        if (action != null) Button(onClick = onAction) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Text(action)
        }
    }
}
