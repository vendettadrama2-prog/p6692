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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "PROMO", "SYSTEM", "VIP")

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == "All") notifications
        else notifications.filter { it.category.equals(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
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
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            TextButton(onClick = onMarkAllAsRead) {
                Text("Mark all read", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Filter Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isSel = selectedFilter == filter
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedFilter = filter },
                    color = if (isSel) PrimaryPurple else CardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                ) {
                    Text(
                        text = filter,
                        color = if (isSel) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Notifications List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications in this tab.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredList) { notif ->
                    val dateStr = remember(notif.timestamp) {
                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(notif.timestamp))
                    }
                    val iconVector = when (notif.category) {
                        "VIP" -> Icons.Default.EmojiEvents
                        "PROMO" -> Icons.Default.LocalFireDepartment
                        else -> Icons.Default.Notifications
                    }
                    val iconColor = when (notif.category) {
                        "VIP" -> GoldYellow
                        "PROMO" -> BrightPurple
                        else -> AccentCyan
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkAsRead(notif.id) },
                        color = if (!notif.isRead) CardBgLight else CardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!notif.isRead) AccentCyan.copy(alpha = 0.5f) else CardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(iconColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = iconVector, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AccentCyan)
                                        )
                                    }
                                }
                                Text(text = notif.message, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                                Text(text = dateStr, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
