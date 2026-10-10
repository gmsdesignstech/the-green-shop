package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Product
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.material3.CircularProgressIndicator

@DrawableRes
fun productPhotoResource(productId: String): Int? = when (productId) {
    "p_coriander" -> R.drawable.product_coriander
    "p_spinach" -> R.drawable.product_spinach
    "p_mint" -> R.drawable.product_mint
    "p_onion" -> R.drawable.product_onion
    "p_potato" -> R.drawable.product_potato
    "p_carrot" -> R.drawable.product_carrot
    "p_tomato" -> R.drawable.product_tomato
    "p_cucumber" -> R.drawable.product_cucumber
    "p_banana" -> R.drawable.product_banana
    "p_apple" -> R.drawable.product_apple
    else -> null
}

@Composable
fun ProductPhoto(product: Product, modifier: Modifier = Modifier) {
    val resource = productPhotoResource(product.id)
    if (!product.imageUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } },
            success = { SubcomposeAsyncImageContent() },
            error = { ProductPhotoFallback(product.name, resource, Modifier.fillMaxSize()) }
        )
    } else if (resource != null) {
        Image(painterResource(resource), product.name, modifier, contentScale = ContentScale.Crop)
    } else {
        ProductPhotoFallback(product.name, null, modifier)
    }
}

@Composable
private fun ProductPhotoFallback(name: String, @DrawableRes resource: Int?, modifier: Modifier) {
    if (resource != null) Image(painterResource(resource), name, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ImageNotSupported, "Photograph unavailable for $name", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(34.dp))
    }
}
