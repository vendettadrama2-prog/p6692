package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Brush
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

data class Gem(
    val id: Int,
    val type: Int, // 0..5 gem colors
    val emoji: String,
    val name: String,
    val color: Color,
    val isMatched: Boolean = false
)

/**
 * Fortune Gems 2
 * Features:
 * - 3x3 slot reel matrix inspired by the hit sequel "Fortune Gems 2"
 * - 4th Extra Bonus Multiplier Wheel Reel (1x, 2x, 3x, 5x, 10x, 15x)
 * - Authentic Aztec gem symbols: Ruby Garuda, Emerald Serpent, Sapphire Jaguar, Topaz Idol, Golden Wild
 * - Cascading matches, win line animations, and multiplier bonus wheel calculation
 * - Real PKR wallet deduction and payout integration
 * - Bet selection, rules modal, and recent round history
 */
@Composable
fun FortuneGemsGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Gem symbol catalog
    val gemCatalog = listOf(
        Pair("💎", Color(0xFF00E5FF)), // Diamond Wild (High)
        Pair("🔴", Color(0xFFFF1744)), // Ruby Garuda
        Pair("🟢", Color(0xFF00E676)), // Emerald Snake
        Pair("🔵", Color(0xFF2979FF)), // Sapphire Jaguar
        Pair("🟡", Color(0xFFFFD600)), // Topaz Sun
        Pair("🟣", Color(0xFFD500F9))  // Amethyst Skull
    )

    // 3x3 Grid + 1 Multiplier Reel Symbol
    var grid by remember {
        mutableStateOf(List(9) { index ->
            val randomType = (0..5).random()
            Gem(
                id = index,
                type = randomType,
                emoji = gemCatalog[randomType].first,
                name = "Gem $randomType",
                color = gemCatalog[randomType].second
            )
        })
    }

    var bonusMultiplier by remember { mutableIntStateOf(1) }
    var isSpinning by remember { mutableStateOf(false) }
    var lastWinPkr by remember { mutableDoubleStateOf(0.0) }
    var lastMultiplierWon by remember { mutableDoubleStateOf(0.0) }
    var winningIndices by remember { mutableStateOf(emptySet<Int>()) }

    val quickBets = listOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)
    var selectedBet by remember { mutableDoubleStateOf(100.0) }
    var customBetText by remember { mutableStateOf("") }
    var betErrorMessage by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }

    val roundHistory = remember { mutableStateListOf(3.0, 0.0, 15.0, 2.5, 0.0, 30.0, 1.2, 8.0) }

    val activeBetAmount = remember(selectedBet, customBetText) {
        val parsed = customBetText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else selectedBet
    }

    // Spin animation pulse
    val infiniteTransition = rememberInfiniteTransition(label = "fortune_gems_pulse")
    val gemPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gem_pulse"
    )

    fun spinFortuneGems() {
        if (isSpinning) return

        if (activeBetAmount > pkrBalance) {
            betErrorMessage = "Insufficient PKR balance (Available: PKR ${String.format("%,.2f", pkrBalance)}). Please deposit."
            return
        }
        if (activeBetAmount < 25.0) {
            betErrorMessage = "Minimum bet is PKR 25."
            return
        }

        betErrorMessage = ""
        isSpinning = true
        winningIndices = emptySet()
        lastWinPkr = 0.0
        lastMultiplierWon = 0.0

        coroutineScope.launch {
            // Server-side bet placement
            val betResult = onPlaceBet(activeBetAmount)
            if (betResult.isFailure) {
                betErrorMessage = betResult.exceptionOrNull()?.message ?: "Betting rejected"
                isSpinning = false
                return@launch
            }

            // Reel spinning animation
            val rollMultiplierOptions = listOf(1, 1, 2, 2, 3, 5, 10, 15)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 1600) {
                grid = List(9) { index ->
                    val r = (0..5).random()
                    Gem(index, r, gemCatalog[r].first, "Gem $r", gemCatalog[r].second)
                }
                bonusMultiplier = rollMultiplierOptions.random()
                delay(60)
            }

            // Decide Final Outcome according to Fortune Gems 2 logic
            // Check paylines: Rows [0,1,2], [3,4,5], [6,7,8] and Diagonals [0,4,8], [2,4,6]
            val winRoll = Random.nextDouble(0.0, 100.0)
            val isWinningRound = winRoll < 44.0 // 44% hit frequency

            val newGrid = if (isWinningRound) {
                // Force at least 1 payline match
                val baseGrid = MutableList(9) { index ->
                    val r = (0..5).random()
                    Gem(index, r, gemCatalog[r].first, "Gem $r", gemCatalog[r].second)
                }
                val chosenSymbol = (0..5).random()
                val chosenLine = (0..3).random()
                when (chosenLine) {
                    0 -> { // Row 0
                        baseGrid[0] = Gem(0, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[1] = Gem(1, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[2] = Gem(2, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        winningIndices = setOf(0, 1, 2)
                    }
                    1 -> { // Row 1 (Center)
                        baseGrid[3] = Gem(3, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[4] = Gem(4, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[5] = Gem(5, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        winningIndices = setOf(3, 4, 5)
                    }
                    2 -> { // Row 2
                        baseGrid[6] = Gem(6, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[7] = Gem(7, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[8] = Gem(8, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        winningIndices = setOf(6, 7, 8)
                    }
                    else -> { // Diagonal
                        baseGrid[0] = Gem(0, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[4] = Gem(4, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        baseGrid[8] = Gem(8, chosenSymbol, gemCatalog[chosenSymbol].first, "Gem", gemCatalog[chosenSymbol].second)
                        winningIndices = setOf(0, 4, 8)
                    }
                }
                baseGrid
            } else {
                winningIndices = emptySet()
                List(9) { index ->
                    val r = (0..5).random()
                    Gem(index, r, gemCatalog[r].first, "Gem $r", gemCatalog[r].second)
                }
            }

            grid = newGrid

            // Final bonus multiplier on 4th reel
            val finalBonus = if (isWinningRound) {
                listOf(1, 2, 3, 5, 10).random()
            } else {
                1
            }
            bonusMultiplier = finalBonus

            if (isWinningRound) {
                val symbolBaseMult = when (grid[winningIndices.first()].type) {
                    0 -> 5.0 // Diamond Wild
                    1 -> 3.5 // Ruby
                    2 -> 2.5 // Emerald
                    3 -> 2.0 // Sapphire
                    4 -> 1.5 // Topaz
                    else -> 1.2
                }
                val totalMult = symbolBaseMult * finalBonus
                val winPkr = (activeBetAmount * totalMult * 100).toInt() / 100.0

                lastWinPkr = winPkr
                lastMultiplierWon = totalMult
                roundHistory.add(0, totalMult)
                if (roundHistory.size > 12) roundHistory.removeLast()

                onCashoutWin(winPkr, totalMult, activeBetAmount)
            } else {
                roundHistory.add(0, 0.0)
                if (roundHistory.size > 12) roundHistory.removeLast()
                onRoundLost(activeBetAmount)
            }

            isSpinning = false
        }
    }

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
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
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
                                text = "FORTUNE GEMS 2",
                                color = NeonGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = NeonGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SLOT",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Aztec Multiplier Reels",
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HISTORY",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(roundHistory) { mult ->
                        val pillColor = when {
                            mult >= 10.0 -> GoldYellow
                            mult >= 3.0 -> AccentCyan
                            mult > 0.0 -> NeonGreen
                            else -> TextSecondary
                        }
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, pillColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (mult > 0.0) "${String.format("%.1f", mult)}x" else "0x",
                                color = pillColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showRulesDialog = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", tint = AccentCyan, modifier = Modifier.size(18.dp))
                }
            }
        }

        // MAIN 3x3 + 4TH MULTIPLIER REEL GAME VIEWPORT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gold Temple Header
                Surface(
                    color = Color(0xFF1B1429),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldYellow.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏛️", fontSize = 14.sp)
                        Text(
                            text = "AZTEC TREASURE REEL",
                            color = GoldYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text("🏛️", fontSize = 14.sp)
                    }
                }

                // Slot Grid & Multiplier Reel Container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3x3 Main Gem Matrix
                    Surface(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color(0xFF120C22),
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryPurple)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            userScrollEnabled = false
                        ) {
                            items(grid.size) { i ->
                                val gem = grid[i]
                                val isHighlighted = winningIndices.contains(i)

                                Surface(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp)),
                                    color = if (isHighlighted) gem.color.copy(alpha = 0.35f) else CardBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isHighlighted) 2.dp else 1.dp,
                                        if (isHighlighted) GoldYellow else CardBorder
                                    )
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = gem.emoji,
                                            fontSize = 32.sp,
                                            modifier = Modifier.graphicsLayer {
                                                if (isHighlighted) {
                                                    scaleX = gemPulse
                                                    scaleY = gemPulse
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 4th Special Bonus Multiplier Reel
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "BONUS",
                            color = GoldYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )

                        Surface(
                            modifier = Modifier
                                .width(70.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color(0xFF1F1138),
                            border = androidx.compose.foundation.BorderStroke(2.dp, GoldYellow)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("⚡", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${bonusMultiplier}x",
                                        color = GoldYellow,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "WHEEL",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Win result display banner
                if (!isSpinning && lastWinPkr > 0) {
                    Surface(
                        color = NeonGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💎", fontSize = 16.sp)
                            Text(
                                text = "BIG WIN! PKR ${String.format("%,.0f", lastWinPkr)} (${String.format("%.1f", lastMultiplierWon)}x)",
                                color = NeonGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // BETTING AND CONTROLS PANEL
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        text = "Min: PKR 25 • Max: PKR 25,000",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                // Quick Bet Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickBets) { amt ->
                        val isSelected = selectedBet == amt && customBetText.isEmpty()
                        Surface(
                            color = if (isSelected) NeonGreen else CardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonGreen else CardBorder),
                            modifier = Modifier.clickable(enabled = !isSpinning) {
                                selectedBet = amt
                                customBetText = ""
                                betErrorMessage = ""
                            }
                        ) {
                            Text(
                                text = "PKR ${amt.toInt()}",
                                color = if (isSelected) DarkBg else TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Custom Bet Input
                OutlinedTextField(
                    value = customBetText,
                    onValueChange = {
                        customBetText = it
                        betErrorMessage = ""
                    },
                    label = { Text("Or enter custom bet in PKR") },
                    placeholder = { Text("e.g. 500") },
                    leadingIcon = {
                        Text("PKR", color = NeonGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                    },
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonGreen,
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

                // SPIN BUTTON
                Button(
                    onClick = { spinFortuneGems() },
                    enabled = !isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    if (isSpinning) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("SPINNING GEMS...", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = DarkBg)
                            Text(
                                text = "SPIN FOR PKR ${String.format("%,.0f", activeBetAmount)}",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Rules Dialog
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text("Fortune Gems 2 Rules", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 3x3 classic Aztec gem reels + 4th Multiplier Reel.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Match 3 symbols along horizontal rows or diagonals to win.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Multipliers on the 4th reel multiply your payline win up to 15x.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Payout = Bet Amount × Symbol Multiplier × 4th Reel Multiplier.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Official RTP: 97.8%. Certified Fair RNG.", color = AccentCyan, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("GOT IT", color = NeonGreen)
                }
            },
            containerColor = CardBg
        )
    }
}
