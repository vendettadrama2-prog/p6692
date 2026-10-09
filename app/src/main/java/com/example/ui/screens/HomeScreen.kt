package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameEntity
import com.example.data.UserEntity
import com.example.ui.components.GameCard
import com.example.ui.components.LogoSize
import com.example.ui.components.P6692Logo
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    user: UserEntity?,
    games: List<GameEntity>,
    favoriteIds: List<String>,
    onGameClick: (GameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayGame: (GameEntity) -> Unit,
    onClaimDaily: () -> Unit,
    onOpenAiAdvisor: (String) -> Unit,
    onViewAllGames: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenDeposit: () -> Unit = {}
) {
    val featuredGames = remember(games) { games.filter { it.isFeatured }.ifEmpty { games.take(4) } }
    val popularGames = remember(games) { games.filter { it.isPopular }.ifEmpty { games.takeLast(4) } }
    val newGames = remember(games) { games.filter { it.isNew }.ifEmpty { games } }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Crash", "Slots", "Mines", "Mini Games", "Cards", "Arcade")

    val displayedGames = remember(games, selectedCategory) {
        if (selectedCategory == "All") games
        else games.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Real PKR Account & Instant Deposit Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₨", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Column {
                            Text("PKR Balance", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "PKR ${String.format("%,.2f", user?.pkrBalance ?: 0.0)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenDeposit,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+ Deposit PKR", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        // Hero Promotional Banner Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                BrightPurple.copy(alpha = 0.85f),
                                PrimaryPurple,
                                SecondaryBg
                            )
                        )
                    )
                    .border(1.5.dp, BrightPurple.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            ) {
                // Background visual art if available
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "P6692 Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )

                // Foreground Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DarkBg.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
                        ) {
                            Text(
                                text = "🔥 SEASON 1 CYBER TOURNAMENT",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Ends in 14h 22m",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column {
                        Text(
                            text = "5,000x Multiplier Hunt",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rocket 92 ascent is active. Climb the leaderboards for VIP bonuses.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                games.firstOrNull { it.id == "cyber_crash" }?.let { onPlayGame(it) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LAUNCH NOW", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onOpenAiAdvisor("Cyber Crash: Rocket 92") },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI TIPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick VIP & Faucet Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrightPurple.copy(alpha = 0.2f))
                                .border(1.dp, BrightPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👑", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "VIP Tier ${user?.vipLevel ?: 3} Pass Active",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${user?.rewardPoints ?: 4850} Reward PTS Earned",
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = onClaimDaily,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "CLAIM +2.5K", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Gemini AI Strategist Teaser Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onOpenAiAdvisor("Platform") },
                color = SecondaryBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.radialGradient(listOf(BrightPurple, AccentCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = DarkBg, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("P6692 Cyber AI Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Tactical analysis, oracle predictions & AI avatars", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan)
                }
            }
        }

        // Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PaddingValues(horizontal = 16.dp)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = selectedCategory == cat
                        val count = if (cat == "All") games.size else games.count { it.category.equals(cat, ignoreCase = true) }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat },
                            color = if (isSel) PrimaryPurple else CardBg,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) BrightPurple else CardBorder
                            )
                        ) {
                            Text(
                                text = "$cat ($count)",
                                color = if (isSel) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Games Display Section (Category-filtered or All Games)
        if (selectedCategory != "All") {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🎮", fontSize = 16.sp)
                        Text(
                            text = "$selectedCategory Games (${displayedGames.size})",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Show All",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { selectedCategory = "All" }
                    )
                }
            }

            items(displayedGames.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameCard(
                        game = pair[0],
                        isFavorite = favoriteIds.contains(pair[0].id),
                        onCardClick = { onGameClick(pair[0]) },
                        onToggleFavorite = { onToggleFavorite(pair[0].id) },
                        onPlayClick = { onPlayGame(pair[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        GameCard(
                            game = pair[1],
                            isFavorite = favoriteIds.contains(pair[1].id),
                            onCardClick = { onGameClick(pair[1]) },
                            onToggleFavorite = { onToggleFavorite(pair[1].id) },
                            onPlayClick = { onPlayGame(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Featured Games Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🔥", fontSize = 16.sp)
                            Text(
                                text = "Featured Games",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "View All",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onViewAllGames() }
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featuredGames) { game ->
                            GameCard(
                                game = game,
                                isFavorite = favoriteIds.contains(game.id),
                                onCardClick = { onGameClick(game) },
                                onToggleFavorite = { onToggleFavorite(game.id) },
                                onPlayClick = { onPlayGame(game) },
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }

            // Popular Arcade Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⚡", fontSize = 16.sp)
                            Text(
                                text = "Popular Right Now",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "View All",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onViewAllGames() }
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularGames) { game ->
                            GameCard(
                                game = game,
                                isFavorite = favoriteIds.contains(game.id),
                                onCardClick = { onGameClick(game) },
                                onToggleFavorite = { onToggleFavorite(game.id) },
                                onPlayClick = { onPlayGame(game) },
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }

            // All Playable Games Catalogue Grid
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🕹️", fontSize = 16.sp)
                        Text(
                            text = "All Games (${displayedGames.size})",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Full Lobby",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onViewAllGames() }
                    )
                }
            }

            items(displayedGames.chunked(2)) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameCard(
                        game = pair[0],
                        isFavorite = favoriteIds.contains(pair[0].id),
                        onCardClick = { onGameClick(pair[0]) },
                        onToggleFavorite = { onToggleFavorite(pair[0].id) },
                        onPlayClick = { onPlayGame(pair[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        GameCard(
                            game = pair[1],
                            isFavorite = favoriteIds.contains(pair[1].id),
                            onCardClick = { onGameClick(pair[1]) },
                            onToggleFavorite = { onToggleFavorite(pair[1].id) },
                            onPlayClick = { onPlayGame(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Responsible Play Compliance Notice
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = CardBg.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "P6692 Secure Real PKR Gaming System",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Official 256-bit SSL encrypted JazzCash and Easypaisa payment checkout. Bets and winnings are settled atomically via the server-side wallet ledger with instant balance synchronization.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
