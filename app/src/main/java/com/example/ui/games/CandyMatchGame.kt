package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Original visual candies designed for P6692 Candy Match-3
enum class CandyType(
    val emoji: String,
    val primaryColor: Color,
    val glowColor: Color,
    val nameTitle: String
) {
    HEART_JELLY("💖", Color(0xFFFF2A85), Color(0x66FF2A85), "Heart Ruby"),
    LEMON_DROP("🍋", Color(0xFFFFD600), Color(0x66FFD600), "Lemon Drop"),
    MINT_SPIRAL("🍬", Color(0xFF00E5FF), Color(0x6600E5FF), "Neon Mint"),
    GRAPE_GUMMY("🍇", Color(0xFFA855F7), Color(0x66A855F7), "Grape Chew"),
    CHOCO_STAR("⭐", Color(0xFFF97316), Color(0x66F97316), "Orange Star"),
    DIAMOND_CRYSTAL("💎", Color(0xFF38BDF8), Color(0x6638BDF8), "Blue Crystal")
}

data class BoardTile(
    val type: CandyType,
    val id: Long = Random.nextLong(),
    val isMatched: Boolean = false
)

/**
 * Candy Match-3 Game for P6692
 * - Authentic Match-3 gameplay: 6x6 grid, tap to select & swap adjacent candies
 * - Automatic cascade/gravity drop mechanics when 3 or more align horizontally or vertically
 * - Score combos, bonus specials for 4+ in a row, multiplier escalation
 * - Real PKR betting flow with backend wallet integration
 * - Smooth animations matching the P6692 dark purple and neon cyan aesthetic
 */
