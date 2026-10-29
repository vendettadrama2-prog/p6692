package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameHistoryEntity
import com.example.data.UserEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    user: UserEntity?,
    gameHistory: List<GameHistoryEntity>,
    customAvatarBitmap: Bitmap?,
    onOpenAiStudio: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onToggleRole: (String) -> Unit,
    onLogout: () -> Unit
) {
    var soundToggle by remember { mutableStateOf(user?.soundEnabled ?: true) }
    var notifToggle by remember { mutableStateOf(user?.notificationsEnabled ?: true) }
    var showResponsiblePlayDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with AI Studio overlay
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BrightPurple, SecondaryBg)))
                            .border(2.dp, AccentCyan, CircleShape)
                            .clickable { onOpenAiStudio() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (customAvatarBitmap != null) {
                            Image(
                                bitmap = customAvatarBitmap.asImageBitmap(),
                                contentDescription = "AI Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(text = "🤖", fontSize = 40.sp)
                        }

                        // AI Sparkle badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "AI Studio",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = user?.username ?: "CyberKnight_92",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user?.email ?: "player@p6692.com",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = PrimaryPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, PrimaryPurple)
                        ) {
                            Text(
                                text = "VIP TIER ${user?.vipLevel ?: 3}",
                                color = BrightPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentCyan)
                        ) {
                            Text(
                                text = user?.role ?: "PLAYER",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onOpenAiStudio,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightPurple.copy(alpha = 0.3f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrightPurple)
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = BrightPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize AI Avatar & Art", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Real PKR Balance Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₨", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Column {
                                Text("Real PKR Wallet Balance", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("PKR ${String.format("%,.2f", user?.pkrBalance ?: 0.0)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }

                        // Demo Practice Balance Strip
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎮", fontSize = 14.sp)
                            }
                            Column {
                                Text("DEMO BALANCE — NOT REAL MONEY", color = GoldYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("PKR ${String.format("%,.2f", user?.demoBalance ?: 5000.0)}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    Surface(
                        color = NeonGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                    ) {
                        Text(
                            text = "VERIFIED",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Admin Access Quick Switcher - strictly hidden for standard users, shown only when user.role == 'ADMIN'
        if (user?.role?.uppercase() == "ADMIN") {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SecondaryBg,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = AccentCyan)
                            }
                            Column {
                                Text("Admin Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Analytics, games catalog, user controls", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = onOpenAdminDashboard,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("OPEN", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Settings Toggles Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("App Preferences", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound & Audio Effects", color = TextSecondary, fontSize = 13.sp)
                        Switch(
                            checked = soundToggle,
                            onCheckedChange = { soundToggle = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryPurple
                            )
                        )
                    }

                    Divider(color = CardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Promotional Notifications", color = TextSecondary, fontSize = 13.sp)
                        Switch(
                            checked = notifToggle,
                            onCheckedChange = { notifToggle = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentCyan
                            )
                        )
                    }

                    Divider(color = CardBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResponsiblePlayDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Responsible Entertainment Guidelines", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan)
                    }
                }
            }
        }

        // Recent Gameplay History
        item {
            Text("Recent Game History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (gameHistory.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardBg,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No games played yet. Jump into Rocket 92 or Neon 777 to see your rounds!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(gameHistory.take(6)) { hist ->
                val isWin = hist.resultType == "WIN" || hist.resultType == "JACKPOT"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = hist.gameTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Bet PKR ${hist.betAmountPkr.toInt()} • Multiplier ${String.format("%.2f", hist.multiplier)}x",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = if (isWin) "+PKR ${hist.payoutPkr.toInt()}" else "0",
                            color = if (isWin) NeonGreen else CoralRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Logout / Switch User Button
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SWITCH ACCOUNT / LOGOUT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showResponsiblePlayDialog) {
        AlertDialog(
            onDismissRequest = { showResponsiblePlayDialog = false },
            title = { Text("Responsible Entertainment", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "P6692 is engineered exclusively for leisure and amusement. No real currency is accepted, stored, or distributed. All game rounds operate with virtual entertainment credits. Play responsibly and take breaks.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResponsiblePlayDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("UNDERSTOOD")
                }
            },
            containerColor = SecondaryBg
        )
    }
}
