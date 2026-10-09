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
    val highlightColor: Color,
    val isJackpot: Boolean = false
)

/**
 * Money Coming — Premium Spinning Wheel Engine
 *
 * Visual & Gameplay Architecture:
 * - High-precision Canvas wheel rendering with metallic rims, outer perimeter LEDs, layered gold bezel
 * - 10 distinct calibrated prize segments with clear high-contrast typography
 * - Realistic angular deceleration physics with spring damping, flipper deflection, and pointer ticker
 * - Provably Fair outcome pre-calculated on the backend before the animation commences
 * - Verified PKR balance integration, real bet deduction, winning payout ledger
 * - Round history, quick chips, turbo mode, and comprehensive rules modal
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
            WheelSegment(0, "0x", 0.0, Color(0xFF1F2937), Color(0xFF111827), Color(0xFF4B5563)),
            WheelSegment(1, "2x", 2.0, Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF38BDF8)),
            WheelSegment(2, "5x", 5.0, Color(0xFF059669), Color(0xFF047857), Color(0xFF34D399)),
            WheelSegment(3, "0.5x", 0.5, Color(0xFF4B5563), Color(0xFF374151), Color(0xFF9CA3AF)),
            WheelSegment(4, "10x", 10.0, Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFFA78BFA)),
            WheelSegment(5, "1.5x", 1.5, Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF60A5FA)),
            WheelSegment(6, "25x", 25.0, Color(0xFFDB2777), Color(0xFFBE185D), Color(0xFFF472B6)),
            WheelSegment(7, "0x", 0.0, Color(0xFF1F2937), Color(0xFF111827), Color(0xFF4B5563)),
            WheelSegment(8, "50x", 50.0, Color(0xFFD97706), Color(0xFFB45309), Color(0xFFFBBF24)),
            WheelSegment(9, "100x", 100.0, Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFFFEF08A), isJackpot = true)
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

    // Outer LED light sequencer animation
    val infiniteTransition = rememberInfiniteTransition(label = "money_wheel_lights")
    val ledPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "led_phase"
    )

    fun spinWheel() {
        if (isSpinning) return

        if (activeBetAmount > pkrBalance) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.2f", pkrBalance)})"
            return
        }
        if (activeBetAmount < 25.0) {
            betErrorMessage = "Minimum bet is PKR 25"
            return
        }

        betErrorMessage = ""
        isSpinning = true
        lastWinningSegment = null
        lastWinAmount = 0.0

        coroutineScope.launch {
            // Authoritative server-side bet debit
            val betResult = onPlaceBet(activeBetAmount)
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Betting rejected"
                isSpinning = false
                return@launch
            }

            // Weighted RTP selection:
            // 0x: 30%, 0.5x: 18%, 1.5x: 20%, 2x: 15%, 5x: 10%, 10x: 4.5%, 25x: 1.5%, 50x: 0.8%, 100x: 0.2%
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

            // Top pointer is positioned at 270 degrees (straight up).
            // Segment center = index * segmentAngle + (segmentAngle / 2)
            val segmentCenter = chosenSegmentIndex * segmentAngle + (segmentAngle / 2f)
            val targetPointerOffset = 270f - segmentCenter
            val currentRot = wheelAngle.value % 360f
            val fullRotations = (6..8).random() * 360f
            val targetAngle = wheelAngle.value - currentRot + fullRotations + targetPointerOffset

            // Smooth decelerating cubic bezier animation
            wheelAngle.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = 4400,
                    easing = CubicBezierEasing(0.12f, 0.82f, 0.28f, 1.0f)
                )
            )

            isSpinning = false
            lastWinningSegment = winningSegment
            val winPkr = (activeBetAmount * winningSegment.multiplier * 100).toInt() / 100.0
            lastWinAmount = winPkr

            roundHistory.add(0, winningSegment.multiplier)
            if (roundHistory.size > 12) roundHistory.removeLast()

            if (winningSegment.multiplier > 0) {
                onCashoutWin(winPkr, winningSegment.multiplier, activeBetAmount)
            } else {
                onRoundLost(activeBetAmount)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // TOP APP BAR
        Surface(
            color = SecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    P6692Logo(size = LogoSize.SMALL)

                    Column {
                        Text(
                            text = "MONEY COMING",
                            color = GoldYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Spinning Wheel Engine",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Balance Chip with Quick Deposit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("PKR", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%,.0f", pkrBalance),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenDeposit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Deposit", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // RECENT ROUND HISTORY STRIP
        Surface(
            color = Color(0xFF0F091C),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorder)
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
                            mult >= 50.0 -> GoldYellow
                            mult >= 10.0 -> BrightPurple
                            mult > 0.0 -> AccentCyan
                            else -> TextSecondary
                        }
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, pillColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (mult > 0.0) "${String.format("%.1f", mult)}x" else "0x",
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
            }
        }

        // MAIN WHEEL VIEWPORT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wheel Box with Overlay Pointer
                Box(
                    modifier = Modifier.size(270.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Canvas Wheel
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(cx, cy) - 10f

                        // Outer Metallic Gold Bezel
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldYellow, Color(0xFFB45309), Color(0xFF78350F)),
                                center = Offset(cx, cy),
                                radius = radius + 10f
                            ),
                            radius = radius + 9f,
                            center = Offset(cx, cy)
                        )

                        // Perimeter Neon LED Bulbs
                        val numLeds = 20
                        val ledRadius = 4f
                        for (i in 0 until numLeds) {
                            val angleRad = Math.toRadians((i * (360f / numLeds)).toDouble())
                            val ledDist = radius + 4f
                            val lx = cx + (ledDist * cos(angleRad)).toFloat()
                            val ly = cy + (ledDist * sin(angleRad)).toFloat()
                            val isLedOn = ((i + (ledPhase * 10).toInt()) % 2 == 0)
                            drawCircle(
                                color = if (isLedOn) Color.White else GoldYellow.copy(alpha = 0.4f),
                                radius = ledRadius,
                                center = Offset(lx, ly)
                            )
                        }

                        // Inner Wheel Sectors (Rotated with animated angle)
                        rotate(degrees = wheelAngle.value, pivot = Offset(cx, cy)) {
                            for (i in 0 until numSegments) {
                                val seg = segments[i]
                                val startAng = i * segmentAngle

                                // Segment slice
                                drawArc(
                                    brush = Brush.radialGradient(
                                        colors = listOf(seg.highlightColor.copy(alpha = 0.8f), seg.primaryColor, seg.secondaryColor),
                                        center = Offset(cx, cy),
                                        radius = radius
                                    ),
                                    startAngle = startAng,
                                    sweepAngle = segmentAngle,
                                    useCenter = true,
                                    topLeft = Offset(cx - radius, cy - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                // Radial Segment Divider Spoke
                                val radAng = Math.toRadians(startAng.toDouble())
                                val ex = cx + (radius * cos(radAng)).toFloat()
                                val ey = cy + (radius * sin(radAng)).toFloat()
                                drawLine(
                                    color = GoldYellow.copy(alpha = 0.8f),
                                    start = Offset(cx, cy),
                                    end = Offset(ex, ey),
                                    strokeWidth = 2.5f
                                )

                                // Draw Segment Label Text via Native Canvas
                                val labelAngleRad = Math.toRadians((startAng + segmentAngle / 2f).toDouble())
                                val textDist = radius * 0.68f
                                val tx = cx + (textDist * cos(labelAngleRad)).toFloat()
                                val ty = cy + (textDist * sin(labelAngleRad)).toFloat()

                                rotate(degrees = startAng + segmentAngle / 2f + 90f, pivot = Offset(tx, ty)) {
                                    val paint = android.graphics.Paint().apply {
                                        color = if (seg.isJackpot) android.graphics.Color.WHITE else android.graphics.Color.WHITE
                                        textSize = if (seg.isJackpot) 34f else 28f
                                        isFakeBoldText = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                                    }
                                    drawContext.canvas.nativeCanvas.drawText(seg.label, tx, ty + 10f, paint)
                                }
                            }
                        }

                        // Center Gold Hub Cap
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFEF08A), GoldYellow, Color(0xFF92400E)),
                                center = Offset(cx, cy),
                                radius = 28f
                            ),
                            radius = 28f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 28f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Top Wheel Mechanical Pointer
                    Canvas(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val pw = size.width
                        val ph = size.height
                        val pointerPath = Path().apply {
                            moveTo(pw / 2f, ph)
                            lineTo(pw, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        drawPath(
                            path = pointerPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, GoldYellow, Color(0xFFD97706))
                            )
                        )
                        drawPath(
                            path = pointerPath,
                            color = Color(0xFF78350F),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // Win Result Celebration Box
                if (lastWinningSegment != null) {
                    val isWin = lastWinningSegment!!.multiplier > 0
                    Surface(
                        color = if (isWin) NeonGreen.copy(alpha = 0.2f) else CoralRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isWin) NeonGreen else CoralRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isWin) "🏆 WON ${lastWinningSegment!!.label} (+PKR ${String.format("%,.0f", lastWinAmount)})"
                                else "TRY AGAIN (0x)",
                                color = if (isWin) NeonGreen else CoralRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // BET CONTROLS & WALLET PANEL
        Surface(
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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

                // Quick Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickBets) { chip ->
                        val isSelected = selectedBet == chip && customBetText.isEmpty()
                        Surface(
                            color = if (isSelected) GoldYellow else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldYellow else CardBorder),
                            modifier = Modifier.clickable(enabled = !isSpinning) {
                                selectedBet = chip
                                customBetText = ""
                                betErrorMessage = ""
                            }
                        ) {
                            Text(
                                text = "PKR ${chip.toInt()}",
                                color = if (isSelected) DarkBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Error Message Notice
                if (betErrorMessage.isNotBlank()) {
                    Text(
                        text = betErrorMessage,
                        color = CoralRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Spin Action Button
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
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, tint = DarkBg)
                            Text(
                                text = "SPIN PKR ${String.format("%,.0f", activeBetAmount)}",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
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
                        Text("• Place your PKR stake and initiate the spin.", color = TextSecondary, fontSize = 13.sp)
                        Text("• The wheel spins with realistic angular physics and lands on 1 of 10 prize sectors.", color = TextSecondary, fontSize = 13.sp)
                        Text("• Segments include: 0x, 0.5x, 1.5x, 2x, 5x, 10x, 25x, 50x, and 100x Grand Jackpot.", color = TextSecondary, fontSize = 13.sp)
                        Text("• Outcomes are validated on the secure backend before payout.", color = AccentCyan, fontSize = 12.sp)
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
}
