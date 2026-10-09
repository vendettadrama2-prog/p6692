package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun P6692TopHeader(
    pkrBalance: Double,
    vipLevel: Int,
    unreadNotifications: Int,
    soundEnabled: Boolean,
    onBalanceClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSoundToggle: () -> Unit,
    onLogoClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SecondaryBg,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo
            Box(modifier = Modifier.clickable { onLogoClick() }) {
                P6692Logo(size = LogoSize.MEDIUM, showSubtext = false)
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real PKR Wallet Balance Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBalanceClick() },
                    color = CardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(NeonGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₨",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkBg
                            )
                        }
                        Text(
                            text = String.format("%,.0f", pkrBalance),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Deposit PKR",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Sound Toggle
                IconButton(
                    onClick = onSoundToggle,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Sound Toggle",
                        tint = if (soundEnabled) AccentCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Notification Bell with Badge
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadNotifications > 0) GoldYellow else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(CoralRed)
                        )
                    }
                }
            }
        }
    }
}
