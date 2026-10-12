package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenShopLightGreen,
    onPrimary = GreenShopDarkText,
    primaryContainer = GreenShopDarkGreen,
    onPrimaryContainer = GreenShopMintSurface,
    secondary = GreenShopYellowBright,
    onSecondary = GreenShopDarkText,
    secondaryContainer = GreenShopDarkSurfaceVariant,
    onSecondaryContainer = GreenShopYellowLight,
    background = GreenShopDarkBackground,
    onBackground = GreenShopDarkTextPrimary,
    surface = GreenShopDarkSurface,
    onSurface = GreenShopDarkTextPrimary,
    surfaceVariant = GreenShopDarkSurfaceVariant,
    onSurfaceVariant = GreenShopDarkTextSecondary,
    outline = GreenShopDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = GreenShopPrimary,
    onPrimary = GreenShopWhite,
    primaryContainer = GreenShopMintSurface,
    onPrimaryContainer = GreenShopDarkGreen,
    secondary = GreenShopYellow,
    onSecondary = GreenShopDarkText,
    secondaryContainer = GreenShopYellowLight,
    onSecondaryContainer = GreenShopDarkText,
    background = GreenShopBackground,
    onBackground = GreenShopDarkText,
    surface = GreenShopWhite,
    onSurface = GreenShopDarkText,
    surfaceVariant = GreenShopMintSurface,
    onSurfaceVariant = GreenShopMutedText,
    outline = GreenShopLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
