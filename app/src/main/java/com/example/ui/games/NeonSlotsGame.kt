package com.example.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SlotSymbol(val icon: String, val name: String, val multiplier: Double)

@Composable
fun NeonSlotsGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val symbols = listOf(
        SlotSymbol("7️⃣", "Lucky Seven", 100.0),
        SlotSymbol("👑", "Neo Crown", 50.0),
        SlotSymbol("💎", "Cyber Diamond", 25.0),
        SlotSymbol("🚀", "Warp Rocket", 15.0),
        SlotSymbol("⚡", "Volt Bolt", 10.0),
        SlotSymbol("🍒", "Neon Cherry", 5.0)
    )

    var reel1 by remember { mutableStateOf(symbols[0]) }
    var reel2 by remember { mutableStateOf(symbols[1]) }
    var reel3 by remember { mutableStateOf(symbols[2]) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedBet by remember { mutableDoubleStateOf(50.0) }
    var winMessage by remember { mutableStateOf("SELECT BET & TAP SPIN") }
    var winAmount by remember { mutableDoubleStateOf(0.0) }
    var showPaytable by remember { mutableStateOf(false) }
    var betErrorMessage by remember { mutableStateOf("") }

    fun spinReels() {
        if (pkrBalance < selectedBet || isSpinning) {
            if (pkrBalance < selectedBet) {
                betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.0f", pkrBalance)}). Please deposit."
            }
            return
        }

        betErrorMessage = ""
        isSpinning = true
        winMessage = "SPINNING REELS..."
        winAmount = 0.0

        coroutineScope.launch {
            // Deduct bet atomically on backend
            val betResult = onPlaceBet(selectedBet)
            if (betResult.isFailure) {
                isSpinning = false
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Bet declined"
                return@launch
            }

            // Rapid shuffling animation
            for (step in 1..18) {
                reel1 = symbols.random()
                reel2 = symbols.random()
                reel3 = symbols.random()
                delay((30 + step * 8).toLong())
            }

            // Final stop outcome
            val s1 = symbols.random()
            val s2 = symbols.random()
            val s3 = symbols.random()
            reel1 = s1
            reel2 = s2
            reel3 = s3

            isSpinning = false

            // Evaluation
            if (s1.icon == s2.icon && s2.icon == s3.icon) {
                // Triple Match Jackpot!
                val mult = s1.multiplier
                val payout = selectedBet * mult
                winAmount = payout
                winMessage = "🔥 MEGA JACKPOT! 3x ${s1.name} (+PKR ${String.format("%,.0f", payout)})"
                onCashoutWin(payout, mult, selectedBet)
            } else if (s1.icon == s2.icon || s2.icon == s3.icon || s1.icon == s3.icon) {
                // Double Match
                val mult = 2.0
                val payout = selectedBet * mult
                winAmount = payout
                winMessage = "✨ DOUBLE MATCH WIN! (+PKR ${String.format("%,.0f", payout)})"
                onCashoutWin(payout, mult, selectedBet)
            } else {
                // No match
                winMessage = "TRY AGAIN FOR 777 COMBO"
                onRoundLost(selectedBet)
            }
        }
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
                        text = "Mega 777 Neon Rush",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Real PKR Slot Machine",
                        color = BrightPurple,
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

        // Slot Arena Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E1038),
                            Color(0xFF0F0720)
                        )
                    )
                )
                .border(2.dp, BrightPurple.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                // Status / Win Ticker
                Surface(
                    color = DarkBg.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (winAmount > 0) NeonGreen else CardBorder)
                ) {
                    Text(
                        text = winMessage,
                        color = if (winAmount > 0) NeonGreen else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Three Reel Windows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(reel1, reel2, reel3).forEach { sym ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(125.dp),
                            color = CardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (isSpinning) AccentCyan else BrightPurple
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = sym.icon,
                                    fontSize = 44.sp
                                )
                            }
                        }
                    }
                }

                // Paytable Preview Toggle
                TextButton(onClick = { showPaytable = !showPaytable }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (showPaytable) "Hide Paytable" else "View Symbol Multipliers", color = AccentCyan, fontSize = 12.sp)
                }

                if (showPaytable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        symbols.take(4).forEach { s ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s.icon, fontSize = 18.sp)
                                Text("${s.multiplier.toInt()}x", color = GoldYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Betting Controls
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
                if (betErrorMessage.isNotBlank()) {
                    Text(text = betErrorMessage, color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Bet Amount Quick Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bet in PKR:", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20.0, 50.0, 100.0, 250.0, 500.0).forEach { bet ->
                            val isSel = selectedBet == bet
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isSpinning) {
                                            selectedBet = bet
                                            betErrorMessage = ""
                                        }
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

                // Spin Button
                Button(
                    onClick = { spinReels() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pkrBalance >= selectedBet) NeonGreen else CardBg
                    ),
                    enabled = !isSpinning
                ) {
                    if (isSpinning) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPIN REELS • PKR ${selectedBet.toInt()}",
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