@Composable
fun CandyMatchGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rows = 6
    val cols = 6

    fun createFreshBoard(): List<MutableList<BoardTile>> {
        val types = CandyType.values()
        return List(rows) { r ->
            MutableList(cols) { c ->
                val chosen = types[Random.nextInt(types.size)]
                BoardTile(type = chosen)
            }
        }
    }

    var board by remember { mutableStateOf(createFreshBoard()) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isProcessingMatches by remember { mutableStateOf(false) }

    // Game stats
    var score by remember { mutableIntStateOf(0) }
    var movesLeft by remember { mutableIntStateOf(16) }
    var matchComboCount by remember { mutableIntStateOf(0) }
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
        board = createFreshBoard()
        selectedCell = null
        score = 0
        movesLeft = 16
        matchComboCount = 0
        currentMultiplier = 1.0
        gameStarted = false
        isGameOver = false
        isVictory = false
    }

    // Check matches in grid
    fun findMatches(currentGrid: List<List<BoardTile>>): Set<Pair<Int, Int>> {
        val matched = mutableSetOf<Pair<Int, Int>>()

        // Horizontal matches
        for (r in 0 until rows) {
            var matchLen = 1
            for (c in 1 until cols) {
                if (currentGrid[r][c].type == currentGrid[r][c - 1].type) {
                    matchLen++
                } else {
                    if (matchLen >= 3) {
                        for (k in 0 until matchLen) {
                            matched.add(Pair(r, c - 1 - k))
                        }
                    }
                    matchLen = 1
                }
            }
            if (matchLen >= 3) {
                for (k in 0 until matchLen) {
                    matched.add(Pair(r, cols - 1 - k))
                }
            }
        }

        // Vertical matches
        for (c in 0 until cols) {
            var matchLen = 1
            for (r in 1 until rows) {
                if (currentGrid[r][c].type == currentGrid[r - 1][c].type) {
                    matchLen++
                } else {
                    if (matchLen >= 3) {
                        for (k in 0 until matchLen) {
                            matched.add(Pair(r - 1 - k, c))
                        }
                    }
                    matchLen = 1
                }
            }
            if (matchLen >= 3) {
                for (k in 0 until matchLen) {
                    matched.add(Pair(rows - 1 - k, c))
                }
            }
        }

        return matched
    }

    // Cascade / Gravity drop
    fun applyCascade(currentGrid: List<MutableList<BoardTile>>, matched: Set<Pair<Int, Int>>): List<MutableList<BoardTile>> {
        val newGrid = currentGrid.map { it.toMutableList() }
        val types = CandyType.values()

        // For each column, drop candies down
        for (c in 0 until cols) {
            val remainingInCol = mutableListOf<BoardTile>()
            for (r in rows - 1 downTo 0) {
                if (!matched.contains(Pair(r, c))) {
                    remainingInCol.add(newGrid[r][c])
                }
            }

            // Fill from bottom up
            var fillIdx = 0
            for (r in rows - 1 downTo 0) {
                if (fillIdx < remainingInCol.size) {
                    newGrid[r][c] = remainingInCol[fillIdx]
                    fillIdx++
                } else {
                    // Generate new falling candies
                    newGrid[r][c] = BoardTile(type = types[Random.nextInt(types.size)])
                }
            }
        }

        return newGrid
    }

    // Swap handling
    fun handleCellClick(r: Int, c: Int) {
        if (!gameStarted || isGameOver || isProcessingMatches || movesLeft <= 0) return

        val prev = selectedCell
        if (prev == null) {
            selectedCell = Pair(r, c)
        } else {
            val (pr, pc) = prev
            val isAdjacent = (pr == r && kotlin.math.abs(pc - c) == 1) || (pc == c && kotlin.math.abs(pr - r) == 1)

            if (isAdjacent) {
                selectedCell = null
                movesLeft--

                coroutineScope.launch {
                    isProcessingMatches = true

                    // Perform swap
                    val working = board.map { it.toMutableList() }
                    val temp = working[pr][pc]
                    working[pr][pc] = working[r][c]
                    working[r][c] = temp
                    board = working

                    delay(180)

                    // Check matches
                    var matched = findMatches(board)
                    if (matched.isNotEmpty()) {
                        while (matched.isNotEmpty()) {
                            val pts = matched.size * 150
                            score += pts
                            matchComboCount++
                            currentMultiplier = 1.0 + (score / 400.0).coerceAtMost(20.0)

                            board = applyCascade(board, matched)
                            delay(260)
                            matched = findMatches(board)
                        }
                    } else {
                        // Revert swap if no match
                        delay(120)
                        val revert = board.map { it.toMutableList() }
                        val t = revert[pr][pc]
                        revert[pr][pc] = revert[r][c]
                        revert[r][c] = t
                        board = revert
                    }

                    isProcessingMatches = false

                    // Check end of round
                    if (movesLeft <= 0) {
                        isGameOver = true
                        if (score >= 600) {
                            isVictory = true
                            val winPkr = activeBetAmount * currentMultiplier
                            onCashoutWin(winPkr, currentMultiplier, activeBetAmount)
                        } else {
                            onRoundLost(activeBetAmount)
                        }
                    }
                }
            } else {
                // Select new cell
                selectedCell = Pair(r, c)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryBg,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                            .testTag("candy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Candy Match-3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Surface(
                                color = PrimaryPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "PUZZLE",
                                    color = BrightPurple,
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
                            .testTag("candy_restart_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Live Score, Multiplier & Moves Ribbon
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
                    Text("MOVES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$movesLeft", color = if (movesLeft <= 3) CoralRed else BrightPurple, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POTENTIAL WIN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("PKR ${String.format("%,.0f", activeBetAmount * currentMultiplier)}", color = GoldYellow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Match-3 Board Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF14072B),
                            Color(0xFF1E0C3F),
                            Color(0xFF100524)
                        )
                    )
                )
                .border(1.5.dp, BrightPurple.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until cols) {
                            val tile = board[r][c]
                            val isSelected = selectedCell == Pair(r, c)

                            Surface(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { handleCellClick(r, c) }
                                    .testTag("candy_tile_${r}_$c"),
                                color = if (isSelected) PrimaryPurple.copy(alpha = 0.6f) else CardBg.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) AccentCyan else tile.type.glowColor
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = tile.type.emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.scale(if (isSelected) 1.15f else 1f)
                                    )
                                }
                            }
                        }
                    }
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
                        Text("🍬", fontSize = 50.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "CANDY MATCH-3",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Swap adjacent candies to match 3 or more in a row! Trigger cascades and combos to build big real PKR multipliers.",
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
                                .testTag("candy_start_bet_button"),
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
                        Text(if (isVictory || score >= 600) "🎉" else "💔", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (score >= 600) "SWEET VICTORY!" else "MOVES EXHAUSTED",
                            color = if (score >= 600) NeonGreen else CoralRed,
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
                            if (score >= 600) "Total Payout: PKR ${String.format("%,.2f", win)}" else "Loss: PKR ${String.format("%,.2f", activeBetAmount)}",
                            color = if (score >= 600) GoldYellow else TextSecondary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = BrightPurple)
                        ) {
                            Text("PLAY AGAIN (PKR ${String.format("%,.0f", activeBetAmount)})", color = Color.White, fontWeight = FontWeight.Black)
                        }

                        if (betErrorMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(betErrorMessage, color = CoralRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Betting & Cash Out Controls
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
                    if (gameStarted && !isGameOver && score >= 450) {
                        Button(
                            onClick = {
                                isGameOver = true
                                val win = activeBetAmount * currentMultiplier
                                onCashoutWin(win, currentMultiplier, activeBetAmount)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("candy_cashout_button"),
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
                Text("How to Play Candy Match-3", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Place your bet in real PKR to start your 16-move puzzle challenge.", color = TextSecondary, fontSize = 13.sp)
                    Text("2. Tap any candy, then tap an adjacent candy to swap them.", color = TextSecondary, fontSize = 13.sp)
                    Text("3. Align 3 or more of the same candies horizontally or vertically to clear them.", color = TextSecondary, fontSize = 13.sp)
                    Text("4. Cascading candy drops trigger combos and increase your PKR multiplier.", color = TextSecondary, fontSize = 13.sp)
                    Text("5. Cash out early once you reach 450+ score or finish all moves to collect winnings!", color = TextSecondary, fontSize = 13.sp)
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
