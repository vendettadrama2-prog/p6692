package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LogoSize
import com.example.ui.components.P6692Logo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

data class PlinkoSlot(
    val multiplier: Double,
    val color: Color,
    val label: String
)

data class PlinkoBall(
    val id: Long,
    val betPkr: Double,
    var progress: Float = 0f,
    var currentSlotIndex: Int = 4,
    var currentX: Float = 0.5f,
    var currentY: Float = 0f
)

data class PlinkoHistoryItem(
    val slotIndex: Int,
    val multiplier: Double,
    val winPkr: Double,
    val color: Color
)

@Composable
fun PlinkoGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedBet by remember { mutableDoubleStateOf(50.0) }
    var selectedRisk by remember { mutableStateOf("Medium") } // "Low", "Medium", "High"
    var isDropping by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }
    var lastMultiplier by remember { mutableStateOf<Double?>(null) }
    var lastWinAmount by remember { mutableStateOf<Double?>(null) }

    // Multiplier slots based on Risk
    val slots = remember(selectedRisk) {
        when (selectedRisk) {
            "Low" -> listOf(
                PlinkoSlot(5.6, CoralRed, "5.6x"),
                PlinkoSlot(2.1, GoldYellow, "2.1x"),
                PlinkoSlot(1.3, BrightPurple, "1.3x"),
                PlinkoSlot(0.9, CardBorder, "0.9x"),
                PlinkoSlot(0.5, DarkBg, "0.5x"),
                PlinkoSlot(0.9, CardBorder, "0.9x"),
                PlinkoSlot(1.3, BrightPurple, "1.3x"),
                PlinkoSlot(2.1, GoldYellow, "2.1x"),
                PlinkoSlot(5.6, CoralRed, "5.6x")
            )
            "High" -> listOf(
                PlinkoSlot(29.0, CoralRed, "29x"),
                PlinkoSlot(9.0, GoldYellow, "9x"),
                PlinkoSlot(3.0, BrightPurple, "3x"),
                PlinkoSlot(0.8, CardBorder, "0.8x"),
                PlinkoSlot(0.2, DarkBg, "0.2x"),
                PlinkoSlot(0.8, CardBorder, "0.8x"),
                PlinkoSlot(3.0, BrightPurple, "3x"),
                PlinkoSlot(9.0, GoldYellow, "9x"),
                PlinkoSlot(29.0, CoralRed, "29x")
            )
            else -> listOf( // Medium
                PlinkoSlot(13.0, CoralRed, "13x"),
                PlinkoSlot(4.0, GoldYellow, "4x"),
                PlinkoSlot(1.8, BrightPurple, "1.8x"),
                PlinkoSlot(0.7, CardBorder, "0.7x"),
                PlinkoSlot(0.4, DarkBg, "0.4x"),
                PlinkoSlot(0.7, CardBorder, "0.7x"),
                PlinkoSlot(1.8, BrightPurple, "1.8x"),
                PlinkoSlot(4.0, GoldYellow, "4x"),
                PlinkoSlot(13.0, CoralRed, "13x")
            )
        }
    }

    val history = remember {
        mutableStateListOf(
            PlinkoHistoryItem(2, 1.8, 90.0, BrightPurple),
            PlinkoHistoryItem(1, 4.0, 200.0, GoldYellow),
            PlinkoHistoryItem(4, 0.4, 20.0, DarkBg),
            PlinkoHistoryItem(6, 1.8, 90.0, BrightPurple),
            PlinkoHistoryItem(0, 13.0, 650.0, CoralRed)
        )
    }

    // Ball Animation State
    val ballX = remember { Animatable(0.5f) }
    val ballY = remember { Animatable(0f) }
    var activeSlotHit by remember { mutableStateOf<Int?>(null) }

    fun dropBall() {
        if (isDropping) return
        if (pkrBalance < selectedBet) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.0f", pkrBalance)}). Please deposit."
            return
        }

        betErrorMessage = ""
        isDropping = true
        activeSlotHit = null

        coroutineScope.launch {
            // Deduct via backend wallet
            val betResult = onPlaceBet(selectedBet)
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Bet could not be placed."
                isDropping = false
                return@launch
            }

            // Plinko pegs: 8 rows of pins
            // Path generation simulating pin bounces
            // Center is 4 (slot range 0 to 8)
            var currentPos = 4
            val pathPoints = mutableListOf<Pair<Float, Float>>()
            pathPoints.add(Pair(0.5f, 0.05f))

            var xNormalized = 0.5f
            for (row in 1..8) {
                val step = if (Random.nextBoolean()) 1 else -1
                currentPos = (currentPos + step).coerceIn(0, 8)
                // row fraction
                val yNorm = 0.05f + (row / 8.5f) * 0.82f
                // map currentPos (0..8) to normalized width (0.12f..0.88f)
                xNormalized = 0.12f + (currentPos / 8f) * 0.76f + (Random.nextFloat() - 0.5f) * 0.02f
                pathPoints.add(Pair(xNormalized, yNorm))
            }

            // Animate ball through path points
            ballX.snapTo(0.5f)
            ballY.snapTo(0.05f)

            for (point in pathPoints) {
                ballX.animateTo(
                    targetValue = point.first,
                    animationSpec = tween(durationMillis = 110, easing = LinearEasing)
                )
                ballY.animateTo(
                    targetValue = point.second,
                    animationSpec = tween(durationMillis = 110, easing = LinearEasing)
                )
            }

            // Landed on slot
            val finalSlotIndex = currentPos.coerceIn(0, slots.size - 1)
            activeSlotHit = finalSlotIndex
            val slot = slots[finalSlotIndex]
            val payout = selectedBet * slot.multiplier

            lastMultiplier = slot.multiplier
            lastWinAmount = payout

            // Credit or loss via backend wallet
            if (slot.multiplier >= 1.0) {
                onCashoutWin(payout, slot.multiplier, selectedBet)
            } else {
                if (payout > 0) {
                    onCashoutWin(payout, slot.multiplier, selectedBet)
                } else {
                    onRoundLost(selectedBet)
                }
            }

            history.add(
                0,
                PlinkoHistoryItem(
                    slotIndex = finalSlotIndex,
                    multiplier = slot.multiplier,
                    winPkr = payout,
                    color = slot.color
                )
            )
            if (history.size > 12) history.removeLast()

            delay(350)
            isDropping = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                P6692Logo(size = LogoSize.SMALL)
                Column {
                    Text(
                        text = "PLINKO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Physics Multiplier Drop",
                        color = AccentCyan,
                        fontSize = 10.sp
                    )
                }
            }

            // Real PKR Balance Pill
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                modifier = Modifier.clickable { onOpenDeposit() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("₨", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        text = String.format("%,.2f", pkrBalance),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Deposit", tint = NeonGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Recent Drops History Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("RECENT:", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(history) { item ->
                        Surface(
                            color = item.color.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, item.color)
                        ) {
                            Text(
                                text = "${item.multiplier}x",
                                color = if (item.multiplier >= 2.0) Color.White else item.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (betErrorMessage.isNotBlank()) {
            Text(
                text = betErrorMessage,
                color = CoralRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Plinko Board Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = CardBg,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    // Triangle Board with Pegs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw 8 rows of triangular pegs
                            val totalRows = 8
                            for (row in 0 until totalRows) {
                                val pinsInRow = row + 3
                                val y = 0.08f * h + (row.toFloat() / (totalRows - 1)) * (0.80f * h)
                                val rowWidth = (pinsInRow.toFloat() / (totalRows + 2)) * (w * 0.86f)
                                val startX = (w - rowWidth) / 2f
                                val spacing = if (pinsInRow > 1) rowWidth / (pinsInRow - 1) else 0f

                                for (col in 0 until pinsInRow) {
                                    val x = startX + col * spacing
                                    // Glow effect for pegs
                                    drawCircle(
                                        color = BrightPurple.copy(alpha = 0.35f),
                                        radius = 6.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                            }

                            // Draw Falling Ball
                            if (isDropping) {
                                val bX = ballX.value * w
                                val bY = ballY.value * h

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AccentCyan, BrightPurple),
                                        center = Offset(bX, bY),
                                        radius = 16.dp.toPx()
                                    ),
                                    radius = 12.dp.toPx(),
                                    center = Offset(bX, bY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 7.dp.toPx(),
                                    center = Offset(bX, bY)
                                )
                            }
                        }
                    }

                    // Bottom Multiplier Slots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        slots.forEachIndexed { index, slot ->
                            val isHit = activeSlotHit == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .graphicsLayer {
                                        if (isHit) {
                                            scaleX = 1.12f
                                            scaleY = 1.12f
                                        }
                                    },
                                color = if (isHit) slot.color else slot.color.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isHit) 2.dp else 1.dp,
                                    slot.color
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = slot.label,
                                        color = if (isHit) Color.White else slot.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Round Result Badge
        AnimatedVisibility(visible = lastMultiplier != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                color = if ((lastMultiplier ?: 0.0) >= 1.0) NeonGreen.copy(alpha = 0.2f) else CoralRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if ((lastMultiplier ?: 0.0) >= 1.0) NeonGreen else CoralRed
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if ((lastMultiplier ?: 0.0) >= 1.0) "WINNER! Multiplier: ${lastMultiplier}x" else "Landed: ${lastMultiplier}x",
                        color = if ((lastMultiplier ?: 0.0) >= 1.0) NeonGreen else CoralRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PKR ${String.format("%,.0f", lastWinAmount ?: 0.0)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Bottom Game Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Risk Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Risk Level:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Low", "Medium", "High").forEach { risk ->
                            val isSel = selectedRisk == risk
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isDropping) { selectedRisk = risk },
                                color = if (isSel) PrimaryPurple else CardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                            ) {
                                Text(
                                    text = risk,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Bet Amount Quick Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bet PKR:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25.0, 50.0, 100.0, 250.0, 500.0).forEach { bet ->
                            val isSel = selectedBet == bet
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isDropping) {
                                        selectedBet = bet
                                        betErrorMessage = ""
                                    },
                                color = if (isSel) AccentCyan else CardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) AccentCyan else CardBorder)
                            ) {
                                Text(
                                    text = bet.toInt().toString(),
                                    color = if (isSel) DarkBg else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Drop Ball Button
                Button(
                    onClick = { dropBall() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pkrBalance >= selectedBet) NeonGreen else CardBg
                    ),
                    enabled = !isDropping
                ) {
                    if (isDropping) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DROP BALL • PKR ${selectedBet.toInt()}",
                            color = DarkBg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
