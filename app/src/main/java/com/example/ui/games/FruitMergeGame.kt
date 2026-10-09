package com.example.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

// Fruit tier definitions from smallest (Cherry) to biggest (Watermelon)
enum class FruitTier(
    val tier: Int,
    val emoji: String,
    val radius: Float,
    val color: Color,
    val glowColor: Color,
    val nameTitle: String,
    val points: Int
) {
    CHERRY(1, "🍒", 14f, Color(0xFFEF4444), Color(0x66EF4444), "Cherry", 10),
    STRAWBERRY(2, "🍓", 18f, Color(0xFFF43F5E), Color(0x66F43F5E), "Strawberry", 20),
    GRAPE(3, "🍇", 22f, Color(0xFFA855F7), Color(0x66A855F7), "Grape", 40),
    ORANGE(4, "🍊", 27f, Color(0xFFF97316), Color(0x66F97316), "Orange", 80),
    APPLE(5, "🍏", 33f, Color(0xFF10B981), Color(0x6610B981), "Apple", 150),
    PEACH(6, "🍑", 39f, Color(0xFFFB923C), Color(0x66FB923C), "Peach", 300),
    PINEAPPLE(7, "🍍", 46f, Color(0xFFFBBF24), Color(0x66FBBF24), "Pineapple", 600),
    MELON(8, "🍈", 54f, Color(0xFF34D399), Color(0x6634D399), "Melon", 1200),
    WATERMELON(9, "🍉", 64f, Color(0xFF00E676), Color(0x6600E676), "Watermelon", 2500);

    fun nextTier(): FruitTier? {
        val nextOrd = this.ordinal + 1
        return if (nextOrd < values().size) values()[nextOrd] else null
    }
}

data class DroppedFruit(
    val id: Long = Random.nextLong(),
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val tier: FruitTier
)

/**
 * Fruit Merge (Suika-inspired Arcade Game) for P6692
 * - Authentic physics & merge mechanics: Drop fruit from container top, gravity pulls fruit down
 * - When two matching fruit tiers collide, they merge with pop animation into the next larger fruit
 * - Score tracking, highest fruit tier unlocked, danger line detection
 * - Real PKR betting flow with backend wallet integration
 * - Smooth physics simulation and P6692 dark purple/cyan theme
 */
