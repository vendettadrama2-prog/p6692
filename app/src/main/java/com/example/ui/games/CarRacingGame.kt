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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class TrafficCar(
    val id: Long = Random.nextLong(),
    var lane: Int, // 0, 1, 2
    var yProgress: Float, // 0.0f (far horizon) to 1.1f (passed player)
    val color: Color,
    val speedFactor: Float = Random.nextFloat() * 0.4f + 0.8f
)

@Composable
fun CarRacingGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Game state: "IDLE", "RACING", "PAUSED", "GAME_OVER"
    var gameState by remember { mutableStateOf("IDLE") }
    var score by remember { mutableIntStateOf(0) }
    var distanceMeters by remember { mutableIntStateOf(0) }
    var currentSpeedKmh by remember { mutableIntStateOf(120) }
    var targetSpeedKmh by remember { mutableIntStateOf(160) }
    var playerLane by remember { mutableFloatStateOf(1f) } // 0f (left), 1f (center), 2f (right)
    var isAccelerating by remember { mutableStateOf(false) }
    var isBraking by remember { mutableStateOf(false) }
    var roadStripeOffset by remember { mutableFloatStateOf(0f) }
    var highScore by remember { mutableIntStateOf(1850) }

    val trafficList = remember { mutableStateListOf<TrafficCar>() }
    val carColors = remember { listOf(BrightCyan, CoralRed, GoldYellow, Color(0xFF10B981), Color(0xFFA855F7)) }

    // Selected car style
    var selectedCarIndex by remember { mutableIntStateOf(0) }
    val playerCarStyles = listOf(
        Triple("Apex Neon V12", BrightPurple, AccentCyan),
        Triple("Crimson GT-R", CoralRed, GoldYellow),
        Triple("Cyber Phantom", AccentCyan, NeonGreen)
    )

    // Game loop
    LaunchedEffect(gameState, isAccelerating, isBraking) {
        if (gameState != "RACING") return@LaunchedEffect

        var lastTrafficSpawn = System.currentTimeMillis()

        while (gameState == "RACING") {
            // Speed control
            if (isAccelerating) {
                currentSpeedKmh = min(280, currentSpeedKmh + 2)
            } else if (isBraking) {
                currentSpeedKmh = max(60, currentSpeedKmh - 4)
            } else {
                if (currentSpeedKmh < targetSpeedKmh) currentSpeedKmh = min(targetSpeedKmh, currentSpeedKmh + 1)
                else if (currentSpeedKmh > targetSpeedKmh) currentSpeedKmh = max(targetSpeedKmh, currentSpeedKmh - 1)
            }

            // Road stripe scroll
            val dt = (currentSpeedKmh / 160f) * 0.06f
            roadStripeOffset = (roadStripeOffset + dt) % 1f
            distanceMeters += (currentSpeedKmh / 36)
            score += (currentSpeedKmh / 40)

            // Spawn traffic
            val now = System.currentTimeMillis()
            val spawnInterval = max(900L, 2200L - (score / 10L))
            if (now - lastTrafficSpawn > spawnInterval) {
                lastTrafficSpawn = now
                val lane = Random.nextInt(3)
                trafficList.add(
                    TrafficCar(
                        lane = lane,
                        yProgress = 0f,
                        color = carColors.random()
                    )
                )
            }

            // Move traffic cars
            val iterator = trafficList.iterator()
            val currentTraffic = trafficList.toList()
            for (car in currentTraffic) {
                val speedDiff = (currentSpeedKmh - 90f * car.speedFactor) / 100f
                car.yProgress += (0.012f + max(0.005f, speedDiff * 0.015f))

                // Collision detection
                // Player is near bottom (yProgress around 0.85f - 0.95f)
                val isPlayerLevel = car.yProgress in 0.78f..0.94f
                val playerLaneInt = kotlin.math.round(playerLane).toInt()
                if (isPlayerLevel && car.lane == playerLaneInt) {
                    gameState = "GAME_OVER"
                    if (score > highScore) highScore = score
                    break
                }
            }

            // Remove cars off-screen
            trafficList.removeAll { it.yProgress > 1.15f }

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
                        modifier = Modifier.testTag("car_racing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PREMIUM CAR RACING",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Arcade Speed Way • Free Play",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }

                    if (gameState == "RACING") {
                        IconButton(onClick = { gameState = "PAUSED" }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Pause", tint = GoldYellow)
                        }
                    } else {
                        IconButton(onClick = {
                            trafficList.clear()
                            score = 0
                            distanceMeters = 0
                            currentSpeedKmh = 140
                            playerLane = 1f
                            gameState = "RACING"
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
            // HUD Dash: Score, Distance, Speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score Card
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

                // Speedometer
                Surface(
                    modifier = Modifier.weight(1.2f),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SPEED", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$currentSpeedKmh",
                                color = if (currentSpeedKmh > 220) CoralRed else GoldYellow,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("KM/H", color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }

                // Distance Card
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
                        Text("DISTANCE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${distanceMeters}m", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Main Canvas: 3D-perspective scrolling road & cars
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0B18))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(gameState) {
                            if (gameState == "RACING") {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.x > 15f && playerLane < 2f) {
                                        playerLane = min(2f, playerLane + 1f)
                                    } else if (dragAmount.x < -15f && playerLane > 0f) {
                                        playerLane = max(0f, playerLane - 1f)
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Horizon line
                    val horizonY = h * 0.12f

                    // Scenery: Dark neon skyline grid
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF090412), Color(0xFF1B0D33)),
                            startY = 0f,
                            endY = horizonY
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, horizonY)
                    )

                    // Road boundaries (perspective trapezoid)
                    val topRoadLeft = w * 0.28f
                    val topRoadRight = w * 0.72f
                    val bottomRoadLeft = w * 0.06f
                    val bottomRoadRight = w * 0.94f

                    val roadPath = Path().apply {
                        moveTo(topRoadLeft, horizonY)
                        lineTo(topRoadRight, horizonY)
                        lineTo(bottomRoadRight, h)
                        lineTo(bottomRoadLeft, h)
                        close()
                    }
                    drawPath(roadPath, color = Color(0xFF161224))

                    // Road neon border lines
                    drawLine(
                        color = PrimaryPurple,
                        start = Offset(topRoadLeft, horizonY),
                        end = Offset(bottomRoadLeft, h),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = PrimaryPurple,
                        start = Offset(topRoadRight, horizonY),
                        end = Offset(bottomRoadRight, h),
                        strokeWidth = 4f
                    )

                    // Lane dividers: 2 dividers for 3 lanes
                    for (laneDiv in 1..2) {
                        val topX = topRoadLeft + (topRoadRight - topRoadLeft) * (laneDiv / 3f)
                        val bottomX = bottomRoadLeft + (bottomRoadRight - bottomRoadLeft) * (laneDiv / 3f)

                        // Dashed animated lines
                        val stripeCount = 12
                        for (i in 0 until stripeCount) {
                            val t1 = ((i + roadStripeOffset) / stripeCount).coerceIn(0f, 1f)
                            val t2 = ((i + roadStripeOffset + 0.5f) / stripeCount).coerceIn(0f, 1f)

                            val sx = topX + (bottomX - topX) * t1
                            val sy = horizonY + (h - horizonY) * t1
                            val ex = topX + (bottomX - topX) * t2
                            val ey = horizonY + (h - horizonY) * t2

                            drawLine(
                                color = GoldYellow.copy(alpha = 0.85f),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 2f + 4f * t1
                            )
                        }
                    }

                    // Draw Traffic cars
                    for (car in trafficList) {
                        val t = car.yProgress.coerceIn(0f, 1.2f)
                        val curHorizonY = horizonY + (h - horizonY) * t
                        val curRoadLeft = topRoadLeft + (bottomRoadLeft - topRoadLeft) * t
                        val curRoadRight = topRoadRight + (bottomRoadRight - topRoadRight) * t
                        val laneWidth = (curRoadRight - curRoadLeft) / 3f

                        val carCenterX = curRoadLeft + laneWidth * (car.lane + 0.5f)
                        val carW = (24f + 36f * t)
                        val carH = (40f + 64f * t)

                        // Car shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(carCenterX - carW / 2 + 4f, curHorizonY - carH / 2 + 6f),
                            size = Size(carW, carH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        // Car body
                        drawRoundRect(
                            color = car.color,
                            topLeft = Offset(carCenterX - carW / 2, curHorizonY - carH / 2),
                            size = Size(carW, carH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        // Car windshield
                        drawRoundRect(
                            color = Color(0xFF0F0B18),
                            topLeft = Offset(carCenterX - carW * 0.35f, curHorizonY - carH * 0.2f),
                            size = Size(carW * 0.7f, carH * 0.35f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Tail lights
                        drawCircle(
                            color = CoralRed,
                            radius = 3f * (1f + t),
                            center = Offset(carCenterX - carW * 0.35f, curHorizonY + carH * 0.45f)
                        )
                        drawCircle(
                            color = CoralRed,
                            radius = 3f * (1f + t),
                            center = Offset(carCenterX + carW * 0.35f, curHorizonY + carH * 0.45f)
                        )
                    }

                    // Draw Player Sports Car
                    val playerT = 0.86f
                    val playerY = horizonY + (h - horizonY) * playerT
                    val pRoadLeft = topRoadLeft + (bottomRoadLeft - topRoadLeft) * playerT
                    val pRoadRight = topRoadRight + (bottomRoadRight - topRoadRight) * playerT
                    val pLaneWidth = (pRoadRight - pRoadLeft) / 3f

                    val playerCenterX = pRoadLeft + pLaneWidth * (playerLane + 0.5f)
                    val pCarW = 56f
                    val pCarH = 100f
                    val selectedCarColor = playerCarStyles[selectedCarIndex].second
                    val selectedGlowColor = playerCarStyles[selectedCarIndex].third

                    // Underglow neon
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(selectedGlowColor.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(playerCenterX, playerY),
                            radius = 65f
                        ),
                        topLeft = Offset(playerCenterX - pCarW - 10f, playerY - pCarH / 2 - 10f),
                        size = Size(pCarW * 2f + 20f, pCarH + 20f)
                    )

                    // Player car chassis
                    drawRoundRect(
                        color = selectedCarColor,
                        topLeft = Offset(playerCenterX - pCarW / 2, playerY - pCarH / 2),
                        size = Size(pCarW, pCarH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Cabin roof
                    drawRoundRect(
                        color = Color(0xFF0B0616),
                        topLeft = Offset(playerCenterX - pCarW * 0.35f, playerY - pCarH * 0.25f),
                        size = Size(pCarW * 0.7f, pCarH * 0.5f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Windshield glass
                    drawRoundRect(
                        color = AccentCyan.copy(alpha = 0.8f),
                        topLeft = Offset(playerCenterX - pCarW * 0.3f, playerY - pCarH * 0.2f),
                        size = Size(pCarW * 0.6f, pCarH * 0.2f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Headlight beams onto road
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, AccentCyan.copy(alpha = 0.5f)),
                            startY = playerY - pCarH / 2 - 90f,
                            endY = playerY - pCarH / 2
                        ),
                        start = Offset(playerCenterX - pCarW * 0.3f, playerY - pCarH / 2),
                        end = Offset(playerCenterX - pCarW * 0.5f, playerY - pCarH / 2 - 90f),
                        strokeWidth = 14f
                    )
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, AccentCyan.copy(alpha = 0.5f)),
                            startY = playerY - pCarH / 2 - 90f,
                            endY = playerY - pCarH / 2
                        ),
                        start = Offset(playerCenterX + pCarW * 0.3f, playerY - pCarH / 2),
                        end = Offset(playerCenterX + pCarW * 0.5f, playerY - pCarH / 2 - 90f),
                        strokeWidth = 14f
                    )
                }

                // Overlay screens: IDLE, PAUSED, GAME_OVER
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
                            Text("🏎️", fontSize = 54.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "PREMIUM CAR RACING",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Neon Highway • Dodge Traffic & Beat Highscore",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(16.dp))

                            // Car Selector Row
                            Text("SELECT YOUR SUPERCAR", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                playerCarStyles.forEachIndexed { index, (name, color, _) ->
                                    FilterChip(
                                        selected = selectedCarIndex == index,
                                        onClick = { selectedCarIndex = index },
                                        label = { Text(name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = color,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    trafficList.clear()
                                    score = 0
                                    distanceMeters = 0
                                    currentSpeedKmh = 140
                                    playerLane = 1f
                                    gameState = "RACING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("start_racing_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("START RACE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                } else if (gameState == "PAUSED") {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg.copy(alpha = 0.85f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("PAUSED", color = GoldYellow, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { gameState = "RACING" },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("RESUME")
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
                            Text("CRASHED!", color = CoralRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("Final Score: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Distance: ${distanceMeters}m", color = TextSecondary, fontSize = 13.sp)
                            Text("Best: $highScore", color = GoldYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    trafficList.clear()
                                    score = 0
                                    distanceMeters = 0
                                    currentSpeedKmh = 140
                                    playerLane = 1f
                                    gameState = "RACING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("restart_racing_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("RACE AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Touch Steering Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steer Left / Right
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FilledIconButton(
                        onClick = { if (playerLane > 0f && gameState == "RACING") playerLane -= 1f },
                        modifier = Modifier.size(56.dp).testTag("steer_left_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgLight),
                        shape = CircleShape
                    ) {
                        Text("◀", color = AccentCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledIconButton(
                        onClick = { if (playerLane < 2f && gameState == "RACING") playerLane += 1f },
                        modifier = Modifier.size(56.dp).testTag("steer_right_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgLight),
                        shape = CircleShape
                    ) {
                        Text("▶", color = AccentCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Brake / Accelerate
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Brake
                    FilledIconButton(
                        onClick = {
                            isBraking = !isBraking
                            if (isBraking) isAccelerating = false
                        },
                        modifier = Modifier.size(56.dp).testTag("brake_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isBraking) CoralRed else CardBgLight
                        ),
                        shape = CircleShape
                    ) {
                        Text("🛑", fontSize = 20.sp)
                    }

                    // Nitro / Gas
                    FilledIconButton(
                        onClick = {
                            isAccelerating = !isAccelerating
                            if (isAccelerating) isBraking = false
                        },
                        modifier = Modifier.size(56.dp).testTag("accelerate_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isAccelerating) NeonGreen else CardBgLight
                        ),
                        shape = CircleShape
                    ) {
                        Text("⚡", fontSize = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
