package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GeminiAiAdvisorSheet(
    initialGameTitle: String = "Cyber Crash: Rocket 92",
    onDismiss: () -> Unit,
    onAvatarGenerated: (Bitmap) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Strategy, 1: Oracle, 2: AI Avatar
    val tabs = listOf("⚡ AI Strategy", "🔮 Cyber Oracle", "🎨 AI Avatar Studio")

    // Strategy state
    var gameTitleInput by remember { mutableStateOf(initialGameTitle) }
    var userQuestion by remember { mutableStateOf("") }
    var strategyResponse by remember { mutableStateOf("") }
    var isStrategyLoading by remember { mutableStateOf(false) }

    // Oracle state
    var oracleCategory by remember { mutableStateOf("Crash") }
    var oracleResponse by remember { mutableStateOf("") }
    var isOracleLoading by remember { mutableStateOf(false) }

    // Avatar state
    var avatarPrompt by remember { mutableStateOf("Cyber samurai warrior with neon purple visor") }
    var selectedImageSize by remember { mutableStateOf("1K") } // 1K, 2K, 4K
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAvatarLoading by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    // Load initial strategy
    LaunchedEffect(Unit) {
        if (strategyResponse.isBlank()) {
            isStrategyLoading = true
            val res = GeminiService.getGameStrategy(initialGameTitle)
            strategyResponse = res.getOrElse { "Could not load strategy: ${it.message}" }
            isStrategyLoading = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        color = SecondaryBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BrightPurple, AccentCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = DarkBg, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("P6692 Cyber AI Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Gemini 3.5 & 3 Pro Intelligence", color = AccentCyan, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSel = selectedTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = index },
                        color = if (isSel) PrimaryPurple else CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                    ) {
                        Text(
                            text = title,
                            color = if (isSel) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        // AI Strategy & Tactics
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Ask Gemini for tactical game probability advice, volatility analysis, or entertainment gameplay tips:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = gameTitleInput,
                                onValueChange = { gameTitleInput = it },
                                label = { Text("Game Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = AccentCyan,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = userQuestion,
                                onValueChange = { userQuestion = it },
                                placeholder = { Text("e.g. What is the safest bail strategy for 2.0x multiplier?") },
                                label = { Text("Question or Tactical Request") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrightPurple,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = BrightPurple,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isStrategyLoading = true
                                        val res = GeminiService.getGameStrategy(gameTitleInput, userQuestion)
                                        strategyResponse = res.getOrElse { "Error: ${it.message}" }
                                        isStrategyLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                enabled = !isStrategyLoading
                            ) {
                                if (isStrategyLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing Volatility...")
                                } else {
                                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GENERATE STRATEGY ANALYSIS")
                                }
                            }

                            if (strategyResponse.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CardBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(text = "⚡ Cyber AI Strategy Intel:", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = strategyResponse, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Cyber Lucky Oracle
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Powered by gemini-3.1-flash-lite for ultra-fast generation of lucky quantum numbers, hot multipliers, and category forecasts:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Crash", "Slots", "Mines", "Cards").forEach { cat ->
                                    val isSel = oracleCategory == cat
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { oracleCategory = cat },
                                        color = if (isSel) BrightPurple else CardBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSel) Color.White else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isOracleLoading = true
                                        val res = GeminiService.getLuckyForecast(oracleCategory)
                                        oracleResponse = res.getOrElse { "Forecast error: ${it.message}" }
                                        isOracleLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightPurple),
                                enabled = !isOracleLoading
                            ) {
                                if (isOracleLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning Quantum Matrix...")
                                } else {
                                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CONSULT CYBER ORACLE")
                                }
                            }

                            if (oracleResponse.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CardBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(text = oracleResponse, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // AI Avatar Studio (gemini-3-pro-image-preview with 1K, 2K, 4K)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Generate custom high-resolution gaming avatars using gemini-3-pro-image-preview with selectable resolution:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = avatarPrompt,
                                onValueChange = { avatarPrompt = it },
                                label = { Text("Avatar Description") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = AccentCyan,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )

                            // Affordance for 1K, 2K, 4K resolution selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Resolution:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("1K", "2K", "4K").forEach { size ->
                                        val isSel = selectedImageSize == size
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedImageSize = size },
                                            color = if (isSel) PrimaryPurple else CardBg,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                                        ) {
                                            Text(
                                                text = size,
                                                color = if (isSel) Color.White else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isAvatarLoading = true
                                        avatarError = null
                                        val res = GeminiService.generateAvatarImage(avatarPrompt, selectedImageSize)
                                        res.fold(
                                            onSuccess = { bmp ->
                                                generatedBitmap = bmp
                                                onAvatarGenerated(bmp)
                                            },
                                            onFailure = { err ->
                                                avatarError = err.message
                                            }
                                        )
                                        isAvatarLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                enabled = !isAvatarLoading
                            ) {
                                if (isAvatarLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBg, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating $selectedImageSize Avatar...", color = DarkBg, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = DarkBg)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CREATE $selectedImageSize AVATAR", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (avatarError != null) {
                                Surface(
                                    color = CoralRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                                ) {
                                    Text(
                                        text = avatarError!!,
                                        color = CoralRed,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (generatedBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .clip(CircleShape)
                                        .border(2.dp, AccentCyan, CircleShape)
                                ) {
                                    Image(
                                        bitmap = generatedBitmap!!.asImageBitmap(),
                                        contentDescription = "Generated Avatar",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
