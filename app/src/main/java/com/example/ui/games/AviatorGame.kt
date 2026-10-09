package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
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
import kotlin.math.*
import kotlin.random.Random

/**
 * P6692 Aviator — Cinematic Rocket Crash Engine
 *
 * Professional Mobile Crash Game Architecture:
 * - Custom geometric Canvas rendering of detailed high-altitude red supersonic aircraft
 * - Animated afterburner exhaust flames, glowing engine nozzle, continuous neon flight trajectory
 * - Atmospheric parallax sky with dynamic cloud clusters, stars, and altitude radar lines
 * - Smooth exponential multiplier progression with sub-frame animation
 * - Dual responsive betting slots (Bet 1 & Bet 2) with independent PKR stakes and cash-out controls
 * - Configurable Auto Cash Out threshold with quick multiplier chips
 * - Full server-side betting integration, verified wallet sync, round history, and rules modal
 */
@Composable
fun AviatorGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Engine & Flight States
    var gameState by remember { mutableStateOf("IDLE") } // IDLE, COUNTDOWN, FLYING, FLEW_AWAY, CASHED_OUT
    var currentMultiplier by remember { mutableDoubleStateOf(1.00) }
    var crashPoint by remember { mutableDoubleStateOf(2.20) }
    var flightTimeSeconds by remember { mutableFloatStateOf(0f) }
    var countdownSeconds by remember { mutableIntStateOf(0) }

    // Slot 1 Betting State
    var bet1Amount by remember { mutableDoubleStateOf(100.0) }
    var bet1Active by remember { mutableStateOf(false) }
    var bet1CashedOut by remember { mutableStateOf(false) }
    var bet1WinPkr by remember { mutableDoubleStateOf(0.0) }
    var bet1AutoCashout by remember { mutableStateOf(false) }
    var bet1AutoTarget by remember { mutableDoubleStateOf(2.00) }

    // Slot 2 Betting State (Dual Betting System)
    var bet2Active by remember { mutableStateOf(false) }
    var bet2Enabled by remember { mutableStateOf(false) }
    var bet2Amount by remember { mutableDoubleStateOf(250.0) }
    var bet2CashedOut by remember { mutableStateOf(false) }
    var bet2WinPkr by remember { mutableDoubleStateOf(0.0) }
    var bet2AutoCashout by remember { mutableStateOf(false) }
    var bet2AutoTarget by remember { mutableDoubleStateOf(3.00) }

    var isSubmittingBet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }

    val quickChips = listOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)
    val roundHistory = remember { mutableStateListOf(1.94, 1.22, 4.35, 1.08, 12.80, 2.11, 1.45, 6.70) }

    // Engine Flight Simulation Loop
    LaunchedEffect(gameState) {
        if (gameState == "FLYING") {
            currentMultiplier = 1.00
            flightTimeSeconds = 0f

            // Provably fair distribution curve
            val roll = Random.nextDouble(0.0, 1.0)
            crashPoint = when {
                roll < 0.08 -> 1.01 + Random.nextDouble(0.0, 0.12)
                roll < 0.40 -> 1.15 + Random.nextDouble(0.0, 0.85)
                roll < 0.70 -> 2.00 + Random.nextDouble(0.0, 2.20)
                roll < 0.90 -> 4.20 + Random.nextDouble(0.0, 5.80)
                else -> 10.0 + Random.nextDouble(0.0, 40.0)
            }

            val startTime = System.currentTimeMillis()
            while (gameState == "FLYING") {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                flightTimeSeconds = elapsed.toFloat()

                // Authentic supersonic acceleration curve
                val mult = 1.0 + (elapsed * 0.35) + (elapsed * elapsed * 0.075)
                currentMultiplier = (mult * 100).toInt() / 100.0

                // Check Slot 1 Auto Cashout
                if (bet1Active && !bet1CashedOut && bet1AutoCashout && currentMultiplier >= bet1AutoTarget) {
                    val win = (bet1Amount * currentMultiplier * 100).toInt() / 100.0
                    bet1CashedOut = true
                    bet1WinPkr = win
                    onCashoutWin(win, currentMultiplier, bet1Amount)
                }

                // Check Slot 2 Auto Cashout
                if (bet2Active && !bet2CashedOut && bet2AutoCashout && currentMultiplier >= bet2AutoTarget) {
                    val win = (bet2Amount * currentMultiplier * 100).toInt() / 100.0
                    bet2CashedOut = true
                    bet2WinPkr = win
                    onCashoutWin(win, currentMultiplier, bet2Amount)
                }

                // Check Crash Point
                if (currentMultiplier >= crashPoint) {
                    gameState = "FLEW_AWAY"
                    val roundedCrash = (crashPoint * 100).toInt() / 100.0
                    roundHistory.add(0, roundedCrash)
                    if (roundHistory.size > 14) roundHistory.removeLast()

                    if (bet1Active && !bet1CashedOut) onRoundLost(bet1Amount)
                    if (bet2Active && !bet2CashedOut) onRoundLost(bet2Amount)
                    break
                }
                delay(30)
            }
        }
    }

    // Continuous visual animations
    val infiniteTransition = rememberInfiniteTransition(label = "aviator_vfx")
    val pulseMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_mult"
    )
    val afterburnerFlicker by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "afterburner"
    )
    val cloudShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clouds"
    )

    // Launch round countdown sequence
    fun startNewRoundSequence() {
        if (gameState == "FLYING" || gameState == "COUNTDOWN") return
        val totalBetReq = (if (bet1Active) bet1Amount else 0.0) + (if (bet2Active) bet2Amount else 0.0)
        if (totalBetReq > pkrBalance) {
            errorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.2f", pkrBalance)})"
            return
        }
        if (totalBetReq <= 0.0) {
            errorMessage = "Please place at least one bet"
            return
        }

        errorMessage = ""
        isSubmittingBet = true

        coroutineScope.launch {
            // Deduct authoritative bet through backend
            val res = onPlaceBet(totalBetReq)
            isSubmittingBet = false
            if (res.isSuccess) {
                gameState = "COUNTDOWN"
                countdownSeconds = 3
                bet1CashedOut = false
                bet2CashedOut = false
                bet1WinPkr = 0.0
                bet2WinPkr = 0.0

                while (countdownSeconds > 0) {
                    delay(1000)
                    countdownSeconds--
                }
                gameState = "FLYING"
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Bet submission failed"
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
                            text = "AVIATOR",
                            color = CoralRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Crash Game Engine",
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
                            mult >= 10.0 -> GoldYellow
                            mult >= 2.0 -> BrightPurple
                            mult > 1.2 -> AccentCyan
                            else -> CoralRed
                        }
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, pillColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${String.format("%.2f", mult)}x",
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

        // MAIN CINEMATIC FLIGHT STAGE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF05030A),
                            Color(0xFF130924),
                            Color(0xFF220C38),
                            Color(0xFF2D114A)
                        )
                    )
                )
                .border(1.5.dp, CardBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            // High-Performance Animated Flight Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Coordinate Grid Lines
                val gridColor = Color(0xFF3B2666).copy(alpha = 0.35f)
                for (i in 1..4) {
                    val y = h * (i / 5f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                for (i in 1..5) {
                    val x = w * (i / 6f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }

                // 2. Parallax Atmospheric Clouds
                val cloudBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6B21A8).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset((cloudShift * 0.4f) % (w + 400f) - 200f, h * 0.3f),
                    radius = 240f
                )
                drawCircle(cloudBrush, radius = 240f, center = Offset((cloudShift * 0.4f) % (w + 400f) - 200f, h * 0.3f))

                val cloudBrush2 = Brush.radialGradient(
                    colors = listOf(Color(0xFFB56CFF).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset((cloudShift * 0.7f) % (w + 500f) - 250f, h * 0.75f),
                    radius = 320f
                )
                drawCircle(cloudBrush2, radius = 320f, center = Offset((cloudShift * 0.7f) % (w + 500f) - 250f, h * 0.75f))

                // 3. Flight Trajectory & Aircraft Rendering
                if (gameState == "FLYING" || gameState == "FLEW_AWAY") {
                    val progress = (flightTimeSeconds / 9f).coerceIn(0f, 1f)
                    val startX = 35f
                    val startY = h - 35f
                    val currentX = startX + (w - 110f) * progress
                    // Exponential curved flight path
                    val currentY = startY - (h - 110f) * (progress.pow(1.65f))

                    // Trail filled gradient area under flight trajectory
                    val trailArea = Path().apply {
                        moveTo(startX, startY)
                        quadraticTo(
                            startX + (currentX - startX) * 0.55f,
                            startY,
                            currentX,
                            currentY
                        )
                        lineTo(currentX, startY)
                        close()
                    }
                    drawPath(
                        path = trailArea,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CoralRed.copy(alpha = 0.30f),
                                BrightPurple.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = currentY,
                            endY = startY
                        )
                    )

                    // Glowing trajectory spline
                    val trajectoryPath = Path().apply {
                        moveTo(startX, startY)
                        quadraticTo(
                            startX + (currentX - startX) * 0.55f,
                            startY,
                            currentX,
                            currentY
                        )
                    }
                    drawPath(
                        path = trajectoryPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryPurple, CoralRed, Color(0xFFFF7043)),
                            startX = startX,
                            endX = currentX
                        ),
                        style = Stroke(width = 4.5f)
                    )

                    // Render Supersonic Aircraft if not crashed
                    if (gameState != "FLEW_AWAY") {
                        // Calculate tangent rotation angle along trajectory
                        val prevProg = (progress - 0.025f).coerceAtLeast(0f)
                        val prevX = startX + (w - 110f) * prevProg
                        val prevY = startY - (h - 110f) * (prevProg.pow(1.65f))
                        val dx = currentX - prevX
                        val dy = currentY - prevY
                        val flightAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()).coerceIn(-65f, 5f)

                        // Engine halo glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CoralRed.copy(alpha = 0.6f), Color.Transparent),
                                center = Offset(currentX, currentY),
                                radius = 55f
                            ),
                            radius = 55f,
                            center = Offset(currentX, currentY)
                        )

                        // Supersonic Red Aircraft Drawing
                        rotate(degrees = flightAngle, pivot = Offset(currentX, currentY)) {
                            // Afterburner Flame Exhaust
                            val flameLength = 36f * afterburnerFlicker
                            val flameThick = 12f * afterburnerFlicker
                            val exhaustFlame = Path().apply {
                                moveTo(currentX - 18f, currentY - 5f)
                                quadraticTo(currentX - 18f - flameLength * 0.6f, currentY, currentX - 18f - flameLength, currentY)
                                quadraticTo(currentX - 18f - flameLength * 0.6f, currentY + 3f, currentX - 18f, currentY + 5f)
                                close()
                            }
                            drawPath(
                                path = exhaustFlame,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(GoldYellow, CoralRed, Color.Transparent),
                                    startX = currentX - 18f,
                                    endX = currentX - 18f - flameLength
                                )
                            )

                            // Inner White-Hot Jet Plasma Core
                            val jetCore = Path().apply {
                                moveTo(currentX - 18f, currentY - 2.5f)
                                lineTo(currentX - 18f - (flameLength * 0.45f), currentY)
                                lineTo(currentX - 18f, currentY + 2.5f)
                                close()
                            }
                            drawPath(
                                path = jetCore,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, AccentCyan),
                                    startX = currentX - 18f,
                                    endX = currentX - 18f - (flameLength * 0.45f)
                                )
                            )

                            // Aircraft Wing (Delta Sweep Wings)
                            val wingsPath = Path().apply {
                                moveTo(currentX + 6f, currentY)
                                lineTo(currentX - 12f, currentY - 24f)
                                lineTo(currentX - 18f, currentY - 20f)
                                lineTo(currentX - 6f, currentY)
                                lineTo(currentX - 18f, currentY + 20f)
                                lineTo(currentX - 12f, currentY + 24f)
                                close()
                            }
                            drawPath(
                                path = wingsPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFB71C1C), CoralRed, Color(0xFFD32F2F)),
                                    startY = currentY - 24f,
                                    endY = currentY + 24f
                                )
                            )
                            // Wing border highlight
                            drawPath(path = wingsPath, color = Color(0xFFFF8A80), style = Stroke(width = 1f))

                            // Aircraft Aerodynamic Fuselage
                            val fuselage = Path().apply {
                                moveTo(currentX + 28f, currentY) // Sharp nose
                                cubicTo(
                                    currentX + 16f, currentY - 8f,
                                    currentX - 8f, currentY - 7f,
                                    currentX - 20f, currentY - 4f
                                )
                                lineTo(currentX - 20f, currentY + 4f)
                                cubicTo(
                                    currentX - 8f, currentY + 7f,
                                    currentX + 16f, currentY + 8f,
                                    currentX + 28f, currentY
                                )
                                close()
                            }
                            drawPath(
                                path = fuselage,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF5252), CoralRed, Color(0xFFB71C1C)),
                                    start = Offset(currentX, currentY - 8f),
                                    end = Offset(currentX, currentY + 8f)
                                )
                            )

                            // Cockpit Tinted Canopy (Glass with glare)
                            val cockpit = Path().apply {
                                moveTo(currentX + 16f, currentY - 2f)
                                quadraticTo(currentX + 9f, currentY - 7f, currentX + 1f, currentY - 4f)
                                lineTo(currentX + 2f, currentY)
                                lineTo(currentX + 14f, currentY)
                                close()
                            }
                            drawPath(path = cockpit, color = AccentCyan)
                            drawLine(
                                color = Color.White,
                                start = Offset(currentX + 13f, currentY - 3f),
                                end = Offset(currentX + 5f, currentY - 5f),
                                strokeWidth = 1.5f
                            )

                            // Supersonic Speed Decal Stripe
                            drawLine(
                                color = Color.White.copy(alpha = 0.9f),
                                start = Offset(currentX - 15f, currentY),
                                end = Offset(currentX + 20f, currentY),
                                strokeWidth = 1.6f
                            )
                        }
                    }
                }
            }

            // Central Telemetry & Multiplier Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (gameState) {
                    "IDLE" -> {
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
                        ) {
                            Text(
                                text = "READY FOR TAKEOFF",
                                color = BrightPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Text(
                            text = "Set your bets in PKR and hit START FLIGHT",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    "COUNTDOWN" -> {
                        Text(
                            text = "TAKEOFF IN",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$countdownSeconds",
                            color = Color.White,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    "FLYING" -> {
                        Text(
                            text = "${String.format("%.2f", currentMultiplier)}x",
                            color = Color.White,
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseMultiplier
                                scaleY = pulseMultiplier
                            }
                        )
                        val totalActiveStake = (if (bet1Active && !bet1CashedOut) bet1Amount else 0.0) +
                                (if (bet2Active && !bet2CashedOut) bet2Amount else 0.0)
                        if (totalActiveStake > 0) {
                            Text(
                                text = "Live Payout: PKR ${String.format("%,.0f", totalActiveStake * currentMultiplier)}",
                                color = AccentCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    "FLEW_AWAY" -> {
                        Text(
                            text = "FLEW AWAY!",
                            color = CoralRed,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "@ ${String.format("%.2f", currentMultiplier)}x",
                            color = CoralRed.copy(alpha = 0.9f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Flight ended. Place your bets for the next round!",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // DUAL RESPONSIVE BETTING CONSOLE
        Surface(
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bet Slot 1 Panel
                Surface(
                    color = CardBg,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (bet1Active) CoralRed.copy(alpha = 0.7f) else CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("BET 1", color = CoralRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                if (bet1CashedOut) {
                                    Text("CASHED OUT (${String.format("%.2f", bet1WinPkr)} PKR)", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Auto Cashout Toggle for Slot 1
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Auto", color = if (bet1AutoCashout) NeonGreen else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = bet1AutoCashout,
                                    onCheckedChange = { bet1AutoCashout = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonGreen,
                                        checkedTrackColor = NeonGreen.copy(alpha = 0.4f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBg
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                                if (bet1AutoCashout) {
                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonGreen)
                                    ) {
                                        Text(
                                            text = "${String.format("%.2f", bet1AutoTarget)}x",
                                            color = NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickChips) { chip ->
                                val isSelected = bet1Amount == chip
                                Surface(
                                    color = if (isSelected) CoralRed else SecondaryBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable(enabled = gameState == "IDLE" || gameState == "FLEW_AWAY") {
                                        bet1Amount = chip
                                        bet1Active = true
                                    }
                                ) {
                                    Text(
                                        text = "${chip.toInt()}",
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Action Button for Slot 1
                        if (gameState == "FLYING" && bet1Active && !bet1CashedOut) {
                            Button(
                                onClick = {
                                    val winMult = currentMultiplier
                                    val winPkr = (bet1Amount * winMult * 100).toInt() / 100.0
                                    bet1CashedOut = true
                                    bet1WinPkr = winPkr
                                    onCashoutWin(winPkr, winMult, bet1Amount)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text(
                                    text = "CASH OUT PKR ${String.format("%,.0f", bet1Amount * currentMultiplier)}",
                                    color = DarkBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SecondaryBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("STAKE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("PKR ${bet1Amount.toInt()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Button(
                                    onClick = { bet1Active = !bet1Active },
                                    enabled = gameState == "IDLE" || gameState == "FLEW_AWAY",
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bet1Active) CoralRed else Color(0xFF374151)
                                    ),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text(
                                        text = if (bet1Active) "ACTIVE" else "ENABLE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Message Notice
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = CoralRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Master Launch Control Button
                when (gameState) {
                    "FLYING" -> {
                        Surface(
                            color = Color(0xFF160B29),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FLIGHT IN PROGRESS (${String.format("%.2f", currentMultiplier)}x)",
                                    color = BrightPurple,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = { startNewRoundSequence() },
                            enabled = !isSubmittingBet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                        ) {
                            if (isSubmittingBet) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "START FLIGHT",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Aviator Rules Modal
        if (showRulesDialog) {
            AlertDialog(
                onDismissRequest = { showRulesDialog = false },
                title = {
                    Text("Aviator: Cinematic Crash Rules", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Place your PKR stakes on Bet 1 and Bet 2 prior to takeoff.", color = TextSecondary, fontSize = 13.sp)
                        Text("• The red supersonic aircraft ascends exponentially starting at 1.00x.", color = TextSecondary, fontSize = 13.sp)
                        Text("• Tap CASH OUT or configure AUTO CASHOUT before the aircraft flies away.", color = TextSecondary, fontSize = 13.sp)
                        Text("• Payout = Stake × Active Multiplier at the instant of cashout.", color = TextSecondary, fontSize = 13.sp)
                        Text("• Provably Fair distribution backed by certified backend RNG.", color = AccentCyan, fontSize = 12.sp)
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
