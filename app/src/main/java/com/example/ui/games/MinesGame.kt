package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LogoSize
import com.example.ui.components.P6692Logo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MinesTile(
    val index: Int,
    val isMine: Boolean,
    var isRevealed: Boolean = false
)

enum class MinesGridSize(val rows: Int, val cols: Int, val totalTiles: Int, val label: String) {
    GRID_3X3(3, 3, 9, "3x3 Mini"),
    GRID_5X5(5, 5, 25, "5x5 Classic"),
    GRID_6X6(6, 6, 36, "6x6 Mega")
}

@Composable
fun MinesGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedGridSize by remember { mutableStateOf(MinesGridSize.GRID_5X5) }
    var mineCount by remember { mutableIntStateOf(3) }
    var selectedBet by remember { mutableDoubleStateOf(50.0) }
    var isPlaying by remember { mutableStateOf(false) }
    var tiles by remember { mutableStateOf(List(25) { MinesTile(it, false) }) }
    var safeRevealedCount by remember { mutableIntStateOf(0) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var gameOutcome by remember { mutableStateOf<String?>(null) } // "WIN", "HIT_MINE", null
    var isPlacingBet by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }

    // Multiplier Calculator based on remaining probabilities
    fun calculateMultiplier(revealed: Int, total: Int, mines: Int): Double {
        if (revealed == 0) return 1.0
        var mult = 1.0
        for (i in 0 until revealed) {
            val safeLeft = total - mines - i
            val totalLeft = total - i
            if (safeLeft <= 0) break
            mult *= (totalLeft.toDouble() / safeLeft.toDouble()) * 0.97
        }
        return (mult * 100).toInt() / 100.0
    }

    // Adjust mine count if grid size changes
    LaunchedEffect(selectedGridSize) {
        val maxAllowedMines = selectedGridSize.totalTiles - 2
        if (mineCount > maxAllowedMines) {
            mineCount = maxAllowedMines.coerceAtLeast(1)
        }
        if (!isPlaying) {
            tiles = List(selectedGridSize.totalTiles) { MinesTile(it, false) }
        }
    }

    fun startNewGame() {
        if (pkrBalance < selectedBet) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.0f", pkrBalance)}). Please deposit."
            return
        }

        betErrorMessage = ""
        isPlacingBet = true

        coroutineScope.launch {
            // Deduct via backend wallet
            val betResult = onPlaceBet(selectedBet)
            isPlacingBet = false
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Bet could not be placed."
                return@launch
            }

            val total = selectedGridSize.totalTiles
            val mineIndices = (0 until total).shuffled().take(mineCount).toSet()
            tiles = List(total) { idx ->
                MinesTile(index = idx, isMine = idx in mineIndices, isRevealed = false)
            }
            safeRevealedCount = 0
            currentMultiplier = 1.0
            isPlaying = true
            gameOutcome = null
        }
    }

    fun revealTile(index: Int) {
        if (!isPlaying || tiles[index].isRevealed) return

        val tile = tiles[index]
        val updatedTiles = tiles.toMutableList()
        updatedTiles[index] = tile.copy(isRevealed = true)
        tiles = updatedTiles

        if (tile.isMine) {
            // Hit a mine!
            tiles = tiles.map { it.copy(isRevealed = true) }
            isPlaying = false
            gameOutcome = "HIT_MINE"
            onRoundLost(selectedBet)
        } else {
            safeRevealedCount++
            currentMultiplier = calculateMultiplier(safeRevealedCount, selectedGridSize.totalTiles, mineCount)
            // Check if all safe cells are revealed
            if (safeRevealedCount == selectedGridSize.totalTiles - mineCount) {
                val payout = selectedBet * currentMultiplier
                tiles = tiles.map { it.copy(isRevealed = true) }
                isPlaying = false
                gameOutcome = "WIN"
                onCashoutWin(payout, currentMultiplier, selectedBet)
            }
        }
    }

    fun cashOut() {
        if (!isPlaying || safeRevealedCount == 0) return
        val payout = selectedBet * currentMultiplier
        tiles = tiles.map { it.copy(isRevealed = true) }
        isPlaying = false
        gameOutcome = "WIN"
        onCashoutWin(payout, currentMultiplier, selectedBet)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                P6692Logo(size = LogoSize.SMALL)
                Column {
                    Text(
                        text = "MINES",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Classic Diamond Grid",
                        color = AccentCyan,
                        fontSize = 10.sp
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
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Deposit", tint = NeonGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Live Multiplier & Estimated Payout Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MULTIPLIER", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (safeRevealedCount == 0) "1.00x" else String.format("%.2fx", currentMultiplier),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (safeRevealedCount > 0) NeonGreen else Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GEMS FOUND", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$safeRevealedCount / ${selectedGridSize.totalTiles - mineCount}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("POTENTIAL WIN", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "PKR ${String.format("%,.0f", selectedBet * currentMultiplier)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentCyan
                    )
                }
            }
        }

        if (betErrorMessage.isNotBlank()) {
            Text(
                text = betErrorMessage,
                color = CoralRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Mines Grid View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val tileSize = when (selectedGridSize) {
                MinesGridSize.GRID_3X3 -> 84.dp
                MinesGridSize.GRID_5X5 -> 56.dp
                MinesGridSize.GRID_6X6 -> 46.dp
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until selectedGridSize.rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0 until selectedGridSize.cols) {
                            val idx = row * selectedGridSize.cols + col
                            val tile = tiles.getOrNull(idx) ?: MinesTile(idx, false)

                            val cellBg = when {
                                tile.isRevealed && tile.isMine -> CoralRed.copy(alpha = 0.85f)
                                tile.isRevealed && !tile.isMine -> NeonGreen.copy(alpha = 0.3f)
                                isPlaying -> PrimaryPurple.copy(alpha = 0.25f)
                                else -> CardBg
                            }

                            val cellBorder = when {
                                tile.isRevealed && tile.isMine -> CoralRed
                                tile.isRevealed && !tile.isMine -> NeonGreen
                                else -> CardBorder
                            }

                            Surface(
                                modifier = Modifier
                                    .size(tileSize)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = isPlaying && !tile.isRevealed) { revealTile(idx) },
                                color = cellBg,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, cellBorder)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (tile.isRevealed) {
                                        Text(
                                            text = if (tile.isMine) "💣" else "💎",
                                            fontSize = if (selectedGridSize == MinesGridSize.GRID_3X3) 32.sp else 22.sp
                                        )
                                    } else {
                                        Text(
                                            text = "•",
                                            color = TextMuted,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Outcome Banner
        AnimatedVisibility(visible = gameOutcome != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                color = if (gameOutcome == "WIN") NeonGreen.copy(alpha = 0.25f) else CoralRed.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (gameOutcome == "WIN") NeonGreen else CoralRed)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (gameOutcome == "WIN") "🎉 CASHED OUT! ${String.format("%.2f", currentMultiplier)}x" else "💥 MINE EXPLODED!",
                        color = if (gameOutcome == "WIN") NeonGreen else CoralRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (gameOutcome == "WIN") "+ PKR ${String.format("%,.0f", selectedBet * currentMultiplier)}" else "- PKR ${selectedBet.toInt()}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isPlaying) {
                    // Grid Size Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Size:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MinesGridSize.values().forEach { size ->
                                val isSel = selectedGridSize == size
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedGridSize = size },
                                    color = if (isSel) PrimaryPurple else CardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                                ) {
                                    Text(
                                        text = size.label,
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mines Count Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mines Count:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 3, 5, 8, 10).filter { it < selectedGridSize.totalTiles - 1 }.forEach { m ->
                                val isSel = mineCount == m
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { mineCount = m },
                                    color = if (isSel) CoralRed else CardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CoralRed else CardBorder)
                                ) {
                                    Text(
                                        text = "$m 💣",
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bet Amount Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bet PKR:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(25.0, 50.0, 100.0, 250.0, 500.0).forEach { bet ->
                                val isSel = selectedBet == bet
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedBet = bet
                                            betErrorMessage = ""
                                        },
                                    color = if (isSel) AccentCyan else CardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) AccentCyan else CardBorder)
                                ) {
                                    Text(
                                        text = bet.toInt().toString(),
                                        color = if (isSel) DarkBg else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Start Game Button
                    Button(
                        onClick = { startNewGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pkrBalance >= selectedBet) NeonGreen else CardBg
                        ),
                        enabled = !isPlacingBet
                    ) {
                        if (isPlacingBet) {
                            CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START MINES • PKR ${selectedBet.toInt()}",
                                color = DarkBg,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else {
                    // In-play Cashout Button
                    Button(
                        onClick = { cashOut() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        enabled = safeRevealedCount > 0
                    ) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (safeRevealedCount == 0) "TAP ANY TILE TO REVEAL" else "CASH OUT • PKR ${String.format("%,.0f", selectedBet * currentMultiplier)}",
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
