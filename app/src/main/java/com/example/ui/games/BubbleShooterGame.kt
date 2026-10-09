package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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

// Bubble visual definition
enum class BubbleColor(val displayColor: Color, val glowColor: Color, val emoji: String, val label: String) {
    CYAN(Color(0xFF00E5FF), Color(0x6600E5FF), "💎", "Cyan"),
    PURPLE(Color(0xFFB56CFF), Color(0x66B56CFF), "🔮", "Purple"),
    PINK(Color(0xFFFF2E93), Color(0x66FF2E93), "🌸", "Pink"),
    YELLOW(Color(0xFFFFD600), Color(0x66FFD600), "⚡", "Yellow"),
    GREEN(Color(0xFF00E676), Color(0x6600E676), "🍀", "Green")
}

data class BubbleCell(
    val color: BubbleColor?,
    val isPopping: Boolean = false
)

data class ProjectileBubble(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor
)

/**
 * Bubble Shooter Game for P6692
 * - Authentic arcade mechanics: Aiming laser, bounce off walls, grid snapping, cluster matching (3+)
 * - Score tracking, combo multipliers, restart action, instructions dialog
 * - Real PKR betting & bankroll integration with backend wallet
 * - Premium dark purple & neon cyan aesthetic
 */
@Composable
fun BubbleShooterGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val numRows = 8
    val numCols = 8
    val bubbleRadius = 18f

    // Grid state: List of rows, each having numCols BubbleCell
    fun createInitialGrid(): List<MutableList<BubbleCell>> {
        val colors = BubbleColor.values()
        return List(numRows) { r ->
            MutableList(numCols) { c ->
                if (r < 4) {
                    BubbleCell(color = colors[Random.nextInt(colors.size)])
                } else {
                    BubbleCell(color = null)
                }
            }
        }
    }

    var grid by remember { mutableStateOf(createInitialGrid()) }
    var currentShooterColor by remember { mutableStateOf(BubbleColor.values()[Random.nextInt(BubbleColor.values().size)]) }
    var nextShooterColor by remember { mutableStateOf(BubbleColor.values()[Random.nextInt(BubbleColor.values().size)]) }

    // Aiming angle in degrees (-75 to +75 from top vertical, 0 = straight up)
    var aimAngleDeg by remember { mutableFloatStateOf(0f) }
    var isAiming by remember { mutableStateOf(false) }
    var activeProjectile by remember { mutableStateOf<ProjectileBubble?>(null) }

    // Game stats
    var score by remember { mutableIntStateOf(0) }
    var bubblesPoppedTotal by remember { mutableIntStateOf(0) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var shotsLeft by remember { mutableIntStateOf(15) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
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
        grid = createInitialGrid()
        score = 0
        bubblesPoppedTotal = 0
        currentMultiplier = 1.0
        shotsLeft = 15
        isGameOver = false
        isVictory = false
        gameStarted = false
        activeProjectile = null
        currentShooterColor = BubbleColor.values()[Random.nextInt(BubbleColor.values().size)]
        nextShooterColor = BubbleColor.values()[Random.nextInt(BubbleColor.values().size)]
    }

    // Cluster matching helper (BFS)
    fun findConnectedMatches(startR: Int, startC: Int, targetColor: BubbleColor): List<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startR, startC))
        visited.add(Pair(startR, startC))

        val neighborsOffsets = listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        )

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            for ((dr, dc) in neighborsOffsets) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until numRows && nc in 0 until numCols) {
                    val neighbor = grid[nr][nc]
                    if (neighbor.color == targetColor && !visited.contains(Pair(nr, nc))) {
                        visited.add(Pair(nr, nc))
                        queue.add(Pair(nr, nc))
                    }
                }
            }
        }
        return visited.toList()
    }

    // Launch shooting
    fun shootBubble(boardWidth: Float, boardHeight: Float) {
        if (shotsLeft <= 0 || isGameOver || activeProjectile != null) return

        val speed = 24f
        val rad = Math.toRadians((aimAngleDeg - 90.0)).toFloat()
        val vx = cos(rad) * speed
        val vy = sin(rad) * speed

        val startX = boardWidth / 2f
        val startY = boardHeight - 35f

        activeProjectile = ProjectileBubble(
            x = startX,
            y = startY,
            vx = vx,
            vy = vy,
            color = currentShooterColor
        )
        shotsLeft--
    }

    // Projectile physics loop
    LaunchedEffect(activeProjectile) {
        val proj = activeProjectile ?: return@LaunchedEffect
        while (activeProjectile != null) {
            delay(16)
            proj.x += proj.vx
            proj.y += proj.vy

            // Wall bounce
            val minX = 25f
            val maxX = 350f
            if (proj.x - bubbleRadius < minX) {
                proj.x = minX + bubbleRadius
                activeProjectile = proj.copy(vx = -proj.vx)
            } else if (proj.x + bubbleRadius > maxX) {
                proj.x = maxX - bubbleRadius
                activeProjectile = proj.copy(vx = -proj.vx)
            }

            // Snap condition: hit ceiling or hit any existing bubble
            var shouldSnap = false
            var snapRow = 0
            var snapCol = 0

            if (proj.y - bubbleRadius <= 40f) {
                shouldSnap = true
                snapRow = 0
                val colW = (maxX - minX) / numCols
                snapCol = ((proj.x - minX) / colW).toInt().coerceIn(0, numCols - 1)
            } else {
                // Check distance against each filled bubble
                val colW = (maxX - minX) / numCols
                val rowH = 36f
                for (r in 0 until numRows) {
                    for (c in 0 until numCols) {
                        if (grid[r][c].color != null) {
                            val bubbleX = minX + c * colW + colW / 2f
                            val bubbleY = 40f + r * rowH + rowH / 2f
                            val dist = hypot(proj.x - bubbleX, proj.y - bubbleY)
                            if (dist < bubbleRadius * 1.85f) {
                                shouldSnap = true
                                // Find nearest empty slot adjacent
                                val targetR = (r + 1).coerceAtMost(numRows - 1)
                                snapRow = if (grid[targetR][c].color == null) targetR else r
                                snapCol = ((proj.x - minX) / colW).toInt().coerceIn(0, numCols - 1)
                                break
                            }
                        }
                    }
                    if (shouldSnap) break
                }
            }

            if (shouldSnap) {
                // Place bubble in grid
                val newGrid = grid.map { it.toMutableList() }
                if (newGrid[snapRow][snapCol].color == null) {
                    newGrid[snapRow][snapCol] = BubbleCell(color = proj.color)
                } else {
                    // search adjacent open slot
                    var placed = false
                    for (r in (snapRow - 1).coerceAtLeast(0)..(snapRow + 1).coerceAtMost(numRows - 1)) {
                        for (c in 0 until numCols) {
                            if (newGrid[r][c].color == null) {
                                newGrid[r][c] = BubbleCell(color = proj.color)
                                snapRow = r
                                snapCol = c
                                placed = true
                                break
                            }
                        }
                        if (placed) break
                    }
                }

                // Check for matches of 3 or more
                val matches = findConnectedMatches(snapRow, snapCol, proj.color)
                if (matches.size >= 3) {
                    matches.forEach { (r, c) ->
                        newGrid[r][c] = BubbleCell(color = null)
                    }
                    val poppedCount = matches.size
                    val points = poppedCount * 120
                    score += points
                    bubblesPoppedTotal += poppedCount
                    currentMultiplier = 1.0 + (score / 350.0).coerceAtMost(15.0)
                }

                grid = newGrid
                activeProjectile = null

                // Cycle shooter colors
                currentShooterColor = nextShooterColor
                nextShooterColor = BubbleColor.values()[Random.nextInt(BubbleColor.values().size)]

                // Check Win condition (all cleared)
                val remainingCount = grid.sumOf { row -> row.count { it.color != null } }
                if (remainingCount == 0) {
                    isVictory = true
                    isGameOver = true
                    val winPkr = activeBetAmount * currentMultiplier
                    onCashoutWin(winPkr, currentMultiplier, activeBetAmount)
                } else if (shotsLeft <= 0) {
                    isGameOver = true
                    if (score >= 400) {
                        val winPkr = activeBetAmount * currentMultiplier
                        onCashoutWin(winPkr, currentMultiplier, activeBetAmount)
                    } else {
                        onRoundLost(activeBetAmount)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top Game Header
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
                            .testTag("bubble_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bubble Shooter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "ARCADE",
                                    color = AccentCyan,
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
                            .testTag("bubble_restart_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Live Score, Multiplier & Shots Info Ribbon
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
                    Text("SHOTS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$shotsLeft", color = if (shotsLeft <= 3) CoralRed else NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POTENTIAL WIN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}", color = GoldYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Bubble Playfield Area
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
                            Color(0xFF190F3A),
                            Color(0xFF0D0622)
                        )
                    )
                )
                .border(1.5.dp, CardBorder, RoundedCornerShape(18.dp))
        ) {
            val boardWidth = 360f
            val boardHeight = 440f

            // Gesture detector for aiming & shooting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameStarted, isGameOver) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (gameStarted && !isGameOver) {
                                    isAiming = true
                                    val dx = offset.x - size.width / 2f
                                    val dy = offset.y - (size.height - 40f)
                                    val deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                                    aimAngleDeg = deg.coerceIn(-75f, 75f)
                                }
                            },
                            onDrag = { change, _ ->
                                if (gameStarted && !isGameOver) {
                                    val dx = change.position.x - size.width / 2f
                                    val dy = change.position.y - (size.height - 40f)
                                    val deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                                    aimAngleDeg = deg.coerceIn(-75f, 75f)
                                }
                            },
                            onDragEnd = {
                                if (gameStarted && !isGameOver && isAiming) {
                                    isAiming = false
                                    shootBubble(size.width.toFloat(), size.height.toFloat())
                                }
                            }
                        )
                    }
                    .pointerInput(gameStarted, isGameOver) {
                        detectTapGestures { offset ->
                            if (gameStarted && !isGameOver) {
                                val dx = offset.x - size.width / 2f
                                val dy = offset.y - (size.height - 40f)
                                val deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                                aimAngleDeg = deg.coerceIn(-75f, 75f)
                                shootBubble(size.width.toFloat(), size.height.toFloat())
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val colW = w / numCols
                    val rowH = 34f

                    // 1. Draw top ceiling boundary line
                    drawLine(
                        color = BrightPurple.copy(alpha = 0.5f),
                        start = Offset(0f, 32f),
                        end = Offset(w, 32f),
                        strokeWidth = 3f
                    )

                    // 2. Draw Bubbles in Grid
                    for (r in 0 until numRows) {
                        for (c in 0 until numCols) {
                            val cell = grid[r][c]
                            if (cell.color != null) {
                                val cx = c * colW + colW / 2f
                                val cy = 40f + r * rowH + rowH / 2f

                                // Outer glow
                                drawCircle(
                                    color = cell.color.glowColor,
                                    radius = bubbleRadius * 1.25f,
                                    center = Offset(cx, cy)
                                )
                                // Main bubble
                                drawCircle(
                                    color = cell.color.displayColor,
                                    radius = bubbleRadius,
                                    center = Offset(cx, cy)
                                )
                                // Inner highlight for glossy 3D effect
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.65f),
                                    radius = bubbleRadius * 0.35f,
                                    center = Offset(cx - bubbleRadius * 0.35f, cy - bubbleRadius * 0.35f)
                                )
                            }
                        }
                    }

                    // 3. Draw Projectile if currently flying
                    activeProjectile?.let { proj ->
                        drawCircle(
                            color = proj.color.glowColor,
                            radius = bubbleRadius * 1.35f,
                            center = Offset(proj.x, proj.y)
                        )
                        drawCircle(
                            color = proj.color.displayColor,
                            radius = bubbleRadius,
                            center = Offset(proj.x, proj.y)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            radius = bubbleRadius * 0.35f,
                            center = Offset(proj.x - bubbleRadius * 0.35f, proj.y - bubbleRadius * 0.35f)
                        )
                    }

                    // 4. Draw Aiming Laser Guide line
                    if (!isGameOver && gameStarted) {
                        val shooterX = w / 2f
                        val shooterY = h - 35f
                        val rad = Math.toRadians((aimAngleDeg - 90.0)).toFloat()
                        val guideLength = 220f
                        val guideEndX = shooterX + cos(rad) * guideLength
                        val guideEndY = shooterY + sin(rad) * guideLength

                        drawLine(
                            color = AccentCyan.copy(alpha = 0.75f),
                            start = Offset(shooterX, shooterY),
                            end = Offset(guideEndX, guideEndY),
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    }

                    // 5. Draw Shooter Base & Current / Next Bubbles
                    val shooterX = w / 2f
                    val shooterY = h - 35f

                    // Turret circle base
                    drawCircle(
                        color = SecondaryBg,
                        radius = 28f,
                        center = Offset(shooterX, shooterY)
                    )
                    // Loaded bubble
                    drawCircle(
                        color = currentShooterColor.glowColor,
                        radius = bubbleRadius * 1.2f,
                        center = Offset(shooterX, shooterY)
                    )
                    drawCircle(
                        color = currentShooterColor.displayColor,
                        radius = bubbleRadius,
                        center = Offset(shooterX, shooterY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = bubbleRadius * 0.35f,
                        center = Offset(shooterX - bubbleRadius * 0.35f, shooterY - bubbleRadius * 0.35f)
                    )

                    // Next bubble mini preview
                    drawCircle(
                        color = nextShooterColor.displayColor.copy(alpha = 0.85f),
                        radius = bubbleRadius * 0.65f,
                        center = Offset(shooterX + 50f, shooterY + 6f)
                    )
                }
            }

            // Start Overlay if game hasn't been placed with real PKR bet
            if (!gameStarted) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg.copy(alpha = 0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🎯", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "BUBBLE SHOOTER",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Aim, shoot and match 3+ bubbles to burst clusters and trigger real PKR multipliers!",
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
                                .testTag("bubble_start_bet_button"),
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

            // Game Over / Victory Overlay
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
                        Text(if (isVictory || score >= 400) "🏆" else "💥", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isVictory) "BOARD CLEARED!" else if (score >= 400) "ROUND COMPLETED!" else "OUT OF SHOTS",
                            color = if (score >= 400) NeonGreen else CoralRed,
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
                            if (score >= 400) "Total Payout: PKR ${String.format("%,.2f", win)}" else "Loss: PKR ${String.format("%,.2f", activeBetAmount)}",
                            color = if (score >= 400) GoldYellow else TextSecondary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
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

        // Betting & Round Controls Panel
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
                // Quick bet chips
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

                // Action buttons: Cash Out early (if game started and has accumulated multiplier) or Deposit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (gameStarted && !isGameOver && score >= 240) {
                        Button(
                            onClick = {
                                isGameOver = true
                                val win = activeBetAmount * currentMultiplier
                                onCashoutWin(win, currentMultiplier, activeBetAmount)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("bubble_cashout_button"),
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

    // Instructions Dialog
    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            containerColor = CardBg,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text("How to Play Bubble Shooter", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Place your bet in real PKR to start the 15-shot round.", color = TextSecondary, fontSize = 13.sp)
                    Text("2. Drag or tap on the board to aim the laser guide. Release to shoot.", color = TextSecondary, fontSize = 13.sp)
                    Text("3. Match 3 or more bubbles of the same color to burst them and earn score.", color = TextSecondary, fontSize = 13.sp)
                    Text("4. Each burst increases your payout multiplier up to 15x.", color = TextSecondary, fontSize = 13.sp)
                    Text("5. Clear the board or reach 400+ score before running out of shots to win real PKR!", color = TextSecondary, fontSize = 13.sp)
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
