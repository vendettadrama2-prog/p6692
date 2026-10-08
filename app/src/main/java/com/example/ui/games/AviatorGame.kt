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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * P6692 Aviator Game
 * Features:
 * - Real PKR betting flow connected to backend wallet
 * - Real-time rising flight curve with dynamic plane trajectory
 * - Increasing multiplier: 1.00x -> 1.20x -> 1.50x -> 2.00x ...
 * - State management: IDLE / WAITING -> FLYING -> FLEW_AWAY (Crash) / CASHED_OUT
 * - Prominent interactive BET and CASH OUT button with live payout preview
 * - Round history pills and recent crash multipliers
 * - P6692 Premium Dark Palette (#07050D, #8B5CF6, #22D3EE, #F43F5E)
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

    var gameState by remember { mutableStateOf("IDLE") } // IDLE, FLYING, FLEW_AWAY, CASHED_OUT
    var currentMultiplier by remember { mutableDoubleStateOf(1.00) }
    var crashPoint by remember { mutableDoubleStateOf(2.20) }
    var flightTimeSeconds by remember { mutableFloatStateOf(0f) }

    val quickBets = listOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)
    var selectedBet by remember { mutableDoubleStateOf(100.0) }
    var customBetText by remember { mutableStateOf("") }
    var cashedOutMultiplier by remember { mutableDoubleStateOf(0.0) }
    var cashedOutAmount by remember { mutableDoubleStateOf(0.0) }

    var isPlacingBet by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }
    val roundHistory = remember { mutableStateListOf(1.94, 1.22, 4.35, 1.08, 12.80, 2.11, 1.45, 6.70) }

    val activeBetAmount = remember(selectedBet, customBetText) {
        val parsed = customBetText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else selectedBet
    }

    // Dynamic flight simulation loop
    LaunchedEffect(gameState) {
        if (gameState == "FLYING") {
            currentMultiplier = 1.00
            flightTimeSeconds = 0f

            // Generate authentic random crash point
            val roll = Random.nextDouble(0.0, 1.0)
            crashPoint = when {
                roll < 0.10 -> 1.02 + Random.nextDouble(0.0, 0.15)
                roll < 0.45 -> 1.20 + Random.nextDouble(0.0, 0.90)
                roll < 0.75 -> 2.10 + Random.nextDouble(0.0, 2.50)
                roll < 0.92 -> 4.60 + Random.nextDouble(0.0, 6.00)
                else -> 12.0 + Random.nextDouble(0.0, 35.0)
            }

            val startTime = System.currentTimeMillis()
            while (gameState == "FLYING") {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                flightTimeSeconds = elapsed.toFloat()
                // Realistic Aviator growth curve
                val mult = 1.0 + (elapsed * 0.38) + (elapsed * elapsed * 0.08)
                currentMultiplier = (mult * 100).toInt() / 100.0

                if (currentMultiplier >= crashPoint) {
                    gameState = "FLEW_AWAY"
                    val roundedCrash = (crashPoint * 100).toInt() / 100.0
                    roundHistory.add(0, roundedCrash)
                    if (roundHistory.size > 12) roundHistory.removeLast()
                    onRoundLost(activeBetAmount)
                    break
                }
                delay(30)
            }
        }
    }

    // Pulse animation for multiplier
    val infiniteTransition = rememberInfiniteTransition(label = "aviator_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
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
                                text = "AVIATOR",
                                color = CoralRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = CoralRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CRASH",
                                    color = CoralRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real PKR Ascent",
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

        // RECENT ROUND HISTORY STRIP
        Surface(
            color = Color(0xFF0F0B1A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORY",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(roundHistory) { mult ->
                        val pillColor = when {
                            mult >= 10.0 -> GoldYellow
                            mult >= 2.0 -> BrightPurple
                            mult >= 1.5 -> AccentCyan
                            else -> CoralRed
                        }
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pillColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${String.format("%.2f", mult)}x",
                                color = pillColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // MAIN FLIGHT RADAR CANVAS & MULTIPLIER DISPLAY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF130924),
                            Color(0xFF0A0514),
                            DarkBg
                        )
                    )
                )
                .border(1.5.dp, if (gameState == "FLYING") CoralRed.copy(alpha = 0.7f) else CardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Aviation Grid & Curved Plane Flight Path
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid background lines
                val gridStroke = Stroke(width = 1f)
                val gridColor = Color(0xFF261944).copy(alpha = 0.5f)
                for (i in 1..5) {
                    val y = h * (i / 6f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                for (i in 1..5) {
                    val x = w * (i / 6f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }

                // Flight Curve
                if (gameState == "FLYING" || gameState == "FLEW_AWAY" || gameState == "CASHED_OUT") {
                    val progress = (flightTimeSeconds / 10f).coerceIn(0f, 1f)
                    val curveStartX = 30f
                    val curveStartY = h - 30f
                    val currentX = curveStartX + (w - 100f) * progress
                    val currentY = curveStartY - (h - 100f) * (progress * progress)

                    val path = Path().apply {
                        moveTo(curveStartX, curveStartY)
                        quadraticTo(
                            curveStartX + (currentX - curveStartX) * 0.6f,
                            curveStartY,
                            currentX,
                            currentY
                        )
                    }

                    // Shadow fill under curve
                    val fillPath = Path().apply {
                        moveTo(curveStartX, curveStartY)
                        quadraticTo(
                            curveStartX + (currentX - curveStartX) * 0.6f,
                            curveStartY,
                            currentX,
                            currentY
                        )
                        lineTo(currentX, curveStartY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                CoralRed.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = path,
                        color = if (gameState == "FLEW_AWAY") Color.Gray else CoralRed,
                        style = Stroke(width = 5f)
                    )

                    // Draw Plane Indicator Icon
                    if (gameState != "FLEW_AWAY") {
                        drawCircle(
                            color = CoralRed,
                            radius = 10f,
                            center = Offset(currentX, currentY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }

            // Central Status & Multiplier Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (gameState) {
                    "IDLE" -> {
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
                        ) {
                            Text(
                                text = "WAITING FOR NEXT ROUND",
                                color = BrightPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Text(
                            text = "Select your bet in PKR and press BET",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    "FLYING" -> {
                        Text(
                            text = "${String.format("%.2f", currentMultiplier)}x",
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        )
                        Text(
                            text = "Current Payout: PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}",
                            color = AccentCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            text = "Round closed. Better luck next flight!",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    "CASHED_OUT" -> {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "CASHED OUT!",
                            color = NeonGreen,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Won PKR ${String.format("%,.0f", cashedOutAmount)} (${String.format("%.2f", cashedOutMultiplier)}x)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Credited to your verified PKR balance",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // BET CONTROLS & WALLET BETTING PANEL
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Amount Badges
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
                        text = "Min: PKR 50 • Max: PKR 25,000",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickBets) { amt ->
                        val isSelected = selectedBet == amt && customBetText.isEmpty()
                        Surface(
                            color = if (isSelected) CoralRed else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CoralRed else CardBorder),
                            modifier = Modifier.clickable(enabled = gameState == "IDLE" || gameState == "FLEW_AWAY" || gameState == "CASHED_OUT") {
                                selectedBet = amt
                                customBetText = ""
                                betErrorMessage = ""
                            }
                        ) {
                            Text(
                                text = "PKR ${amt.toInt()}",
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Custom Bet Input Field
                OutlinedTextField(
                    value = customBetText,
                    onValueChange = {
                        customBetText = it
                        betErrorMessage = ""
                    },
                    label = { Text("Or enter custom bet in PKR") },
                    placeholder = { Text("e.g. 750") },
                    leadingIcon = {
                        Text("PKR", color = CoralRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                    },
                    enabled = gameState == "IDLE" || gameState == "FLEW_AWAY" || gameState == "CASHED_OUT",
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CoralRed,
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

                // PRIMARY ACTION BUTTON (BET / CASH OUT)
                when (gameState) {
                    "FLYING" -> {
                        val currentEstimatedWin = activeBetAmount * currentMultiplier
                        Button(
                            onClick = {
                                val winMult = currentMultiplier
                                val winPkr = (activeBetAmount * winMult * 100).toInt() / 100.0
                                cashedOutMultiplier = winMult
                                cashedOutAmount = winPkr
                                gameState = "CASHED_OUT"
                                roundHistory.add(0, (winMult * 100).toInt() / 100.0)
                                if (roundHistory.size > 12) roundHistory.removeLast()
                                onCashoutWin(winPkr, winMult, activeBetAmount)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CASH OUT",
                                    color = DarkBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "WIN PKR ${String.format("%,.0f", currentEstimatedWin)} (${String.format("%.2f", currentMultiplier)}x)",
                                    color = DarkBg.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                if (activeBetAmount > pkrBalance) {
                                    betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.2f", pkrBalance)}). Please deposit."
                                    return@Button
                                }
                                if (activeBetAmount < 50.0) {
                                    betErrorMessage = "Minimum bet is PKR 50."
                                    return@Button
                                }

                                isPlacingBet = true
                                betErrorMessage = ""

                                coroutineScope.launch {
                                    val betResult = onPlaceBet(activeBetAmount)
                                    isPlacingBet = false
                                    if (betResult.isSuccess) {
                                        gameState = "FLYING"
                                    } else {
                                        betErrorMessage = betResult.exceptionOrNull()?.message ?: "Failed to place bet"
                                    }
                                }
                            },
                            enabled = !isPlacingBet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                        ) {
                            if (isPlacingBet) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "BET PKR ${String.format("%,.0f", activeBetAmount)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
