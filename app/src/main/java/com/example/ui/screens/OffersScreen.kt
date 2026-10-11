package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SampleData
import com.example.ui.components.ProductCard
import com.example.ui.theme.GreenShopMutedText
import com.example.viewmodel.GreenShopViewModel

@Composable
fun OffersScreen(viewModel: GreenShopViewModel) {
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val offers = SampleData.products.filter {
        it.inStock && it.originalPrice > it.price && it.discountPercent > 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = "Offers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Current savings on available products",
                style = MaterialTheme.typography.bodyMedium,
                color = GreenShopMutedText
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(offers, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    quantity = viewModel.getProductQuantity(product.id),
                    isFavorite = product.id in favoriteIds,
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
