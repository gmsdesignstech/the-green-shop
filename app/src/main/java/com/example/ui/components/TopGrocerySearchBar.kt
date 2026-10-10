package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopDarkText
import com.example.ui.theme.GreenShopLightBorder
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopMutedText
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TopGrocerySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    selectedCategoryFilter: String = "ALL",
    onCategoryFilterChange: ((String) -> Unit)? = null,
    onSearchSubmit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val searchHints = listOf(
        "Search \"fresh spinach\" or \"palak\"",
        "Search \"farm fresh tomatoes\"",
        "Search \"robusta bananas\" or \"apples\"",
        "Search \"sweet watermelon\" or \"papaya\"",
        "Search \"organic carrots\" or \"coriander\"",
        "Search \"fresh mint\" or \"pudina\""
    )
    var currentHintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(query.isEmpty()) {
        if (query.isEmpty()) {
            while (true) {
                delay(3000)
                currentHintIndex = (currentHintIndex + 1) % searchHints.size
            }
        }
    }

    val categoryFilterPills = listOf(
        "ALL" to "All Produce",
        "cat_veg" to "🥦 Vegetables",
        "cat_fruits" to "🍎 Fruits",
        "cat_organic" to "🌱 Organic",
        "cat_herbs" to "🌿 Fresh Herbs",
        "cat_combos" to "🧺 Combos"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Main Search Input Container
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .testTag("top_grocery_search_input"),
            placeholder = {
                AnimatedContent(
                    targetState = searchHints[currentHintIndex],
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "search_animated_placeholder"
                ) { hint ->
                    Text(
                        text = hint,
                        fontSize = 13.sp,
                        color = GreenShopMutedText,
                        maxLines = 1
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Fruits and Vegetables",
                    tint = GreenShopDarkGreen,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClearQuery()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.testTag("top_grocery_search_clear")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = GreenShopDarkText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GreenShopMintSurface)
                            .clickable {
                                onQueryChange("Tomatoes")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = GreenShopPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearchSubmit?.invoke()
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GreenShopWhite,
                unfocusedContainerColor = GreenShopWhite,
                focusedBorderColor = GreenShopPrimary,
                unfocusedBorderColor = GreenShopLightBorder,
                cursorColor = GreenShopPrimary
            )
        )

        // Category Filter Quick Scoping Pills
        AnimatedVisibility(visible = query.isNotEmpty() && onCategoryFilterChange != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categoryFilterPills.forEach { (catId, catLabel) ->
                    val isSelected = selectedCategoryFilter == catId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) GreenShopPrimary else GreenShopWhite)
                            .border(
                                1.dp,
                                if (isSelected) GreenShopPrimary else GreenShopLightBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onCategoryFilterChange?.invoke(catId) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("filter_scope_$catId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = catLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) GreenShopWhite else GreenShopDarkText
                        )
                    }
                }
            }
        }
    }
}
