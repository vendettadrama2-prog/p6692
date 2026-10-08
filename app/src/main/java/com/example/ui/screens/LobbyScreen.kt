package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameEntity
import com.example.ui.components.GameCard
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    games: List<GameEntity>,
    favoriteIds: List<String>,
    onGameClick: (GameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayGame: (GameEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var onlyFavorites by remember { mutableStateOf(false) }

    val categories = listOf("All", "Crash", "Slots", "Mines", "Mini Games", "Cards", "Arcade")

    val filteredGames = remember(games, searchQuery, selectedCategory, onlyFavorites, favoriteIds) {
        games.filter { game ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Favorites" -> favoriteIds.contains(game.id)
                else -> game.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    game.title.contains(searchQuery, ignoreCase = true) ||
                    game.category.contains(searchQuery, ignoreCase = true) ||
                    game.description.contains(searchQuery, ignoreCase = true)
            val matchesFav = !onlyFavorites || favoriteIds.contains(game.id)
            matchesCategory && matchesSearch && matchesFav
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Title with Active Games Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🎮", fontSize = 20.sp)
                    Column {
                        Text("Game Lobby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${games.size} Real PKR Games Available", color = AccentCyan, fontSize = 11.sp)
                    }
                }

                Surface(
                    color = NeonGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                ) {
                    Text(
                        text = "LIVE PKR BETTING",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search games, categories, themes...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AccentCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                singleLine = true
            )

            // Category Chips Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Favorites Pill
                item {
                    val isFavActive = onlyFavorites || selectedCategory == "Favorites"
                    val favCount = favoriteIds.size
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (onlyFavorites) {
                                    onlyFavorites = false
                                } else {
                                    onlyFavorites = true
                                    selectedCategory = "All"
                                }
                            },
                        color = if (isFavActive) CoralRed else CardBg,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFavActive) CoralRed else CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (isFavActive) Color.White else CoralRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Favorites ($favCount)",
                                color = if (isFavActive) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category Items
                items(categories) { cat ->
                    val isSel = selectedCategory == cat && !onlyFavorites
                    val count = if (cat == "All") games.size else games.count { it.category.equals(cat, ignoreCase = true) }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                selectedCategory = cat
                                onlyFavorites = false
                            },
                        color = if (isSel) PrimaryPurple else CardBg,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                    ) {
                        Text(
                            text = "$cat ($count)",
                            color = if (isSel) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Games Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (filteredGames.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🕹️", fontSize = 48.sp)
                    Text("No games match your search", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Try clearing search filters or selecting 'All' category",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = "All"
                            onlyFavorites = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show All Games (${games.size})")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                ) {
                    items(filteredGames, key = { it.id }) { game ->
                        GameCard(
                            game = game,
                            isFavorite = favoriteIds.contains(game.id),
                            onCardClick = { onGameClick(game) },
                            onToggleFavorite = { onToggleFavorite(game.id) },
                            onPlayClick = { onPlayGame(game) }
                        )
                    }
                }
            }
        }
    }
}
