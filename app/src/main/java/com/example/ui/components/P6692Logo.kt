package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

enum class LogoSize {
    SMALL, MEDIUM, LARGE, HERO
}

@Composable
fun P6692Logo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.MEDIUM,
    showSubtext: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val iconSize: Dp = when (size) {
        LogoSize.SMALL -> 28.dp
        LogoSize.MEDIUM -> 38.dp
        LogoSize.LARGE -> 52.dp
        LogoSize.HERO -> 76.dp
    }

    val textSize = when (size) {
        LogoSize.SMALL -> 16.sp
        LogoSize.MEDIUM -> 22.sp
        LogoSize.LARGE -> 28.sp
        LogoSize.HERO -> 38.sp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Futuristic Emblem Badge
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize / 3.5f))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrightPurple.copy(alpha = 0.9f),
                            PrimaryPurple,
                            SecondaryBg
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AccentCyan.copy(alpha = pulseAlpha),
                            BrightPurple,
                            PrimaryPurple
                        )
                    ),
                    shape = RoundedCornerShape(iconSize / 3.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Check if img_p6692_logo is available or use vector crest
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = "P6692 Logo Emblem",
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.65f)
            )
        }

        // P6692 Neon Typographic Brandmark
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "P",
                    fontSize = textSize,
                    fontWeight = FontWeight.Black,
                    color = AccentCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "6692",
                    fontSize = textSize,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                if (size != LogoSize.SMALL) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AccentCyan.copy(alpha = 0.25f), BrightPurple.copy(alpha = 0.25f))
                                )
                            )
                            .border(0.5.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "VIP",
                            fontSize = (textSize.value * 0.42).sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                }
            }
            if (showSubtext && (size == LogoSize.LARGE || size == LogoSize.HERO)) {
                Text(
                    text = "PREMIUM CYBER ENTERTAINMENT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
