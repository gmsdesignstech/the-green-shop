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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.RecipeItem
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow
import com.example.viewmodel.GreenShopViewModel

@Composable
fun RecipesScreen(
    viewModel: GreenShopViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text(
                    text = "Farm-to-Kitchen Recipes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cook healthy meals with 1-click ingredient basket delivery",
                    fontSize = 13.sp,
                    color = GreenShopMutedText
                )
            }
        }

        items(SampleData.recipes) { recipe ->
            RecipeCard(
                recipe = recipe,
                onAddAllIngredients = { viewModel.addAllRecipeIngredients(recipe) },
                onIngredientClick = { viewModel.showProductDetail(it) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun RecipeCard(
    recipe: RecipeItem,
    onAddAllIngredients: () -> Unit,
    onIngredientClick: (com.example.model.Product) -> Unit
) {
    val ingredientProducts = SampleData.products.filter { it.id in recipe.ingredientProductIds }
    val totalIngredientsPrice = ingredientProducts.sumOf { it.price }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("recipe_card_${recipe.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenShopLightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GreenShopMintSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = recipe.emoji, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = recipe.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏱️ ${recipe.prepTime} • 🔥 ${recipe.calories}",
                                fontSize = 11.sp,
                                color = GreenShopMutedText
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenShopMintSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = GreenShopYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${recipe.rating}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenShopDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ingredients Pill Row
            Text(
                text = "Fresh Ingredients (${ingredientProducts.size}):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = GreenShopMutedText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ingredientProducts.forEach { product ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F8E9))
                            .clickable { onIngredientClick(product) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${product.emoji} ${product.name.split(" ").first()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenShopDarkGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add all ingredients button
            Button(
                onClick = onAddAllIngredients,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("recipe_add_all_button_${recipe.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenShopPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "Add all",
                    tint = GreenShopWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add All ${ingredientProducts.size} Items (₹${totalIngredientsPrice.toInt()})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
