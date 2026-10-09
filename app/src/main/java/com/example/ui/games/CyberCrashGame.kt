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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.random.Random

@Composable
fun CyberCrashGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var gameState by remember { mutableStateOf("IDLE") } // IDLE, FLYING, CRASHED, BAILED
    var currentMultiplier by remember { mutableDoubleStateOf(1.00) }
    var crashTarget by remember { mutableDoubleStateOf(2.50) }
    var selectedBet by remember { mutableDoubleStateOf(100.0) }
    var customBetText by remember { mutableStateOf("") }
    var bailedMultiplier by remember { mutableDoubleStateOf(0.0) }
    var flightProgress by remember { mutableFloatStateOf(0f) }
    var isPlacingBet by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }
    val recentHistory = remember { mutableStateListOf(1.85, 3.42, 1.20, 8.90, 2.15, 1.05, 4.60) }

    val activeBetAmount = remember(selectedBet, customBetText) {
        val parsed = customBetText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else selectedBet
    }

    // Multiplier ticker coroutine
    LaunchedEffect(gameState) {
        if (gameState == "FLYING") {
            currentMultiplier = 1.00
            flightProgress = 0f
            // Generate realistic randomized crash point
            val roll = Random.nextDouble(0.0, 1.0)
            crashTarget = when {
                roll < 0.12 -> 1.05 + Random.nextDouble(0.0, 0.25)
                roll < 0.50 -> 1.30 + Random.nextDouble(0.0, 1.4)
                roll < 0.82 -> 2.60 + Random.nextDouble(0.0, 3.2)
                roll < 0.95 -> 5.80 + Random.nextDouble(0.0, 8.0)
                else -> 15.0 + Random.nextDouble(0.0, 30.0)
            }

            val startTime = System.currentTimeMillis()
            while (gameState == "FLYING") {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val mult = 1.0 + (elapsed * 0.4) + (elapsed * elapsed * 0.12)
                currentMultiplier = mult
                flightProgress = (elapsed.toFloat() / 15f).coerceIn(0f, 1f)

                if (mult >= crashTarget) {
                    gameState = "CRASHED"
                    recentHistory.add(0, (mult * 100).toInt() / 100.0)
                    if (recentHistory.size > 10) recentHistory.removeLast()
                    onRoundLost(activeBetAmount)
                    break
                }
                delay(35)
            }
        }
    }

    // Cyber animation loops
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_effects")
    val cyberPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyber_pulse"
    )
    val warpGlow by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warp_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Game Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                Column {
                    Text(
                        text = "Cyber Crash: Rocket 92",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Real PKR Betting Engine",
                        color = NeonGreen,
                        fontSize = 11.sp
                    )
                }
            }

            // Real PKR Wallet Balance Pill
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
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Deposit",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent Multiplier History Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text(
                    text = "ROUNDS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
            items(recentHistory) { hist ->
                val isHigh = hist >= 2.00
                Surface(
                    color = if (isHigh) PrimaryPurple.copy(alpha = 0.3f) else SecondaryBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isHigh) BrightPurple else CardBorder
                    )
                ) {
                    Text(
                        text = String.format("%.2fx", hist),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHigh) AccentCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Center Rocket Arena Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0B1E),
                            Color(0xFF0A0714)
                        )
                    )
                )
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Animated Flight Arc Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid background lines
                val gridColor = Color(0xFF1D1438)
                for (i in 1..5) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * i / 6),
                        end = Offset(w, h * i / 6),
                        strokeWidth = 1f
                    )
                }

                if (gameState == "FLYING" || gameState == "BAILED" || gameState == "CRASHED") {
                    val path = Path()
                    path.moveTo(w * 0.08f, h * 0.90f)

                    val endX = w * 0.08f + (w * 0.80f * flightProgress)
                    val endY = h * 0.90f - (h * 0.70f * flightProgress)

                    path.quadraticTo(
                        w * 0.40f * flightProgress,
                        h * 0.90f,
                        endX,
                        endY
                    )

                    val arcColor = when (gameState) {
                        "FLYING" -> AccentCyan
                        "BAILED" -> NeonGreen
                        else -> CoralRed
                    }

                    drawPath(
                        path = path,
                        color = arcColor,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Gradient glowing area under trajectory
                    val fillPath = Path().apply {
                        moveTo(w * 0.08f, h * 0.90f)
                        quadraticTo(
                            w * 0.40f * flightProgress,
                            h * 0.90f,
                            endX,
                            endY
                        )
                        lineTo(endX, h * 0.90f)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(arcColor.copy(alpha = 0.22f), Color.Transparent),
                            startY = endY,
                            endY = h * 0.90f
                        )
                    )

                    // CYBER ROCKET / WARP SHIP ANIMATION VISUAL
                    if (gameState == "FLYING" || gameState == "BAILED") {
                        // Calculate heading angle
                        val prevProg = (flightProgress - 0.03f).coerceAtLeast(0f)
                        val prevX = w * 0.08f + (w * 0.80f * prevProg)
                        val prevY = h * 0.90f - (h * 0.70f * prevProg)
                        val dx = endX - prevX
                        val dy = endY - prevY
                        val flightAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()).coerceIn(-65f, 10f)

                        // Plasma Halo / Shield Field
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentCyan.copy(alpha = 0.6f), PrimaryPurple.copy(alpha = 0.2f), Color.Transparent),
                                center = Offset(endX, endY),
                                radius = warpGlow + 20f
                            ),
                            radius = warpGlow + 20f,
                            center = Offset(endX, endY)
                        )

                        // Draw Cyber Rocket 92 Starship
                        rotate(degrees = flightAngle, pivot = Offset(endX, endY)) {
                            // 1. Dual Plasma Exhaust Thrusters
                            val exhaustLen = 30f * cyberPulse
                            val flameTop = Path().apply {
                                moveTo(endX - 16f, endY - 6f)
                                quadraticTo(endX - 16f - exhaustLen * 0.7f, endY - 6f, endX - 16f - exhaustLen, endY - 6f)
                                quadraticTo(endX - 16f - exhaustLen * 0.5f, endY - 3f, endX - 16f, endY - 3f)
                                close()
                            }
                            drawPath(
                                path = flameTop,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AccentCyan, NeonGreen, Color.Transparent),
                                    startX = endX - 16f,
                                    endX = endX - 16f - exhaustLen
                                )
                            )

                            val flameBottom = Path().apply {
                                moveTo(endX - 16f, endY + 3f)
                                quadraticTo(endX - 16f - exhaustLen * 0.7f, endY + 6f, endX - 16f - exhaustLen, endY + 6f)
                                quadraticTo(endX - 16f - exhaustLen * 0.5f, endY + 6f, endX - 16f, endY + 6f)
                                close()
                            }
                            drawPath(
                                path = flameBottom,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AccentCyan, NeonGreen, Color.Transparent),
                                    startX = endX - 16f,
                                    endX = endX - 16f - exhaustLen
                                )
                            )

                            // 2. Cyber Sweep Wings (Futuristic stealth geometry)
                            val wings = Path().apply {
                                moveTo(endX + 8f, endY - 24f) // Top wingtip
                                lineTo(endX + 14f, endY - 6f)
                                lineTo(endX - 14f, endY - 5f)
                                lineTo(endX - 8f, endY - 18f)
                                close()
                            }
                            drawPath(path = wings, color = Color(0xFF1E1035))
                            drawPath(path = wings, color = AccentCyan, style = Stroke(width = 1.8f))

                            val bottomWings = Path().apply {
                                moveTo(endX + 8f, endY + 24f) // Bottom wingtip
                                lineTo(endX + 14f, endY + 6f)
                                lineTo(endX - 14f, endY + 5f)
                                lineTo(endX - 8f, endY + 18f)
                                close()
                            }
                            drawPath(path = bottomWings, color = Color(0xFF1E1035))
                            drawPath(path = bottomWings, color = AccentCyan, style = Stroke(width = 1.8f))

                            // Wingtip Laser Lights
                            drawCircle(color = AccentCyan, radius = 2.5f, center = Offset(endX + 8f, endY - 24f))
                            drawCircle(color = NeonGreen, radius = 2.5f, center = Offset(endX + 8f, endY + 24f))

                            // 3. Cyber Rocket Fuselage (Dart-shaped futuristic hull)
                            val hull = Path().apply {
                                moveTo(endX + 26f, endY) // Needle nose tip
                                lineTo(endX + 12f, endY - 8f)
                                lineTo(endX - 16f, endY - 7f)
                                lineTo(endX - 18f, endY)
                                lineTo(endX - 16f, endY + 7f)
                                lineTo(endX + 12f, endY + 8f)
                                close()
                            }
                            drawPath(
                                path = hull,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2E1A47), Color(0xFF4C1D95), Color(0xFF1E1B4B)),
                                    startX = endX - 18f,
                                    endX = endX + 26f
                                )
                            )
                            drawPath(path = hull, color = BrightPurple, style = Stroke(width = 2f))

                            // 4. Neon Cyber Cockpit / Sensor Array
                            val cockpit = Path().apply {
                                moveTo(endX + 18f, endY)
                                lineTo(endX + 8f, endY - 4f)
                                lineTo(endX + 2f, endY)
                                lineTo(endX + 8f, endY + 4f)
                                close()
                            }
                            drawPath(path = cockpit, color = AccentCyan)
                            drawCircle(color = Color.White, radius = 2f, center = Offset(endX + 12f, endY))

                            // 5. "92" Hull Glow Line
                            drawLine(
                                color = NeonGreen,
                                start = Offset(endX - 10f, endY),
                                end = Offset(endX + 12f, endY),
                                strokeWidth = 2f
                            )
                        }
                    } else if (gameState == "CRASHED") {
                        // Crash explosion shockwave visual
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CoralRed.copy(alpha = 0.8f), GoldYellow.copy(alpha = 0.4f), Color.Transparent),
                                center = Offset(endX, endY),
                                radius = 45f
                            ),
                            radius = 45f,
                            center = Offset(endX, endY)
                        )
                        drawCircle(
                            color = CoralRed,
                            radius = 18f,
                            center = Offset(endX, endY),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // Big Multiplier Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (gameState) {
                    "IDLE" -> {
                        Text(
                            text = "🚀",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "READY FOR WARP TAKEOFF",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Enter bet amount & press PLACE BET",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    "FLYING" -> {
                        Text(
                            text = String.format("%.2fx", currentMultiplier),
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrightPurple)
                        ) {
                            Text(
                                text = "EST. WIN: PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    "BAILED" -> {
                        Text(
                            text = "🎉 CASHED OUT!",
                            color = NeonGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("@ %.2fx", bailedMultiplier),
                            color = NeonGreen,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "+PKR ${String.format("%,.0f", activeBetAmount * bailedMultiplier)} credited to wallet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    "CRASHED" -> {
                        Text(
                            text = "💥 CORE CRASHED",
                            color = CoralRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("@ %.2fx", crashTarget),
                            color = CoralRed,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Bust! Round settled via backend",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Betting & Action Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance & Error Notice
                if (betErrorMessage.isNotBlank()) {
                    Surface(
                        color = CoralRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = betErrorMessage,
                            color = CoralRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Bet Amount Quick Selectors in Real PKR
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bet Amount in PKR:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Available: PKR ${String.format("%,.0f", pkrBalance)}",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50.0, 100.0, 250.0, 500.0, 1000.0).forEach { bet ->
                            val isSel = selectedBet == bet && customBetText.isEmpty()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (gameState == "IDLE" || gameState == "CRASHED" || gameState == "BAILED") {
                                            selectedBet = bet
                                            customBetText = ""
                                            betErrorMessage = ""
                                        }
                                    },
                                color = if (isSel) PrimaryPurple else CardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) BrightPurple else CardBorder
                                )
                            ) {
                                Text(
                                    text = bet.toInt().toString(),
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Main Bet / Cashout Button
                when (gameState) {
                    "IDLE", "CRASHED", "BAILED" -> {
                        Button(
                            onClick = {
                                if (pkrBalance < activeBetAmount) {
                                    betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.0f", pkrBalance)}). Please deposit to continue."
                                    return@Button
                                }
                                betErrorMessage = ""
                                isPlacingBet = true
                                coroutineScope.launch {
                                    val result = onPlaceBet(activeBetAmount)
                                    isPlacingBet = false
                                    if (result.isSuccess) {
                                        gameState = "FLYING"
                                    } else {
                                        betErrorMessage = result.exceptionOrNull()?.message ?: "Bet could not be placed."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pkrBalance >= activeBetAmount) NeonGreen else CardBg
                            ),
                            enabled = !isPlacingBet
                        ) {
                            if (isPlacingBet) {
                                CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            } else {
                                Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = DarkBg)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PLACE BET • PKR ${String.format("%,.0f", activeBetAmount)}",
                                    color = DarkBg,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    "FLYING" -> {
                        val currentCashoutValue = activeBetAmount * currentMultiplier
                        Button(
                            onClick = {
                                bailedMultiplier = currentMultiplier
                                gameState = "BAILED"
                                recentHistory.add(0, (bailedMultiplier * 100).toInt() / 100.0)
                                if (recentHistory.size > 10) recentHistory.removeLast()
                                onCashoutWin(currentCashoutValue, bailedMultiplier, activeBetAmount)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = DarkBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CASH OUT • PKR ${String.format("%,.0f", currentCashoutValue)} (${String.format("%.2f", currentMultiplier)}x)",
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
}
