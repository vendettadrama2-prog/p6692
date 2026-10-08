package com.example.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class MineCell(
    val index: Int,
    val isMine: Boolean,
    var isRevealed: Boolean = false
)

@Composable
fun CyberMinesGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var mineCount by remember { mutableIntStateOf(3) }
    var selectedBet by remember { mutableDoubleStateOf(50.0) }
    var isPlaying by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(List(25) { MineCell(it, false) }) }
    var safeRevealedCount by remember { mutableIntStateOf(0) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var gameOutcome by remember { mutableStateOf<String?>(null) } // "WIN", "HIT_MINE", null
    var isPlacingBet by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }

    fun calculateMultiplier(revealed: Int, mines: Int): Double {
        if (revealed == 0) return 1.0
        var mult = 1.0
        for (i in 0 until revealed) {
            val safeLeft = 25 - mines - i
            val totalLeft = 25 - i
            mult *= (totalLeft.toDouble() / safeLeft.toDouble()) * 0.96
        }
        return (mult * 100).toInt() / 100.0
    }

    fun startNewGame() {
        if (pkrBalance < selectedBet) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.0f", pkrBalance)}). Please deposit."
            return
        }

        betErrorMessage = ""
        isPlacingBet = true

        coroutineScope.launch {
            val betResult = onPlaceBet(selectedBet)
            isPlacingBet = false
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Bet could not be placed."
                return@launch
            }

            val mineIndices = (0 until 25).shuffled().take(mineCount).toSet()
            grid = List(25) { idx ->
                MineCell(index = idx, isMine = idx in mineIndices, isRevealed = false)
            }
            safeRevealedCount = 0
            currentMultiplier = 1.0
            isPlaying = true
            gameOutcome = null
        }
    }

    fun revealCell(index: Int) {
        if (!isPlaying || grid[index].isRevealed) return

        val cell = grid[index]
        val newGrid = grid.toMutableList()
        newGrid[index] = cell.copy(isRevealed = true)
        grid = newGrid

        if (cell.isMine) {
            // Hit a mine! Reveal all
            grid = grid.map { it.copy(isRevealed = true) }
            isPlaying = false
            gameOutcome = "HIT_MINE"
            onRoundLost(selectedBet)
        } else {
            safeRevealedCount++
            currentMultiplier = calculateMultiplier(safeRevealedCount, mineCount)
            // If all safe cells uncovered:
            if (safeRevealedCount == 25 - mineCount) {
                val payout = selectedBet * currentMultiplier
                isPlaying = false
                gameOutcome = "WIN"
                onCashoutWin(payout, currentMultiplier, selectedBet)
            }
        }
    }

    fun cashOut() {
        if (!isPlaying || safeRevealedCount == 0) return
        val payout = selectedBet * currentMultiplier
        grid = grid.map { it.copy(isRevealed = true) }
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
                        text = "Grid Hunter: Cyber Mines",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Real PKR Strategic Grid",
                        color = AccentCyan,
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

        // Live Game Multiplier & Info Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardBg.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CURRENT WIN MULTIPLIER", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = if (safeRevealedCount == 0) "1.00x" else String.format("%.2fx", currentMultiplier),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (safeRevealedCount > 0) NeonGreen else Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("ESTIMATED PAYOUT", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = "PKR ${String.format("%,.0f", selectedBet * currentMultiplier)}",
                        fontSize = 20.sp,
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

        // 5x5 Grid Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 5) {
                            val idx = row * 5 + col
                            val cell = grid[idx]

                            val cellBg = when {
                                cell.isRevealed && cell.isMine -> CoralRed.copy(alpha = 0.8f)
                                cell.isRevealed && !cell.isMine -> NeonGreen.copy(alpha = 0.25f)
                                isPlaying -> PrimaryPurple.copy(alpha = 0.3f)
                                else -> CardBg
                            }

                            val cellBorder = when {
                                cell.isRevealed && cell.isMine -> CoralRed
                                cell.isRevealed && !cell.isMine -> NeonGreen
                                else -> CardBorder
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cellBg)
                                    .border(1.5.dp, cellBorder, RoundedCornerShape(12.dp))
                                    .clickable { revealCell(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.isRevealed) {
                                    Text(
                                        text = if (cell.isMine) "💥" else "💎",
                                        fontSize = 22.sp
                                    )
                                } else {
                                    Text(
                                        text = "•",
                                        color = TextMuted,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isPlaying) {
                    // Bet Selectors & Mines Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bet PKR:", color = TextSecondary, fontSize = 12.sp)
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
                                    color = if (isSel) PrimaryPurple else CardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                                ) {
                                    Text(
                                        text = bet.toInt().toString(),
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLACE BET • PKR ${selectedBet.toInt()}",
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
                            text = if (safeRevealedCount == 0) "TAP TILES TO FIND GEMS" else "CASH OUT • PKR ${String.format("%,.0f", selectedBet * currentMultiplier)}",
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
