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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class PianoTile(
    val id: Long = Random.nextLong(),
    val col: Int, // 0..3 (4 columns)
    var yProgress: Float, // 0.0f (top) to 1.15f (bottom miss)
    var isTapped: Boolean = false,
    val noteName: String
)

enum class PianoDifficulty(val label: String, val speed: Float, val spawnMs: Long) {
    EASY("Easy", 0.012f, 650L),
    NORMAL("Normal", 0.018f, 480L),
    HARD("Virtuoso", 0.025f, 340L)
}

@Composable
fun PianoTilesGame(
    pkrBalance: Double,
    soundEnabled: Boolean,
    onPlaceBet: suspend (betPkr: Double) -> Result<Double>,
    onCashoutWin: (winPkr: Double, multiplier: Double, betPkr: Double) -> Unit,
    onRoundLost: (betPkr: Double) -> Unit,
    onOpenDeposit: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Game state: "IDLE", "PLAYING", "PAUSED", "GAME_OVER", "PRACTICE"
    var gameState by remember { mutableStateOf("IDLE") }
    var difficulty by remember { mutableStateOf(PianoDifficulty.NORMAL) }
    var isPracticeMode by remember { mutableStateOf(false) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(142) }
    var lastNotePlayed by remember { mutableStateOf("Ready") }

    val tiles = remember { mutableStateListOf<PianoTile>() }
    val musicalNotes = listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5")

    fun onTileTap(tile: PianoTile) {
        if (tile.isTapped || gameState != "PLAYING" && gameState != "PRACTICE") return
        tile.isTapped = true
        score += 1
        combo += 1
        if (combo > maxCombo) maxCombo = combo
        lastNotePlayed = tile.noteName
    }

    fun onEmptyColumnTapped(col: Int) {
        if (gameState != "PLAYING") return
        mistakes += 1
        combo = 0
        if (!isPracticeMode && mistakes >= 3) {
            gameState = "GAME_OVER"
            if (score > highScore) highScore = score
        }
    }

    // Main animation & falling loop
    LaunchedEffect(gameState, difficulty, isPracticeMode) {
        if (gameState != "PLAYING" && gameState != "PRACTICE") return@LaunchedEffect

        var lastSpawn = System.currentTimeMillis()

        while (gameState == "PLAYING" || gameState == "PRACTICE") {
            val now = System.currentTimeMillis()
            val currentSpeed = difficulty.speed * (1f + min(1.2f, score / 80f))

            // Spawn new tiles
            if (now - lastSpawn > difficulty.spawnMs) {
                lastSpawn = now
                val col = Random.nextInt(4)
                tiles.add(
                    PianoTile(
                        col = col,
                        yProgress = 0f,
                        noteName = musicalNotes.random()
                    )
                )
            }

            // Move tiles down
            val currentTiles = tiles.toList()
            for (tile in currentTiles) {
                tile.yProgress += currentSpeed

                // Missed tile check (passed bottom without tapping)
                if (tile.yProgress > 1.05f && !tile.isTapped) {
                    mistakes += 1
                    combo = 0
                    tile.isTapped = true // Avoid double penalty
                    if (!isPracticeMode && mistakes >= 3) {
                        gameState = "GAME_OVER"
                        if (score > highScore) highScore = score
                        break
                    }
                }
            }

            // Remove out of bounds
            tiles.removeAll { it.yProgress > 1.15f }

            delay(20L)
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
                        modifier = Modifier.testTag("piano_tiles_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NEON PIANO TILES",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPracticeMode) "Practice Mode (No Fail)" else "Rhythm Concert • Tap Black Tiles",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }

                    if (gameState == "PLAYING" || gameState == "PRACTICE") {
                        IconButton(onClick = { gameState = "PAUSED" }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Pause", tint = GoldYellow)
                        }
                    } else {
                        IconButton(onClick = {
                            tiles.clear()
                            score = 0
                            combo = 0
                            mistakes = 0
                            gameState = if (isPracticeMode) "PRACTICE" else "PLAYING"
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
            // HUD Dashboard
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
                        Text("NOTES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Combo Streak
                Surface(
                    modifier = Modifier.weight(1f),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrightPurple.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("COMBO", color = BrightPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${combo}x", color = GoldYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Lives / Mistakes (Hearts)
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
                        Text("LIVES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val livesLeft = if (isPracticeMode) "∞" else "❤️".repeat(max(0, 3 - mistakes))
                        Text(
                            livesLeft.ifEmpty { "💀" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4-Column Piano Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0B18))
                    .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                // 4 Interactive Columns
                Row(modifier = Modifier.fillMaxSize()) {
                    for (col in 0..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(0.5.dp, Color(0xFF281945))
                                .clickable { onEmptyColumnTapped(col) }
                        )
                    }
                }

                // Target Hit Line at 85% height
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val hitY = size.height * 0.82f
                    drawLine(
                        color = AccentCyan.copy(alpha = 0.8f),
                        start = Offset(0f, hitY),
                        end = Offset(size.width, hitY),
                        strokeWidth = 3f
                    )
                }

                // Falling Piano Tiles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colW = size.width / 4f
                    val tileHeight = size.height * 0.18f

                    for (tile in tiles) {
                        val tileY = size.height * tile.yProgress - tileHeight / 2
                        val tileX = tile.col * colW

                        if (tile.isTapped) {
                            // Tapped tile flash
                            drawRoundRect(
                                color = NeonGreen.copy(alpha = 0.4f),
                                topLeft = Offset(tileX + 4f, tileY),
                                size = Size(colW - 8f, tileHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        } else {
                            // Active black piano key with neon purple glow
                            drawRoundRect(
                                color = Color(0xFF1E1035),
                                topLeft = Offset(tileX + 3f, tileY),
                                size = Size(colW - 6f, tileHeight),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = BrightPurple,
                                topLeft = Offset(tileX + 3f, tileY),
                                size = Size(colW - 6f, tileHeight),
                                cornerRadius = CornerRadius(8f, 8f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f)
                            )
                        }
                    }
                }

                // Direct Tappable touch points on tiles
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val boardWidth = maxWidth
                    val boardHeight = maxHeight
                    val colW = boardWidth / 4

                    for (tile in tiles) {
                        if (!tile.isTapped) {
                            val tileY = boardHeight * tile.yProgress - 35.dp
                            val tileX = colW * tile.col

                            Box(
                                modifier = Modifier
                                    .offset(x = tileX, y = tileY)
                                    .width(colW)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTileTap(tile) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎵",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Start / GameOver Overlay
                if (gameState == "IDLE") {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg.copy(alpha = 0.90f)),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎹", fontSize = 52.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "NEON PIANO TILES",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Tap falling black tiles on rhythm\nAvoid missing or tapping white space",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(Modifier.height(16.dp))

                            // Difficulty Selector
                            Text("DIFFICULTY", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PianoDifficulty.values().forEach { diff ->
                                    FilterChip(
                                        selected = difficulty == diff,
                                        onClick = { difficulty = diff },
                                        label = { Text(diff.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryPurple,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Practice Mode toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Practice Mode (Infinite Lives):", color = TextSecondary, fontSize = 12.sp)
                                Switch(
                                    checked = isPracticeMode,
                                    onCheckedChange = { isPracticeMode = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                )
                            }

                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    tiles.clear()
                                    score = 0
                                    combo = 0
                                    mistakes = 0
                                    gameState = if (isPracticeMode) "PRACTICE" else "PLAYING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("start_piano_tiles_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("START CONCERT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Text("💔", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("SONG CONCLUDED", color = CoralRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Total Notes: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Max Combo: ${maxCombo}x", color = GoldYellow, fontSize = 14.sp)
                            Text("High Score: $highScore", color = AccentCyan, fontSize = 13.sp)

                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    tiles.clear()
                                    score = 0
                                    combo = 0
                                    mistakes = 0
                                    gameState = "PLAYING"
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                                    .testTag("restart_piano_tiles_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("PLAY AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
