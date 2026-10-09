package com.example.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

enum class LudoPlayer(val displayName: String, val color: Color, val homeOffset: Offset, val startSquare: Int) {
    RED("Red", Color(0xFFEF4444), Offset(0.18f, 0.18f), 1),
    GREEN("Green", Color(0xFF10B981), Offset(0.82f, 0.18f), 14),
    YELLOW("Yellow", Color(0xFFFBBF24), Offset(0.82f, 0.82f), 27),
    BLUE("Blue", Color(0xFF38BDF8), Offset(0.18f, 0.82f), 40)
}

data class LudoToken(
    val id: Int, // 0..3 per player
    val player: LudoPlayer,
    var position: Int = -1, // -1: in yard/base, 0..51: main track, 100..105: home column, 106: finished
)

@Composable
fun LudoGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Number of players: 2, 3, or 4
    var playerCount by remember { mutableIntStateOf(4) }
    val activePlayers = remember(playerCount) {
        when (playerCount) {
            2 -> listOf(LudoPlayer.RED, LudoPlayer.YELLOW)
            3 -> listOf(LudoPlayer.RED, LudoPlayer.GREEN, LudoPlayer.YELLOW)
            else -> listOf(LudoPlayer.RED, LudoPlayer.GREEN, LudoPlayer.YELLOW, LudoPlayer.BLUE)
        }
    }

    var currentTurnIndex by remember { mutableIntStateOf(0) }
    val currentPlayer = activePlayers[currentTurnIndex % activePlayers.size]

    var diceValue by remember { mutableIntStateOf(6) }
    var hasRolledDice by remember { mutableStateOf(false) }
    var isDiceRolling by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Roll the dice to start!") }
    var winnerPlayer by remember { mutableStateOf<LudoPlayer?>(null) }

    // Tokens state: 4 tokens per player
    val tokens = remember {
        mutableStateListOf<LudoToken>().apply {
            LudoPlayer.values().forEach { player ->
                for (i in 0..3) {
                    add(LudoToken(id = i, player = player, position = -1))
                }
            }
        }
    }

    // Safe squares on main track: 1, 9, 14, 22, 27, 35, 40, 48
    val safeSquares = setOf(1, 9, 14, 22, 27, 35, 40, 48)

    fun checkWinner(): LudoPlayer? {
        for (player in activePlayers) {
            val playerTokens = tokens.filter { it.player == player }
            if (playerTokens.all { it.position == 106 }) {
                return player
            }
        }
        return null
    }

    fun nextTurn(bonusRoll: Boolean = false) {
        hasRolledDice = false
        val won = checkWinner()
        if (won != null) {
            winnerPlayer = won
            statusMessage = "${won.displayName} Player Won the Match! 🎉"
            return
        }

        if (bonusRoll) {
            statusMessage = "Bonus turn! Roll again."
        } else {
            currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size
            statusMessage = "${activePlayers[currentTurnIndex % activePlayers.size].displayName}'s turn to roll."
        }
    }

    fun moveToken(token: LudoToken) {
        if (!hasRolledDice || winnerPlayer != null) return

        var bonusTurn = false

        if (token.position == -1) {
            // Can only leave yard with a 6
            if (diceValue == 6) {
                token.position = token.player.startSquare
                bonusTurn = true
                statusMessage = "${token.player.displayName} token entered track!"
            } else {
                return
            }
        } else if (token.position in 0..51) {
            // Main track movement
            val targetPos = (token.position + diceValue) % 52
            // Check capture
            if (!safeSquares.contains(targetPos)) {
                val opponentToken = tokens.find { it.position == targetPos && it.player != token.player }
                if (opponentToken != null) {
                    opponentToken.position = -1 // Captured back to yard
                    bonusTurn = true
                    statusMessage = "Captured ${opponentToken.player.displayName} token! +Bonus roll!"
                }
            }
            token.position = targetPos
        } else if (token.position in 100..105) {
            // Home stretch
            if (token.position + diceValue <= 106) {
                token.position += diceValue
                if (token.position == 106) {
                    bonusTurn = true
                    statusMessage = "Token reached home! 🎉"
                }
            } else {
                return
            }
        }

        if (diceValue == 6) bonusTurn = true
        nextTurn(bonusRoll = bonusTurn)
    }

    fun rollDice() {
        if (hasRolledDice || winnerPlayer != null) return
        isDiceRolling = true
        val roll = Random.nextInt(1, 7)
        diceValue = roll
        isDiceRolling = false
        hasRolledDice = true

        val myTokens = tokens.filter { it.player == currentPlayer }
        val canMoveAny = myTokens.any { token ->
            if (token.position == -1) roll == 6
            else if (token.position in 100..105) token.position + roll <= 106
            else token.position != 106
        }

        if (!canMoveAny) {
            statusMessage = "No legal moves for ${currentPlayer.displayName}. Next turn!"
            nextTurn(bonusRoll = false)
        } else {
            statusMessage = "Select a highlighted token to move by $roll squares."
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
                        modifier = Modifier.testTag("ludo_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LUDO MASTER CLUB",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Local Multiplayer (2-4 Players)",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            tokens.forEach { it.position = -1 }
                            currentTurnIndex = 0
                            hasRolledDice = false
                            winnerPlayer = null
                            diceValue = 6
                            statusMessage = "Roll the dice to start!"
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Game", tint = GoldYellow)
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
            // Player Selector Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Players:", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                listOf(2, 3, 4).forEach { count ->
                    FilterChip(
                        selected = playerCount == count,
                        onClick = {
                            playerCount = count
                            tokens.forEach { it.position = -1 }
                            currentTurnIndex = 0
                            hasRolledDice = false
                            winnerPlayer = null
                        },
                        label = { Text("${count}P", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Current Turn & Status Badge
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = CardBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, currentPlayer.color)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(currentPlayer.color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${currentPlayer.displayName}'s Turn",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = statusMessage,
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Ludo Board: 15x15 Classic Canvas
            Box(
                modifier = Modifier
                    .size(330.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0B18))
                    .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val cell = w / 15f

                    // Draw 4 Corner Yards (6x6 each)
                    // Red (Top-Left)
                    drawRect(Color(0xFFEF4444), Offset(0f, 0f), Size(cell * 6, cell * 6))
                    drawRect(Color(0xFF1D1233), Offset(cell, cell), Size(cell * 4, cell * 4))

                    // Green (Top-Right)
                    drawRect(Color(0xFF10B981), Offset(cell * 9, 0f), Size(cell * 6, cell * 6))
                    drawRect(Color(0xFF1D1233), Offset(cell * 10, cell), Size(cell * 4, cell * 4))

                    // Blue (Bottom-Left)
                    drawRect(Color(0xFF38BDF8), Offset(0f, cell * 9), Size(cell * 6, cell * 6))
                    drawRect(Color(0xFF1D1233), Offset(cell, cell * 10), Size(cell * 4, cell * 4))

                    // Yellow (Bottom-Right)
                    drawRect(Color(0xFFFBBF24), Offset(cell * 9, cell * 9), Size(cell * 6, cell * 6))
                    drawRect(Color(0xFF1D1233), Offset(cell * 10, cell * 10), Size(cell * 4, cell * 4))

                    // Center Home Triangle (3x3 at center)
                    drawRect(Color(0xFF281945), Offset(cell * 6, cell * 6), Size(cell * 3, cell * 3))

                    // Grid cell borders
                    for (x in 0..15) {
                        drawLine(Color(0x333B2666), Offset(x * cell, 0f), Offset(x * cell, size.height), 1f)
                    }
                    for (y in 0..15) {
                        drawLine(Color(0x333B2666), Offset(0f, y * cell), Offset(size.width, y * cell), 1f)
                    }

                    // Home columns
                    for (i in 1..5) {
                        drawRect(Color(0xFFEF4444).copy(alpha = 0.5f), Offset(cell * i, cell * 7), Size(cell, cell))
                        drawRect(Color(0xFF10B981).copy(alpha = 0.5f), Offset(cell * 7, cell * i), Size(cell, cell))
                        drawRect(Color(0xFFFBBF24).copy(alpha = 0.5f), Offset(cell * (14 - i), cell * 7), Size(cell, cell))
                        drawRect(Color(0xFF38BDF8).copy(alpha = 0.5f), Offset(cell * 7, cell * (14 - i)), Size(cell, cell))
                    }
                }

                // Interactive Token Markers on Board
                // Render Tokens for Active Players
                for (token in tokens.filter { activePlayers.contains(it.player) }) {
                    val canMove = hasRolledDice && token.player == currentPlayer && (
                            (token.position == -1 && diceValue == 6) ||
                                    (token.position in 0..51) ||
                                    (token.position in 100..105 && token.position + diceValue <= 106)
                            )

                    // Compute pixel offset on 330dp board
                    val (xDp, yDp) = when {
                        token.position == -1 -> {
                            // In base yard
                            val baseOffsetX = when (token.player) {
                                LudoPlayer.RED, LudoPlayer.BLUE -> 30 + (token.id % 2) * 45
                                LudoPlayer.GREEN, LudoPlayer.YELLOW -> 220 + (token.id % 2) * 45
                            }
                            val baseOffsetY = when (token.player) {
                                LudoPlayer.RED, LudoPlayer.GREEN -> 30 + (token.id / 2) * 45
                                LudoPlayer.BLUE, LudoPlayer.YELLOW -> 220 + (token.id / 2) * 45
                            }
                            Pair(baseOffsetX.dp, baseOffsetY.dp)
                        }
                        token.position == 106 -> Pair(150.dp, 150.dp) // Home finished
                        else -> {
                            // On track
                            val angle = (token.position * (360.0 / 52.0)) * (Math.PI / 180.0)
                            val r = 110.0
                            val cx = 150.0 + r * Math.cos(angle)
                            val cy = 150.0 + r * Math.sin(angle)
                            Pair(cx.dp, cy.dp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = xDp, y = yDp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(token.player.color)
                            .border(
                                width = if (canMove) 2.5.dp else 1.dp,
                                color = if (canMove) Color.White else Color.Black,
                                shape = CircleShape
                            )
                            .clickable(enabled = canMove) {
                                moveToken(token)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${token.id + 1}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dice Rolling Station
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive 3D Dice Box
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !hasRolledDice && winnerPlayer == null) { rollDice() }
                        .testTag("roll_ludo_dice_button"),
                    color = CardBgLight,
                    border = androidx.compose.foundation.BorderStroke(2.dp, currentPlayer.color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val diceIcon = when (diceValue) {
                            1 -> "⚀"
                            2 -> "⚁"
                            3 -> "⚂"
                            4 -> "⚃"
                            5 -> "⚄"
                            else -> "⚅"
                        }
                        Text(
                            text = diceIcon,
                            fontSize = 42.sp,
                            color = if (diceValue == 6) GoldYellow else Color.White
                        )
                    }
                }

                // Roll Action Button
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { rollDice() },
                        enabled = !hasRolledDice && winnerPlayer == null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (hasRolledDice) "DICE ROLLED: $diceValue" else "ROLL DICE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = if (hasRolledDice) "Tap token to advance" else "Tap dice or button to roll",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
