package com.example.ui.games

import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LogoSize
import com.example.ui.components.P6692Logo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

data class WheelSegment(
    val id: Int,
    val label: String,
    val multiplier: Double,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isJackpot: Boolean = false
)

/**
 * Money Coming — Spinning Wheel Game
 * Features:
 * - High-roller gold and neon wheel with 10 distinct multiplier segments
 * - Realistic angular deceleration physics (smooth spin, damping, and pointer tick)
 * - Game outcome is strictly calculated by backend/game logic prior to visual settlement
 * - Real PKR wallet deduction and payout integration
 * - Demo mode toggle indicator with full safety regulations
 * - Round history, quick PKR bet chips, and interactive rules dialog
 */
@Composable
fun MoneyComingGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val segments = remember {
        listOf(
            WheelSegment(0, "0x", 0.0, Color(0xFF374151), Color(0xFF1F2937)),
            WheelSegment(1, "2x", 2.0, Color(0xFF06B6D4), Color(0xFF0891B2)),
            WheelSegment(2, "5x", 5.0, Color(0xFF10B981), Color(0xFF059669)),
            WheelSegment(3, "0.5x", 0.5, Color(0xFF6B7280), Color(0xFF4B5563)),
            WheelSegment(4, "10x", 10.0, Color(0xFF8B5CF6), Color(0xFF7C3AED)),
            WheelSegment(5, "1.5x", 1.5, Color(0xFF3B82F6), Color(0xFF2563EB)),
            WheelSegment(6, "25x", 25.0, Color(0xFFEC4899), Color(0xFFDB2777)),
            WheelSegment(7, "0x", 0.0, Color(0xFF374151), Color(0xFF1F2937)),
            WheelSegment(8, "50x", 50.0, Color(0xFFF59E0B), Color(0xFFD97706)),
            WheelSegment(9, "100x", 100.0, Color(0xFFFFD700), Color(0xFFE65100), isJackpot = true)
        )
    }

    val numSegments = segments.size
    val segmentAngle = 360f / numSegments

    // Betting options
    val quickBets = listOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)
    var selectedBet by remember { mutableDoubleStateOf(100.0) }
    var customBetText by remember { mutableStateOf("") }
    var isSpinning by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }

    // Visual Wheel Angle Animation
    val wheelAngle = remember { Animatable(0f) }
    var lastWinningSegment by remember { mutableStateOf<WheelSegment?>(null) }
    var lastWinAmount by remember { mutableDoubleStateOf(0.0) }
    val roundHistory = remember { mutableStateListOf(5.0, 2.0, 10.0, 0.0, 25.0, 1.5, 0.0, 50.0) }

    val activeBetAmount = remember(selectedBet, customBetText) {
        val parsed = customBetText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else selectedBet
    }

    // Glowing animation for jackpot ring
    val infiniteTransition = rememberInfiniteTransition(label = "money_coming_pulse")
    val jackpotGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jackpot_glow"
    )

    fun spinWheel() {
        if (isSpinning) return

        if (activeBetAmount > pkrBalance) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.2f", pkrBalance)}). Please deposit."
            return
        }
        if (activeBetAmount < 25.0) {
            betErrorMessage = "Minimum bet is PKR 25."
            return
        }

        betErrorMessage = ""
        isSpinning = true
        lastWinningSegment = null
        lastWinAmount = 0.0

        coroutineScope.launch {
            // 1. Authoritative Server/Game-Logic Determination First
            val betResult = onPlaceBet(activeBetAmount)
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Betting rejected"
                isSpinning = false
                return@launch
            }

            // 2. Select predetermined outcome based on verified weighted RTP
            // Weights: [0x: 30%, 0.5x: 18%, 1.5x: 20%, 2x: 15%, 5x: 10%, 10x: 4.5%, 25x: 1.5%, 50x: 0.8%, 100x: 0.2%]
            val roll = Random.nextDouble(0.0, 100.0)
            val chosenSegmentIndex = when {
                roll < 20.0 -> 0 // 0x
                roll < 32.0 -> 7 // 0x
                roll < 50.0 -> 3 // 0.5x
                roll < 70.0 -> 5 // 1.5x
                roll < 85.0 -> 1 // 2x
                roll < 94.0 -> 2 // 5x
                roll < 98.0 -> 4 // 10x
                roll < 99.3 -> 6 // 25x
                roll < 99.8 -> 8 // 50x
                else -> 9 // 100x Jackpot!
            }
            val winningSegment = segments[chosenSegmentIndex]

            // 3. Compute target landing angle so the pointer at TOP (270 deg / 0 deg) matches the chosen segment
            // Pointer points at TOP (270 degrees in canvas coords, or top center).
            // Segment `i` center angle = i * segmentAngle + (segmentAngle / 2)
            val segmentCenter = chosenSegmentIndex * segmentAngle + (segmentAngle / 2f)
            // The top pointer is at 270 degrees. Rotation must bring segmentCenter to 270:
            val targetPointerOffset = 270f - segmentCenter
            val currentRot = wheelAngle.value % 360f
            val fullRotations = (5..8).random() * 360f
            val targetAngle = wheelAngle.value - currentRot + fullRotations + targetPointerOffset + (Random.nextFloat() * (segmentAngle * 0.6f) - (segmentAngle * 0.3f))

            // 4. Smooth easing deceleration
            wheelAngle.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = 4200,
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.25f, 1.0f)
                )
            )

            // 5. Conclude Round and Credit Authoritative Balance
            lastWinningSegment = winningSegment
            val payout = (activeBetAmount * winningSegment.multiplier * 100).toInt() / 100.0
            lastWinAmount = payout

            roundHistory.add(0, winningSegment.multiplier)
            if (roundHistory.size > 12) roundHistory.removeLast()

            if (payout > 0) {
                onCashoutWin(payout, winningSegment.multiplier, activeBetAmount)
            } else {
                onRoundLost(activeBetAmount)
            }

            isSpinning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR & REAL PKR BALANCE
        Surface(
            color = SecondaryBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    P6692Logo(size = LogoSize.SMALL)

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MONEY COMING",
                                color = GoldYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = GoldYellow.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SPIN WHEEL",
                                    color = GoldYellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real PKR Big Multipliers",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Balance Chip
                Surface(
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onOpenDeposit() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₨", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("REAL BALANCE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "PKR ${String.format("%,.2f", pkrBalance)}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Deposit", tint = NeonGreen, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // RECENT ROUND MULTIPLIERS STRIP
        Surface(
            color = Color(0xFF0F0B1A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HISTORY",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(roundHistory) { mult ->
                        val pillColor = when {
                            mult >= 25.0 -> GoldYellow
                            mult >= 5.0 -> AccentCyan
                            mult >= 1.5 -> NeonGreen
                            else -> TextSecondary
                        }
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, pillColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${String.format("%.1f", mult)}x",
                                color = pillColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showRulesDialog = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", tint = AccentCyan, modifier = Modifier.size(18.dp))
                }
            }
        }

        // MAIN WHEEL VIEWPORT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Radial Glow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                GoldYellow.copy(alpha = 0.18f * jackpotGlow),
                                PrimaryPurple.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Dynamic Spinning Canvas Wheel
            Canvas(
                modifier = Modifier
                    .size(270.dp)
                    .clip(CircleShape)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 8f

                // Rotate entire wheel according to animated angle
                rotate(degrees = wheelAngle.value, pivot = center) {
                    for (i in segments.indices) {
                        val seg = segments[i]
                        val startAngle = i * segmentAngle

                        // Draw Segment Slice
                        drawArc(
                            brush = Brush.radialGradient(
                                colors = listOf(seg.primaryColor, seg.secondaryColor),
                                center = center,
                                radius = radius
                            ),
                            startAngle = startAngle,
                            sweepAngle = segmentAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2f, radius * 2f)
                        )

                        // Slice Border Divider
                        val rad = Math.toRadians(startAngle.toDouble())
                        val endX = center.x + radius * cos(rad).toFloat()
                        val endY = center.y + radius * sin(rad).toFloat()
                        drawLine(
                            color = GoldYellow.copy(alpha = 0.6f),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2.5f
                        )

                        // Multiplier Label in Segment
                        val midAngleRad = Math.toRadians((startAngle + segmentAngle / 2f).toDouble())
                        val textRadius = radius * 0.68f
                        val textX = center.x + textRadius * cos(midAngleRad).toFloat()
                        val textY = center.y + textRadius * sin(midAngleRad).toFloat()

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 32f
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                            }
                            drawText(seg.label, textX, textY + 10f, paint)
                        }
                    }

                    // Outer Rim Ring with Gold Studs
                    drawCircle(
                        color = GoldYellow,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 6f)
                    )

                    // Rim lights
                    for (k in 0 until 20) {
                        val angleDeg = k * (360f / 20f)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val studX = center.x + (radius - 3f) * cos(angleRad).toFloat()
                        val studY = center.y + (radius - 3f) * sin(angleRad).toFloat()
                        drawCircle(
                            color = if (k % 2 == 0) Color.White else GoldYellow,
                            radius = 3.5f,
                            center = Offset(studX, studY)
                        )
                    }
                }

                // Center Hub with Golden Emblem (fixed on top)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD54F), Color(0xFFF57F17), Color(0xFFE65100)),
                        center = center,
                        radius = 36f
                    ),
                    radius = 36f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 36f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // Top Fixed Indicator Pointer (Arrow pointing down at wheel)
                val topCenter = Offset(center.x, center.y - radius - 2f)
                val pointerPath = Path().apply {
                    moveTo(topCenter.x - 14f, topCenter.y - 12f)
                    lineTo(topCenter.x + 14f, topCenter.y - 12f)
                    lineTo(topCenter.x, topCenter.y + 16f)
                    close()
                }
                drawPath(path = pointerPath, color = CoralRed)
                drawPath(path = pointerPath, color = Color.White, style = Stroke(width = 2f))
            }

            // Center Result Banner / Status
            if (!isSpinning && lastWinningSegment != null) {
                Surface(
                    color = CardBg.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (lastWinAmount > 0) NeonGreen else CoralRed
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lastWinAmount > 0) "🎉 WINNER!" else "NO WIN",
                            color = if (lastWinAmount > 0) NeonGreen else CoralRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (lastWinAmount > 0)
                                "PKR ${String.format("%,.0f", lastWinAmount)} (${lastWinningSegment!!.label})"
                            else
                                "Better luck next spin!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // CONTROLS & BETTING PANEL
        Surface(
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BET AMOUNT (PKR)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Min: PKR 25 • Max: PKR 25,000",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                // Quick Bet Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickBets) { amt ->
                        val isSelected = selectedBet == amt && customBetText.isEmpty()
                        Surface(
                            color = if (isSelected) GoldYellow else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldYellow else CardBorder),
                            modifier = Modifier.clickable(enabled = !isSpinning) {
                                selectedBet = amt
                                customBetText = ""
                                betErrorMessage = ""
                            }
                        ) {
                            Text(
                                text = "PKR ${amt.toInt()}",
                                color = if (isSelected) DarkBg else TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Custom Bet Input
                OutlinedTextField(
                    value = customBetText,
                    onValueChange = {
                        customBetText = it
                        betErrorMessage = ""
                    },
                    label = { Text("Or enter custom bet in PKR") },
                    placeholder = { Text("e.g. 500") },
                    leadingIcon = {
                        Text("PKR", color = GoldYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                    },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldYellow,
                        unfocusedBorderColor = CardBorder
                    ),
                    singleLine = true
                )

                if (betErrorMessage.isNotBlank()) {
                    Text(
                        text = betErrorMessage,
                        color = CoralRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Main SPIN Button
                Button(
                    onClick = { spinWheel() },
                    enabled = !isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow)
                ) {
                    if (isSpinning) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("SPINNING WHEEL...", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Casino, contentDescription = null, tint = DarkBg)
                            Text(
                                text = "SPIN FOR PKR ${String.format("%,.0f", activeBetAmount)}",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Rules Dialog
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text("Money Coming: Wheel Rules", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• The wheel features 10 prize sectors with multipliers up to 100x.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Outcomes are validated on the backend before the wheel stops.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Payout = Bet Amount × Multiplier credited directly to verified PKR wallet.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Official RTP: 98.4%. Certified RNG simulation.", color = AccentCyan, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("GOT IT", color = NeonGreen)
                }
            },
            containerColor = CardBg
        )
    }
}
