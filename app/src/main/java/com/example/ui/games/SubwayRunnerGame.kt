package com.example.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class SubwayObstacleType {
    TRAIN,
    BARRIER_HIGH, // Must slide under
    BARRIER_LOW,  // Must jump over
    COIN_LINE
}

data class SubwayObstacle(
    val id: Long = Random.nextLong(),
    val lane: Int, // 0: left, 1: center, 2: right
    var yProgress: Float, // 0.0f (far) to 1.1f (near/passed)
    val type: SubwayObstacleType,
    var collected: Boolean = false
)

@Composable
fun SubwayRunnerGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Game states: "IDLE", "RUNNING", "PAUSED", "GAME_OVER"
    var gameState by remember { mutableStateOf("IDLE") }
    var score by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var distanceMeters by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(3420) }

    // Character state
    var currentLane by remember { mutableIntStateOf(1) } // 0, 1, 2
    var isJumping by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var jumpProgress by remember { mutableFloatStateOf(0f) } // 0f to 1f

    val obstacles = remember { mutableStateListOf<SubwayObstacle>() }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }

    // Touch gesture swipe detector
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    fun handleSwipeLeft() {
        if (currentLane > 0 && gameState == "RUNNING") currentLane -= 1
    }

    fun handleSwipeRight() {
        if (currentLane < 2 && gameState == "RUNNING") currentLane += 1
    }

    fun handleSwipeUp() {
        if (!isJumping && gameState == "RUNNING") {
            isJumping = true
            isSliding = false
        }
    }

    fun handleSwipeDown() {
        if (gameState == "RUNNING") {
            isSliding = true
            isJumping = false
        }
    }

    // Main Game Loop
    LaunchedEffect(gameState) {
        if (gameState != "RUNNING") return@LaunchedEffect

        var lastSpawnTime = System.currentTimeMillis()
        var jumpStartTime = 0L
        var slideStartTime = 0L

        while (gameState == "RUNNING") {
            // Update distance & score
            distanceMeters += (speedMultiplier * 2).toInt()
            score += (speedMultiplier * 3).toInt()

            // Increase speed progressively
            speedMultiplier = min(2.5f, 1.0f + (distanceMeters / 1200f))

            // Jump animation logic
            if (isJumping) {
                if (jumpStartTime == 0L) jumpStartTime = System.currentTimeMillis()
                val elapsed = System.currentTimeMillis() - jumpStartTime
                if (elapsed < 600) {
                    jumpProgress = kotlin.math.sin((elapsed / 600f) * Math.PI).toFloat()
                } else {
                    isJumping = false
                    jumpProgress = 0f
                    jumpStartTime = 0L
                }
            }

            // Slide animation logic
            if (isSliding) {
                if (slideStartTime == 0L) slideStartTime = System.currentTimeMillis()
                val elapsed = System.currentTimeMillis() - slideStartTime
                if (elapsed >= 550) {
                    isSliding = false
                    slideStartTime = 0L
                }
            }

            // Spawn obstacles & coins
            val now = System.currentTimeMillis()
            val spawnRate = max(650L, (1800L / speedMultiplier).toLong())
            if (now - lastSpawnTime > spawnRate) {
                lastSpawnTime = now
                val lane = Random.nextInt(3)
                val type = when (Random.nextInt(10)) {
                    0, 1, 2 -> SubwayObstacleType.COIN_LINE
                    3, 4, 5 -> SubwayObstacleType.BARRIER_LOW
                    6, 7 -> SubwayObstacleType.BARRIER_HIGH
                    else -> SubwayObstacleType.TRAIN
                }
                obstacles.add(SubwayObstacle(lane = lane, yProgress = 0f, type = type))
            }

            // Move & check collisions
            val currentObs = obstacles.toList()
            for (obs in currentObs) {
                obs.yProgress += (0.018f * speedMultiplier)

                // Player collision check at z=0.82f..0.94f
                val atPlayerLevel = obs.yProgress in 0.82f..0.94f
                if (atPlayerLevel && obs.lane == currentLane) {
                    when (obs.type) {
                        SubwayObstacleType.COIN_LINE -> {
                            if (!obs.collected) {
                                obs.collected = true
                                coinsCollected += 1
                                score += 50
                            }
                        }
                        SubwayObstacleType.BARRIER_LOW -> {
                            // Can only survive if jumped
                            if (!isJumping) {
                                gameState = "GAME_OVER"
                                if (score > highScore) highScore = score
                                break
                            }
                        }
                        SubwayObstacleType.BARRIER_HIGH -> {
                            // Can only survive if sliding
                            if (!isSliding) {
                                gameState = "GAME_OVER"
                                if (score > highScore) highScore = score
                                break
                            }
                        }
                        SubwayObstacleType.TRAIN -> {
                            // Train collides unless not in lane
                            gameState = "GAME_OVER"
                            if (score > highScore) highScore = score
                            break
                        }
                    }
                }
            }

            // Clean up off-screen
            obstacles.removeAll { it.yProgress > 1.15f }

            delay(25L)
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Surface(
                color = SecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subway_runner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SUBWAY RUNNER 3D",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Endless Track • Swipe to Dodge",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }

                    if (gameState == "RUNNING") {
                        IconButton(onClick = { gameState = "PAUSED" }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Pause", tint = GoldYellow)
                        }
                    } else {
                        IconButton(onClick = {
                            obstacles.clear()
                            score = 0
                            coinsCollected = 0
                            distanceMeters = 0
                            currentLane = 1
                            speedMultiplier = 1.0f
                            gameState = "RUNNING"
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AccentCyan)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SCORE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$score", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Coins
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("COINS", color = GoldYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("$coinsCollected", color = GoldYellow, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Distance
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("METERS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${distanceMeters}m", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3D Perspective Track Canvas with Swipe Controls
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF07040F))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(gameState) {
                            if (gameState == "RUNNING") {
                                detectDragGestures(
                                    onDragStart = {
                                        totalDragX = 0f
                                        totalDragY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragX += dragAmount.x
                                        totalDragY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        if (abs(totalDragX) > abs(totalDragY)) {
                                            // Horizontal swipe
                                            if (totalDragX > 30f) handleSwipeRight()
                                            else if (totalDragX < -30f) handleSwipeLeft()
                                        } else {
                                            // Vertical swipe
                                            if (totalDragY < -30f) handleSwipeUp()
                                            else if (totalDragY > 30f) handleSwipeDown()
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val horizonY = h * 0.14f

                    // Subway tunnel background
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF0F0A1F), Color(0xFF1E1038)),
                            startY = 0f,
                            endY = horizonY
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, horizonY)
                    )

                    // Three tracks perspective
                    val topL = w * 0.32f
                    val topR = w * 0.68f
                    val botL = w * 0.04f
                    val botR = w * 0.96f

                    // Track base
                    val trackPath = Path().apply {
                        moveTo(topL, horizonY)
                        lineTo(topR, horizonY)
                        lineTo(botR, h)
                        lineTo(botL, h)
                        close()
                    }
                    drawPath(trackPath, color = Color(0xFF130E22))

                    // Rails (4 rails for 3 lanes)
                    for (i in 0..3) {
                        val sx = topL + (topR - topL) * (i / 3f)
                        val ex = botL + (botR - botL) * (i / 3f)
                        drawLine(
                            color = PrimaryPurple.copy(alpha = 0.8f),
                            start = Offset(sx, horizonY),
                            end = Offset(ex, h),
                            strokeWidth = 3f
                        )
                    }

                    // Render Obstacles
                    for (obs in obstacles) {
                        val t = obs.yProgress.coerceIn(0f, 1.2f)
                        val curY = horizonY + (h - horizonY) * t
                        val curL = topL + (botL - topL) * t
                        val curR = topR + (botR - topR) * t
                        val laneW = (curR - curL) / 3f
                        val centerX = curL + laneW * (obs.lane + 0.5f)

                        when (obs.type) {
                            SubwayObstacleType.COIN_LINE -> {
                                if (!obs.collected) {
                                    val coinR = 8f + 14f * t
                                    drawCircle(
                                        color = GoldYellow,
                                        radius = coinR,
                                        center = Offset(centerX, curY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        radius = coinR * 0.45f,
                                        center = Offset(centerX, curY)
                                    )
                                }
                            }
                            SubwayObstacleType.BARRIER_LOW -> {
                                // Wooden hurdle to jump
                                val barW = (24f + 48f * t)
                                val barH = (12f + 20f * t)
                                drawRoundRect(
                                    color = CoralRed,
                                    topLeft = Offset(centerX - barW / 2, curY - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                            SubwayObstacleType.BARRIER_HIGH -> {
                                // High neon arch to slide under
                                val barW = (28f + 56f * t)
                                val barH = (20f + 36f * t)
                                drawRoundRect(
                                    color = BrightPurple,
                                    topLeft = Offset(centerX - barW / 2, curY - barH - 25f * t),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                            SubwayObstacleType.TRAIN -> {
                                // Subway train carriage
                                val trainW = (32f + 64f * t)
                                val trainH = (50f + 110f * t)
                                drawRoundRect(
                                    color = Color(0xFF22D3EE),
                                    topLeft = Offset(centerX - trainW / 2, curY - trainH),
                                    size = Size(trainW, trainH),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                // Train windows
                                drawRoundRect(
                                    color = Color(0xFF0A1B2A),
                                    topLeft = Offset(centerX - trainW * 0.35f, curY - trainH * 0.8f),
                                    size = Size(trainW * 0.7f, trainH * 0.3f),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }

                    // Render Player Character (Runner)
                    val pT = 0.88f
                    val pCurY = horizonY + (h - horizonY) * pT - (jumpProgress * 70f)
                    val pCurL = topL + (botL - topL) * pT
                    val pCurR = topR + (botR - topR) * pT
                    val pLaneW = (pCurR - pCurL) / 3f
                    val pCenterX = pCurL + pLaneW * (currentLane + 0.5f)

                    val charHeight = if (isSliding) 36f else 64f
                    val charWidth = 34f

                    // Character Shadow
                    drawOval(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(pCenterX - 22f, horizonY + (h - horizonY) * pT + 4f),
                        size = Size(44f, 16f)
                    )

                    // Character Body
                    drawRoundRect(
                        color = if (isSliding) AccentCyan else BrightPurple,
                        topLeft = Offset(pCenterX - charWidth / 2, pCurY - charHeight),
                        size = Size(charWidth, charHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Head
                    if (!isSliding) {
                        drawCircle(
                            color = GoldYellow,
                            radius = 12f,
                            center = Offset(pCenterX, pCurY - charHeight - 8f)
                        )
                    }
                }

                // Game State Overlays
                if (gameState == "IDLE") {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg.copy(alpha = 0.88f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🏃", fontSize = 52.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "SUBWAY RUNNER 3D",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Swipe Left / Right to switch tracks\nSwipe Up to Jump • Swipe Down to Slide",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    obstacles.clear()
                                    score = 0
                                    coinsCollected = 0
                                    distanceMeters = 0
                                    currentLane = 1
                                    speedMultiplier = 1.0f
                                    gameState = "RUNNING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("start_subway_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("START RUN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                } else if (gameState == "GAME_OVER") {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg.copy(alpha = 0.92f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("💥", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("RUN TERMINATED!", color = CoralRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Score: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Coins: 🪙 $coinsCollected", color = GoldYellow, fontSize = 14.sp)
                            Text("High Score: $highScore", color = AccentCyan, fontSize = 13.sp)

                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    obstacles.clear()
                                    score = 0
                                    coinsCollected = 0
                                    distanceMeters = 0
                                    currentLane = 1
                                    speedMultiplier = 1.0f
                                    gameState = "RUNNING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("restart_subway_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("RUN AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // On-screen D-Pad Controls for Touch Convenience
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left / Right
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilledIconButton(
                        onClick = { handleSwipeLeft() },
                        modifier = Modifier.size(54.dp).testTag("runner_left_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgLight),
                        shape = CircleShape
                    ) {
                        Text("◀", color = AccentCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledIconButton(
                        onClick = { handleSwipeRight() },
                        modifier = Modifier.size(54.dp).testTag("runner_right_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgLight),
                        shape = CircleShape
                    ) {
                        Text("▶", color = AccentCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Jump / Slide
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilledIconButton(
                        onClick = { handleSwipeUp() },
                        modifier = Modifier.size(54.dp).testTag("runner_jump_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryPurple),
                        shape = CircleShape
                    ) {
                        Text("⬆ JUMP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledIconButton(
                        onClick = { handleSwipeDown() },
                        modifier = Modifier.size(54.dp).testTag("runner_slide_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgLight),
                        shape = CircleShape
                    ) {
                        Text("⬇ SLIDE", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}
