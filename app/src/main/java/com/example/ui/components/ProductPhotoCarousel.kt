package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Product

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductPhotoCarousel(products: List<Product>, onProductClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    if (products.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { products.size })
    Column(modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp, beyondViewportPageCount = 1, modifier = Modifier.fillMaxWidth()) { page ->
            val product = products[page]
            Row(
                Modifier.padding(horizontal = 22.dp).fillMaxWidth().height(142.dp).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface).clickable { onProductClick(product) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductPhoto(product, Modifier.size(116.dp).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp)); Text(product.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp)); Text("₹${product.price.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            products.indices.forEach { index ->
                Box(Modifier.padding(horizontal = 3.dp).size(if (pagerState.currentPage == index) 9.dp else 6.dp).clip(CircleShape)
                    .background(if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
        }
    }
}
