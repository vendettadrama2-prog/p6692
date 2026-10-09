package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameEntity
import com.example.ui.theme.*

@Composable
fun GameCard(
    game: GameEntity,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(game.accentColorHex))
    } catch (e: Exception) {
        PrimaryPurple
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() },
        color = CardBg,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            // Visual Hero Header of Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.45f),
                                CardBgLight
                            )
                        )
                    )
            ) {
                // Multiplier Tag
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    color = DarkBg.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "MAX ${game.maxMultiplier}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Favorite Heart Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(DarkBg.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) CoralRed else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Big Game Icon Silhouette
                val emojiIcon = when (game.id) {
                    "money_coming", "spin", "spain", "spinning_wheel", "spin_wheel" -> "🎡"
                    "fortune_gems", "fortune_gems_2" -> "💎"
                    "bubble_shooter" -> "🎯"
                    "candy_match" -> "🍬"
                    "fruit_merge" -> "🍉"
                    "aviator", "rocket", "rocket_aviator" -> "🚀"
                    "mines" -> "💎"
                    "plinko", "neon_plinko" -> "🔴"
                    "jelly" -> "🍓"
                    "neon_spin" -> "🎰"
                    "cyber_crash" -> "🚀"
                    else -> when (game.category) {
                        "Crash" -> "🚀"
                        "Slots" -> "🎰"
                        "Mines" -> "💣"
                        "Cards" -> "🃏"
                        "Arcade" -> "🕹️"
                        "Puzzle" -> "🧩"
                        else -> "🎮"
                    }
                }
                Text(
                    text = emojiIcon,
                    fontSize = 42.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Quick Play Action Overlay Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple)
                        .clickable { onPlayClick() }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card Body Info
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = game.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = game.category,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldYellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = game.rating.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
