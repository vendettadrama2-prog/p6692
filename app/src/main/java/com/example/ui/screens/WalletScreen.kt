package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.data.UserEntity
import com.example.data.WithdrawalEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletScreen(
    user: UserEntity?,
    transactions: List<TransactionEntity>,
    withdrawals: List<WithdrawalEntity>,
    onClaimFaucet: () -> Unit,
    onOpenDeposit: () -> Unit,
    onRequestWithdrawal: (amount: Double, method: String, title: String, number: String, callback: (Boolean, String) -> Unit) -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Ledger, 1: Withdrawals
    var showWithdrawDialog by remember { mutableStateOf(false) }

    // Withdrawal Form State
    var wthAmount by remember { mutableDoubleStateOf(1000.0) }
    var wthCustomText by remember { mutableStateOf("") }
    var wthMethod by remember { mutableStateOf("JAZZCASH") }
    var wthAccountTitle by remember { mutableStateOf("CyberPlayer") }
    var wthAccountNumber by remember { mutableStateOf("03001234567") }
    var wthErrorMessage by remember { mutableStateOf("") }
    var wthSuccessMessage by remember { mutableStateOf("") }

    val activeWithdrawAmount = remember(wthAmount, wthCustomText) {
        val parsed = wthCustomText.toDoubleOrNull()
        if (parsed != null && parsed > 0) parsed else wthAmount
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real PKR Cash Wallet Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1038),
                                Color(0xFF140B26),
                                DarkBg
                            )
                        )
                    )
                    .border(1.5.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Text(
                                    text = "REAL PKR WALLET",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Instant JazzCash & Easypaisa Settlements",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = NeonGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                        ) {
                            Text(
                                text = "ACTIVE ACCOUNT",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Balance Display
                    Column {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "PKR",
                                color = NeonGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format("%,.2f", user?.pkrBalance ?: 25000.0),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Authenticated Server Balance • Instant Settlement",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Action Buttons: Deposit & Withdraw
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenDeposit,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Icon(imageVector = Icons.Default.AddCard, contentDescription = null, tint = DarkBg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DEPOSIT PKR", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                wthErrorMessage = ""
                                wthSuccessMessage = ""
                                showWithdrawDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowOutward, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WITHDRAW PKR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Quick PKR Booster Strip
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldYellow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Column {
                            Text("Daily VIP Booster", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Claim free +PKR 1,000 bonus credits", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onClaimFaucet,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("CLAIM +1,000", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Ledger & Withdrawal Navigation Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "📊 Wallet Ledger (${transactions.size})",
                    "📤 Withdrawals (${withdrawals.size})"
                ).forEachIndexed { index, title ->
                    val isSel = currentTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { currentTab = index },
                        color = if (isSel) PrimaryPurple else CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                    ) {
                        Text(
                            text = title,
                            color = if (isSel) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // TAB 0: Complete Server-Side Wallet Ledger
        if (currentTab == 0) {
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No ledger transactions recorded yet.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(transactions) { txn ->
                    val dateStr = remember(txn.timestamp) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(txn.timestamp))
                    }
                    val isCredit = txn.type in listOf("DEPOSIT", "WIN", "REFUND", "ADJUSTMENT")
                    val typeColor = when (txn.type) {
                        "DEPOSIT" -> NeonGreen
                        "WIN" -> GoldYellow
                        "BET" -> BrightPurple
                        "LOSS" -> CoralRed
                        "WITHDRAWAL" -> AccentCyan
                        "REFUND" -> NeonGreen
                        else -> TextSecondary
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = typeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, typeColor)
                                    ) {
                                        Text(
                                            text = txn.type,
                                            color = typeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = txn.description,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = (if (isCredit) "+" else "-") + "PKR ${String.format("%,.0f", txn.amountPkr)}",
                                    color = if (isCredit) NeonGreen else CoralRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (txn.newBalance > 0) "Bal: PKR ${String.format("%,.0f", txn.newBalance)}" else txn.transactionId,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(text = dateStr, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: User Withdrawals History
        if (currentTab == 1) {
            if (withdrawals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No withdrawal requests yet. Tap 'WITHDRAW PKR' above.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(withdrawals) { wth ->
                    val dateStr = remember(wth.requestedAt) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(wth.requestedAt))
                    }
                    val statusColor = when (wth.status) {
                        "COMPLETED" -> NeonGreen
                        "APPROVED" -> AccentCyan
                        "REJECTED" -> CoralRed
                        else -> GoldYellow
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardBg,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${if (wth.method == "JAZZCASH") "🔴 JazzCash" else "🟢 Easypaisa"} • ${wth.accountNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )

                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, statusColor)
                                ) {
                                    Text(
                                        text = wth.status,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PKR ${String.format("%,.0f", wth.amountPkr)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(text = dateStr, color = TextMuted, fontSize = 11.sp)
                            }

                            Text(
                                text = "Ref: ${wth.withdrawalId} (${wth.accountTitle})" + if (wth.adminNotes.isNotBlank()) " • Note: ${wth.adminNotes}" else "",
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // PKR Withdrawal Request Modal Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Request PKR Withdrawal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { showWithdrawDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Available Balance: PKR ${String.format("%,.2f", user?.pkrBalance ?: 0.0)}",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    // Method Selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { wthMethod = "JAZZCASH" },
                            color = if (wthMethod == "JAZZCASH") CoralRed.copy(alpha = 0.2f) else SecondaryBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (wthMethod == "JAZZCASH") CoralRed else CardBorder)
                        ) {
                            Text(
                                text = "JazzCash",
                                color = if (wthMethod == "JAZZCASH") CoralRed else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { wthMethod = "EASYPAISA" },
                            color = if (wthMethod == "EASYPAISA") NeonGreen.copy(alpha = 0.2f) else SecondaryBg,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (wthMethod == "EASYPAISA") NeonGreen else CardBorder)
                        ) {
                            Text(
                                text = "Easypaisa",
                                color = if (wthMethod == "EASYPAISA") NeonGreen else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    OutlinedTextField(
                        value = wthAccountTitle,
                        onValueChange = { wthAccountTitle = it },
                        label = { Text("Account Title / Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wthAccountNumber,
                        onValueChange = { wthAccountNumber = it },
                        label = { Text("Mobile Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = if (wthCustomText.isNotBlank()) wthCustomText else wthAmount.toInt().toString(),
                        onValueChange = {
                            wthCustomText = it
                            val parsed = it.toDoubleOrNull()
                            if (parsed != null && parsed > 0) wthAmount = parsed
                        },
                        label = { Text("Withdrawal Amount in PKR") },
                        leadingIcon = { Text("PKR", color = NeonGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true
                    )

                    if (wthErrorMessage.isNotBlank()) {
                        Text(text = wthErrorMessage, color = CoralRed, fontSize = 11.sp)
                    }

                    if (wthSuccessMessage.isNotBlank()) {
                        Text(text = wthSuccessMessage, color = NeonGreen, fontSize = 11.sp)
                    }

                    Text(
                        text = "Min withdrawal: PKR 500. Funds are verified and dispatched through $wthMethod business API.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userBal = user?.pkrBalance ?: 0.0
                        if (activeWithdrawAmount < 500) {
                            wthErrorMessage = "Minimum withdrawal amount is PKR 500."
                            return@Button
                        }
                        if (activeWithdrawAmount > userBal) {
                            wthErrorMessage = "Insufficient PKR balance for withdrawal."
                            return@Button
                        }
                        if (wthAccountNumber.length < 10) {
                            wthErrorMessage = "Please enter valid mobile account number."
                            return@Button
                        }

                        wthErrorMessage = ""
                        onRequestWithdrawal(activeWithdrawAmount, wthMethod, wthAccountTitle, wthAccountNumber) { success, msg ->
                            if (success) {
                                wthSuccessMessage = msg
                                showWithdrawDialog = false
                                currentTab = 1
                            } else {
                                wthErrorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SUBMIT WITHDRAWAL • PKR ${String.format("%,.0f", activeWithdrawAmount)}", color = DarkBg, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = CardBg
        )
    }
}
