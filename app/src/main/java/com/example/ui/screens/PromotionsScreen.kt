package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyRewardEntity
import com.example.data.UserEntity
import com.example.ui.theme.*

data class MissionItem(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val maxProgress: Int,
    val rewardCredits: Double,
    val rewardPoints: Int,
    var isClaimed: Boolean = false
)

@Composable
fun PromotionsScreen(
    user: UserEntity?,
    dailyRewards: List<DailyRewardEntity>,
    onClaimDayReward: (DailyRewardEntity) -> Unit,
    onClaimMission: (Double, Int) -> Unit
) {
    var missions by remember {
        mutableStateOf(
            listOf(
                MissionItem("m1", "Warp Pilot", "Play 3 rounds of Cyber Crash: Rocket 92", 2, 3, 1500.0, 150),
                MissionItem("m2", "Combo Master", "Hit a 2.0x+ multiplier in any demo game", 1, 1, 2000.0, 200),
                MissionItem("m3", "Cyber Stylist", "Generate a new AI Avatar badge with Gemini", 0, 1, 3000.0, 300),
                MissionItem("m4", "Lucky Streak", "Spin the Mega 777 Neon Rush 5 times", 3, 5, 2500.0, 250)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // VIP Loyalty Level Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrightPurple, PrimaryPurple, SecondaryBg)
                        )
                    )
                    .border(1.5.dp, GoldYellow.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "👑", fontSize = 28.sp)
                            Column {
                                Text("P6692 VIP CLUB", color = GoldYellow, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                                Text("Tier 3: Cyber Elite", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            color = DarkBg.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "4,850 / 10,000 XP",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { 0.485f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldYellow,
                        trackColor = DarkBg.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "Next Tier Unlocks: +50% Daily Faucet Bonus & Priority Gemini AI Strategy Generation",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 7-Day Login Streak Ladder
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📅", fontSize = 16.sp)
                        Text(
                            text = "7-Day Daily Login Streak",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(text = "Claim Daily", color = AccentCyan, fontSize = 11.sp)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dailyRewards) { reward ->
                        val isAvailable = !reward.isClaimed && reward.isCurrentDay
                        Surface(
                            modifier = Modifier
                                .width(96.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = if (reward.isClaimed) CardBg.copy(alpha = 0.5f) else CardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAvailable) BrightPurple else CardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "DAY ${reward.day}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAvailable) AccentCyan else TextSecondary
                                )

                                Text(
                                    text = if (reward.isClaimed) "✅" else "🎁",
                                    fontSize = 24.sp
                                )

                                Text(
                                    text = "+${reward.rewardCredits.toInt()}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isAvailable) {
                                    Button(
                                        onClick = { onClaimDayReward(reward) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("CLAIM", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (reward.isClaimed) {
                                    Text("CLAIMED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("LOCKED", fontSize = 9.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Missions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🎯", fontSize = 16.sp)
                    Text(
                        text = "Daily Quests & Missions",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = "Resets in 11h", color = TextSecondary, fontSize = 11.sp)
            }
        }

        items(missions) { mission ->
            val canClaim = mission.progress >= mission.maxProgress && !mission.isClaimed
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = mission.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = mission.description,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { (mission.progress.toFloat() / mission.maxProgress).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentCyan,
                                trackColor = SecondaryBg
                            )
                            Text(
                                text = "${mission.progress}/${mission.maxProgress}",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (canClaim) {
                        Button(
                            onClick = {
                                onClaimMission(mission.rewardCredits, mission.rewardPoints)
                                missions = missions.map {
                                    if (it.id == mission.id) it.copy(isClaimed = true) else it
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+${mission.rewardCredits.toInt()}", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else if (mission.isClaimed) {
                        Text(
                            text = "CLAIMED",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Surface(
                            color = SecondaryBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${mission.rewardCredits.toInt()}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