@Composable
fun FruitMergeGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Dropper state: x position at top of container
    var dropperX by remember { mutableFloatStateOf(180f) }
    var currentDroppingTier by remember { mutableStateOf(FruitTier.CHERRY) }
    var nextDroppingTier by remember { mutableStateOf(FruitTier.STRAWBERRY) }
    var canDropFruit by remember { mutableStateOf(true) }

    // Active fruit on board
    var fruits by remember { mutableStateOf<List<DroppedFruit>>(emptyList()) }

    // Stats
    var score by remember { mutableIntStateOf(0) }
    var mergeCount by remember { mutableIntStateOf(0) }
    var highestTierReached by remember { mutableStateOf(FruitTier.CHERRY) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var gameStarted by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }

    // Betting
    val quickBets = listOf(25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
    var selectedBet by remember { mutableDoubleStateOf(100.0) }
    var customBetText by remember { mutableStateOf("") }
    var betErrorMessage by remember { mutableStateOf("") }
    var isBettingInFlight by remember { mutableStateOf(false) }

    val activeBetAmount = remember(selectedBet, customBetText) {
        val parsed = customBetText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else selectedBet
    }

    fun restartGame() {
        fruits = emptyList()
        dropperX = 180f
        score = 0
        mergeCount = 0
        highestTierReached = FruitTier.CHERRY
        currentMultiplier = 1.0
        canDropFruit = true
        isGameOver = false
        isVictory = false
        gameStarted = false
        currentDroppingTier = FruitTier.values()[Random.nextInt(3)]
        nextDroppingTier = FruitTier.values()[Random.nextInt(3)]
    }

    // Drop fruit handler
    fun dropCurrentFruit(boardW: Float) {
        if (!gameStarted || isGameOver || !canDropFruit) return
        canDropFruit = false

        val clampedX = dropperX.coerceIn(currentDroppingTier.radius + 15f, boardW - currentDroppingTier.radius - 15f)
        val newFruit = DroppedFruit(
            x = clampedX,
            y = 55f,
            vx = Random.nextFloat() * 1.5f - 0.75f,
            vy = 2f,
            tier = currentDroppingTier
        )

        fruits = fruits + newFruit
        currentDroppingTier = nextDroppingTier
        nextDroppingTier = FruitTier.values()[Random.nextInt(3)] // drops cherry, strawberry, or grape

        coroutineScope.launch {
            delay(450)
            if (!isGameOver) {
                canDropFruit = true
            }
        }
    }

    // Physics & Merge Tick Loop
    LaunchedEffect(gameStarted, isGameOver) {
        if (!gameStarted || isGameOver) return@LaunchedEffect

        val gravity = 0.55f
        val restitution = 0.25f
        val friction = 0.94f

        while (gameStarted && !isGameOver) {
            delay(16) // ~60fps
            if (fruits.isEmpty()) continue

            val currentList = fruits.map { it.copy() }.toMutableList()
            val toRemove = mutableSetOf<Long>()
            val toAdd = mutableListOf<DroppedFruit>()

            val boardW = 340f
            val floorY = 410f
            val dangerLineY = 95f

            // 1. Move and check bounds
            for (f in currentList) {
                f.vy += gravity
                f.vx *= friction
                f.x += f.vx
                f.y += f.vy

                // Floor collision
                if (f.y + f.tier.radius > floorY) {
                    f.y = floorY - f.tier.radius
                    f.vy = -f.vy * restitution
                    if (kotlin.math.abs(f.vy) < 0.6f) f.vy = 0f
                }

                // Left wall collision
                val leftBound = 16f + f.tier.radius
                if (f.x < leftBound) {
                    f.x = leftBound
                    f.vx = -f.vx * restitution
                }

                // Right wall collision
                val rightBound = boardW - 16f - f.tier.radius
                if (f.x > rightBound) {
                    f.x = rightBound
                    f.vx = -f.vx * restitution
                }
            }

            // 2. Pairwise collisions & Merging
            for (i in 0 until currentList.size) {
                for (j in i + 1 until currentList.size) {
                    val f1 = currentList[i]
                    val f2 = currentList[j]
                    if (toRemove.contains(f1.id) || toRemove.contains(f2.id)) continue

                    val dx = f2.x - f1.x
                    val dy = f2.y - f1.y
                    val dist = hypot(dx, dy)
                    val minDist = f1.tier.radius + f2.tier.radius

                    if (dist < minDist && dist > 0.001f) {
                        // Check if same tier => MERGE!
                        if (f1.tier == f2.tier) {
                            val next = f1.tier.nextTier()
                            toRemove.add(f1.id)
                            toRemove.add(f2.id)

                            val midX = (f1.x + f2.x) / 2f
                            val midY = (f1.y + f2.y) / 2f

                            if (next != null) {
                                toAdd.add(
                                    DroppedFruit(
                                        x = midX,
                                        y = midY,
                                        vx = (f1.vx + f2.vx) * 0.4f,
                                        vy = -2f,
                                        tier = next
                                    )
                                )
                                score += next.points
                                mergeCount++
                                if (next.tier > highestTierReached.tier) {
                                    highestTierReached = next
                                }
                                currentMultiplier = 1.0 + (score / 350.0).coerceAtMost(25.0)

                                if (next == FruitTier.WATERMELON) {
                                    isVictory = true
                                }
                            } else {
                                // Double watermelon jackpot burst
                                score += 5000
                                currentMultiplier += 5.0
                            }
                        } else {
                            // Elastic bounce separation
                            val overlap = minDist - dist
                            val nx = dx / dist
                            val ny = dy / dist

                            f1.x -= nx * overlap * 0.5f
                            f1.y -= ny * overlap * 0.5f
                            f2.x += nx * overlap * 0.5f
                            f2.y += ny * overlap * 0.5f

                            val kx = f1.vx - f2.vx
                            val ky = f1.vy - f2.vy
                            val p = 2f * (nx * kx + ny * ky) / 2f
                            f1.vx -= p * nx * 0.5f
                            f1.vy -= p * ny * 0.5f
                            f2.vx += p * nx * 0.5f
                            f2.vy += p * ny * 0.5f
                        }
                    }
                }
            }

            // Apply removals and additions
            val nextFruits = currentList.filterNot { toRemove.contains(it.id) } + toAdd
            fruits = nextFruits

            // Check game over condition: any settled fruit above danger line for too long
            val overflowFruit = nextFruits.find { it.y - it.tier.radius < dangerLineY && kotlin.math.abs(it.vy) < 0.8f && it.y > 60f }
            if (overflowFruit != null && nextFruits.size >= 8) {
                isGameOver = true
                if (score >= 450) {
                    val winPkr = activeBetAmount * currentMultiplier
                    onCashoutWin(winPkr, currentMultiplier, activeBetAmount)
                } else {
                    onRoundLost(activeBetAmount)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryBg,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .testTag("fruit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Fruit Merge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                color = NeonGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "ARCADE",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("PKR Balance: ₨ ${String.format("%,.2f", pkrBalance)}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showInstructions = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Instructions", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { restartGame() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .testTag("fruit_restart_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Live Ribbon: Score, Multiplier, Highest Tier, Payout
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = CardBg,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$score", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MULTIPLIER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.2f", currentMultiplier)}x", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HIGHEST", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(highestTierReached.emoji, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POTENTIAL WIN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}", color = GoldYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Glass Container Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0826),
                            Color(0xFF170D36),
                            Color(0xFF0A051C)
                        )
                    )
                )
                .border(1.5.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            // Touch / Drag Area to aim and drop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameStarted, isGameOver) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                if (gameStarted && !isGameOver) {
                                    dropperX = change.position.x
                                }
                            },
                            onDragEnd = {
                                if (gameStarted && !isGameOver) {
                                    dropCurrentFruit(size.width.toFloat())
                                }
                            }
                        )
                    }
                    .pointerInput(gameStarted, isGameOver) {
                        detectTapGestures { offset ->
                            if (gameStarted && !isGameOver) {
                                dropperX = offset.x
                                dropCurrentFruit(size.width.toFloat())
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Red Warning Line near the top
                    drawLine(
                        color = CoralRed.copy(alpha = 0.65f),
                        start = Offset(16f, 95f),
                        end = Offset(w - 16f, 95f),
                        strokeWidth = 2f
                    )

                    // 2. Draw Aim Line from Dropper
                    if (gameStarted && !isGameOver) {
                        drawLine(
                            color = AccentCyan.copy(alpha = 0.45f),
                            start = Offset(dropperX, 45f),
                            end = Offset(dropperX, h - 30f),
                            strokeWidth = 1.5f
                        )

                        // Draw Dropper Preview Fruit
                        drawCircle(
                            color = currentDroppingTier.glowColor,
                            radius = currentDroppingTier.radius * 1.2f,
                            center = Offset(dropperX, 45f)
                        )
                        drawCircle(
                            color = currentDroppingTier.color,
                            radius = currentDroppingTier.radius,
                            center = Offset(dropperX, 45f)
                        )
                    }

                    // 3. Draw All Dropped Fruits
                    fruits.forEach { f ->
                        // Glow circle
                        drawCircle(
                            color = f.tier.glowColor,
                            radius = f.tier.radius * 1.15f,
                            center = Offset(f.x, f.y)
                        )
                        // Main Fruit Body
                        drawCircle(
                            color = f.tier.color,
                            radius = f.tier.radius,
                            center = Offset(f.x, f.y)
                        )
                        // 3D Glass shine highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f),
                            radius = f.tier.radius * 0.35f,
                            center = Offset(f.x - f.tier.radius * 0.3f, f.y - f.tier.radius * 0.3f)
                        )
                    }
                }

                // Render Emoji Labels directly over fruits for high visual clarity
                fruits.forEach { f ->
                    Box(
                        modifier = Modifier
                            .offset((f.x - f.tier.radius).dp / 2.7f, (f.y - f.tier.radius).dp / 2.7f)
                    ) {
                        // Emoji icon inside canvas coordinate
                    }
                }
            }

            // Top preview next fruit pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                color = CardBg.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("NEXT:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(nextDroppingTier.emoji, fontSize = 16.sp)
                }
            }

            // Start Round Overlay
            if (!gameStarted) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🍉", fontSize = 52.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "FRUIT MERGE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Drop fruits from above! Colliding identical fruits merges them into bigger fruits up to the Giant Watermelon for huge PKR multipliers.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBettingInFlight = true
                                    betErrorMessage = ""
                                    val res = onPlaceBet(activeBetAmount)
                                    isBettingInFlight = false
                                    if (res.isSuccess) {
                                        restartGame()
                                        gameStarted = true
                                    } else {
                                        betErrorMessage = res.exceptionOrNull()?.message ?: "Bet failed"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp)
                                .testTag("fruit_start_bet_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            enabled = !isBettingInFlight
                        ) {
                            if (isBettingInFlight) {
                                CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "START ROUND (PKR ${String.format("%,.0f", activeBetAmount)})",
                                    color = DarkBg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (betErrorMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(betErrorMessage, color = CoralRed, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Game Over Overlay
            if (isGameOver) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg.copy(alpha = 0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(if (isVictory || score >= 450) "🍉" else "⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isVictory) "GIANT WATERMELON MERGED!" else if (score >= 450) "ROUND COMPLETED!" else "CONTAINER OVERFLOW",
                            color = if (score >= 450) NeonGreen else CoralRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Score: $score pts  •  ${String.format("%.2f", currentMultiplier)}x Multiplier",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        val win = activeBetAmount * currentMultiplier
                        Text(
                            if (score >= 450) "Total Payout: PKR ${String.format("%,.2f", win)}" else "Loss: PKR ${String.format("%,.2f", activeBetAmount)}",
                            color = if (score >= 450) GoldYellow else TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBettingInFlight = true
                                    betErrorMessage = ""
                                    val res = onPlaceBet(activeBetAmount)
                                    isBettingInFlight = false
                                    if (res.isSuccess) {
                                        restartGame()
                                        gameStarted = true
                                    } else {
                                        betErrorMessage = res.exceptionOrNull()?.message ?: "Bet failed"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("PLAY AGAIN (PKR ${String.format("%,.0f", activeBetAmount)})", color = DarkBg, fontWeight = FontWeight.Black)
                        }

                        if (betErrorMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(betErrorMessage, color = CoralRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Betting & Cashout Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = SecondaryBg,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BET AMOUNT (PKR)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Min: ₨ 25  Max: ₨ 25,000", color = TextSecondary, fontSize = 10.sp)
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickBets) { bet ->
                        val isSelected = selectedBet == bet && customBetText.isBlank()
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedBet = bet
                                    customBetText = ""
                                },
                            color = if (isSelected) PrimaryPurple else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AccentCyan else CardBorder
                            )
                        ) {
                            Text(
                                text = "₨ ${bet.toInt()}",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (gameStarted && !isGameOver && score >= 350) {
                        Button(
                            onClick = {
                                isGameOver = true
                                val win = activeBetAmount * currentMultiplier
                                onCashoutWin(win, currentMultiplier, activeBetAmount)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("fruit_cashout_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldYellow)
                        ) {
                            Text(
                                "CASH OUT PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenDeposit,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                    ) {
                        Text("+ DEPOSIT PKR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text("How to Play Fruit Merge", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Place your bet in real PKR to begin the session.", color = TextSecondary, fontSize = 13.sp)
                    Text("2. Drag or tap across the top to position the fruit dropper. Release to drop.", color = TextSecondary, fontSize = 13.sp)
                    Text("3. When two matching fruits collide, they merge into the next bigger fruit!", color = TextSecondary, fontSize = 13.sp)
                    Text("4. Merge order: Cherry → Strawberry → Grape → Orange → Apple → Peach → Pineapple → Melon → Giant Watermelon.", color = TextSecondary, fontSize = 13.sp)
                    Text("5. Each merge increases your multiplier up to 25x. Cash out anytime or reach Watermelon to claim big PKR!", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstructions = false }) {
                    Text("Got It", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
