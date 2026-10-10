package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GreenShopDarkGreen
import com.example.ui.theme.GreenShopLightGreen
import com.example.ui.theme.GreenShopMintSurface
import com.example.ui.theme.GreenShopPrimary
import com.example.ui.theme.GreenShopWhite
import com.example.ui.theme.GreenShopYellow

@Composable
fun GreenShopLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    Image(
        painter = painterResource(R.drawable.green_shop_logo),
        contentDescription = "THE GREEN SHOP logo",
        modifier = modifier
            .size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun GreenShopBrandBanner(
    modifier: Modifier = Modifier,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GreenShopLogoBadge(size = 148.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "THE GREEN SHOP",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = GreenShopDarkGreen,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.8.sp
        )
        if (showTagline) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(GreenShopPrimary))
                Text(
                    text = "  Freshness At Your Doorstep  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = GreenShopDarkGreen
                )
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(GreenShopPrimary))
            }
        }
    }
}

@Composable
fun GreenDeliveryScooterGraphic(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(GreenShopMintSurface, GreenShopWhite)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.85f)) {
            val w = this.size.width
            val h = this.size.height

            // Wheels
            drawCircle(color = Color(0xFF263238), radius = w * 0.09f, center = Offset(w * 0.22f, h * 0.75f))
            drawCircle(color = Color(0xFFCFD8DC), radius = w * 0.045f, center = Offset(w * 0.22f, h * 0.75f))
            drawCircle(color = Color(0xFF263238), radius = w * 0.09f, center = Offset(w * 0.78f, h * 0.75f))
            drawCircle(color = Color(0xFFCFD8DC), radius = w * 0.045f, center = Offset(w * 0.78f, h * 0.75f))

            // Chassis
            val chassis = Path().apply {
                moveTo(w * 0.22f, h * 0.75f)
                lineTo(w * 0.45f, h * 0.75f)
                lineTo(w * 0.60f, h * 0.68f)
                lineTo(w * 0.75f, h * 0.75f)
                lineTo(w * 0.78f, h * 0.75f)
                lineTo(w * 0.72f, h * 0.52f)
                lineTo(w * 0.50f, h * 0.52f)
                close()
            }
            drawPath(path = chassis, color = GreenShopPrimary)

            // Handlebar
            drawLine(
                color = GreenShopDarkGreen,
                start = Offset(w * 0.70f, h * 0.52f),
                end = Offset(w * 0.74f, h * 0.35f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            // Headlight
            drawCircle(color = GreenShopYellow, radius = w * 0.04f, center = Offset(w * 0.76f, h * 0.42f))

            // Delivery Box (Green Shop backpack)
            drawRoundRect(
                color = GreenShopDarkGreen,
                topLeft = Offset(w * 0.14f, h * 0.38f),
                size = Size(w * 0.25f, h * 0.26f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Leaf on delivery box
            drawCircle(color = GreenShopLightGreen, radius = w * 0.04f, center = Offset(w * 0.26f, h * 0.51f))

            // Rider
            // Body
            drawRoundRect(
                color = GreenShopPrimary,
                topLeft = Offset(w * 0.44f, h * 0.40f),
                size = Size(w * 0.16f, h * 0.22f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Helmet
            drawCircle(color = GreenShopDarkGreen, radius = w * 0.09f, center = Offset(w * 0.55f, h * 0.26f))
            // Visor
            drawLine(
                color = Color.Black,
                start = Offset(w * 0.58f, h * 0.26f),
                end = Offset(w * 0.64f, h * 0.26f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Speed lines
            drawLine(
                color = GreenShopLightGreen,
                start = Offset(w * 0.02f, h * 0.42f),
                end = Offset(w * 0.10f, h * 0.42f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = GreenShopLightGreen,
                start = Offset(0f, h * 0.55f),
                end = Offset(w * 0.08f, h * 0.55f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = GreenShopLightGreen,
                start = Offset(w * 0.03f, h * 0.68f),
                end = Offset(w * 0.11f, h * 0.68f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}
