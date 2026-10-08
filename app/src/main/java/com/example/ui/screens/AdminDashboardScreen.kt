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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    users: List<UserEntity>,
    games: List<GameEntity>,
    deposits: List<DepositEntity>,
    withdrawals: List<WithdrawalEntity>,
    transactions: List<TransactionEntity>,
    jazzCashConfig: JazzCashConfigEntity?,
    auditLogs: List<AuditLogEntity>,
    onToggleGameEnabled: (String, Boolean) -> Unit,
    onToggleGameFeatured: (String, Boolean) -> Unit,
    onGrantCredits: (String, Double) -> Unit,
    onToggleBanUser: (String, Boolean) -> Unit,
    onVerifyDeposit: (depositId: Long, approve: Boolean, note: String) -> Unit,
    onProcessWithdrawal: (withdrawalId: Long, approve: Boolean, complete: Boolean, note: String) -> Unit,
    onSaveJazzCashConfig: (JazzCashConfigEntity) -> Unit,
    onBack: () -> Unit
) {
    var adminTab by remember { mutableIntStateOf(0) }
    val pendingDepCount = remember(deposits) { deposits.count { it.status == "PENDING" } }
    val pendingWthCount = remember(withdrawals) { withdrawals.count { it.status == "PENDING" } }

    val tabs = listOf(
        "📊 Overview",
        "💳 Deposits ($pendingDepCount)",
        "📤 Withdrawals ($pendingWthCount)",
        "📜 Ledger (${transactions.size})",
        "⚙️ JazzCash",
        "🎮 Games",
        "👥 Users",
        "📋 Logs"
    )

    var selectedUserForGrant by remember { mutableStateOf<UserEntity?>(null) }
    var grantAmountInput by remember { mutableStateOf("10000") }

    // Payment Gateway Configuration Edit States
    val initialConfig = jazzCashConfig ?: JazzCashConfigEntity()
    var jcBusinessName by remember(jazzCashConfig) { mutableStateOf(initialConfig.businessName) }
    var jcAccountNumber by remember(jazzCashConfig) { mutableStateOf(initialConfig.accountNumber) }
    var jcMerchantId by remember(jazzCashConfig) { mutableStateOf(initialConfig.merchantId) }
    var jcApiPassword by remember(jazzCashConfig) { mutableStateOf(initialConfig.apiPassword) }
    var jcIntegritySalt by remember(jazzCashConfig) { mutableStateOf(initialConfig.integritySalt) }
    var paymentEnv by remember(jazzCashConfig) { mutableStateOf(initialConfig.environment) }
    var configSaveSuccess by remember { mutableStateOf(false) }

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
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text("P6692 Admin Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Financial Telemetry & User Controls", color = AccentCyan, fontSize = 11.sp)
                }
            }

            Surface(
                color = AccentCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
            ) {
                Text(
                    text = "ROOT ADMIN",
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Tab Row (Horizontal Scrollable)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { index ->
                val title = tabs[index]
                val isSel = adminTab == index
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { adminTab = index },
                    color = if (isSel) PrimaryPurple else CardBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                ) {
                    Text(
                        text = title,
                        color = if (isSel) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (adminTab) {
                0 -> {
                    // TAB 0: Overview Metrics
                    val totalPkrDeposits = remember(deposits) {
                        deposits.filter { it.status == "SUCCESSFUL" }.sumOf { it.amountPkr }
                    }
                    val totalPkrWithdrawals = remember(withdrawals) {
                        withdrawals.filter { it.status == "COMPLETED" }.sumOf { it.amountPkr }
                    }
                    val totalUserBalance = remember(users) {
                        users.sumOf { it.pkrBalance }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item {
                            Text("Real-Time Platform & Financial Telemetry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Total Deposits" to "PKR ${String.format("%,.0f", totalPkrDeposits)}",
                                    "Dispatched WTH" to "PKR ${String.format("%,.0f", totalPkrWithdrawals)}",
                                    "User Balances" to "PKR ${String.format("%,.0f", totalUserBalance)}"
                                ).forEach { (label, value) ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        color = CardBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = label, color = TextMuted, fontSize = 10.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = value, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Payment Gateway Configuration Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("• JazzCash Business: $jcBusinessName (Account: $jcAccountNumber)", color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("• Merchant ID: $jcMerchantId • Secure Salt: 498z5u0th4", color = TextSecondary, fontSize = 11.sp)
                                    Text("• Pending Deposits: $pendingDepCount • Pending Withdrawals: $pendingWthCount", color = GoldYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("• Backend Verification: Active idempotency and atomic ledger enabled", color = NeonGreen, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Deposits
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("JazzCash PKR Deposits (${deposits.size} Total)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Official Merchant Audit", color = AccentCyan, fontSize = 11.sp)
                            }
                        }

                        if (deposits.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No deposit records found.", color = TextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(deposits) { dep ->
                                val dateStr = remember(dep.createdAt) {
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(dep.createdAt))
                                }
                                val statusColor = when (dep.status) {
                                    "SUCCESSFUL" -> NeonGreen
                                    "FAILED" -> CoralRed
                                    else -> GoldYellow
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CardBg,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🔴 JazzCash • ${dep.orderId}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                color = statusColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, statusColor)
                                            ) {
                                                Text(
                                                    text = dep.status,
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
                                            Text(text = "Amount: PKR ${String.format("%,.0f", dep.amountPkr)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(text = dateStr, color = TextMuted, fontSize = 11.sp)
                                        }

                                        Text(text = "Order ID: ${dep.orderId}", color = AccentCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(text = if (dep.gatewayTxnId.isNotBlank()) "JazzCash Ref: ${dep.gatewayTxnId}" else "Checkout: Official JazzCash Merchant Switch", color = GoldYellow, fontSize = 11.sp)

                                        if (dep.adminNotes.isNotBlank()) {
                                            Text(text = "Audit Note: ${dep.adminNotes}", color = TextMuted, fontSize = 10.sp)
                                        }

                                        if (dep.status == "PENDING") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { onVerifyDeposit(dep.id, true, "Verified against JazzCash Merchant Statement") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Approve & Credit", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                OutlinedButton(
                                                    onClick = { onVerifyDeposit(dep.id, false, "Declined by Admin") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Withdrawals Management
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Withdrawal Requests (${withdrawals.size} Total)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Review & Dispatch", color = AccentCyan, fontSize = 11.sp)
                            }
                        }

                        if (withdrawals.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No withdrawal requests yet.", color = TextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(withdrawals) { wth ->
                                val dateStr = remember(wth.requestedAt) {
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(wth.requestedAt))
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
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${if (wth.method == "JAZZCASH") "🔴 JazzCash" else "🟢 Easypaisa"} • ${wth.accountNumber}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                color = statusColor.copy(alpha = 0.2f),
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
                                            Text(text = "Amount: PKR ${String.format("%,.0f", wth.amountPkr)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(text = dateStr, color = TextMuted, fontSize = 11.sp)
                                        }

                                        Text(text = "Account Title: ${wth.accountTitle} • Ref: ${wth.withdrawalId}", color = AccentCyan, fontSize = 11.sp)

                                        if (wth.status == "PENDING") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { onProcessWithdrawal(wth.id, true, true, "Dispatched via JazzCash Business") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Dispatched & Paid", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                OutlinedButton(
                                                    onClick = { onProcessWithdrawal(wth.id, false, false, "Declined by Admin") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Reject & Refund", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: Complete Wallet Ledger
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("Complete Server-Side Ledger (${transactions.size} records)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        items(transactions) { txn ->
                            val dateStr = remember(txn.timestamp) {
                                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(txn.timestamp))
                            }
                            val isCredit = txn.type in listOf("DEPOSIT", "WIN", "REFUND", "ADJUSTMENT")
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "${txn.type} • ${txn.description}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = "$dateStr • Bal: PKR ${String.format("%,.0f", txn.newBalance)}", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = (if (isCredit) "+" else "-") + "PKR ${String.format("%,.0f", txn.amountPkr)}",
                                        color = if (isCredit) NeonGreen else CoralRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: Official JazzCash Merchant Configuration
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("Official JazzCash Merchant Credentials", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("P6692 receives deposits strictly through this verified JazzCash Merchant account:", color = TextSecondary, fontSize = 12.sp)
                        }

                        if (configSaveSuccess) {
                            item {
                                Surface(
                                    color = NeonGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                                ) {
                                    Text("✅ JazzCash merchant credentials updated & synchronized with backend!", color = NeonGreen, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                        }

                        // Environment Selection: Sandbox vs Production
                        item {
                            Surface(
                                color = SecondaryBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Gateway Environment Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf("PRODUCTION", "SANDBOX").forEach { env ->
                                            val isSel = paymentEnv == env
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { paymentEnv = env; configSaveSuccess = false },
                                                color = if (isSel) (if (env == "PRODUCTION") NeonGreen else GoldYellow).copy(alpha = 0.2f) else CardBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) (if (env == "PRODUCTION") NeonGreen else GoldYellow) else CardBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = if (env == "PRODUCTION") "🟢 Live / Production" else "🟡 Sandbox / Test",
                                                        color = if (isSel) (if (env == "PRODUCTION") NeonGreen else GoldYellow) else TextSecondary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (paymentEnv == "PRODUCTION") "Active: Routing transactions to official live JazzCash banking switch." else "Active: Running on official JazzCash test sandbox switch for validation.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // JazzCash Section Header
                        item {
                            Surface(
                                color = CoralRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(24.dp).clip(CircleShape).background(CoralRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("JC", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                    Text("Official JazzCash Merchant Account Credentials", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = jcBusinessName,
                                onValueChange = { jcBusinessName = it; configSaveSuccess = false },
                                label = { Text("JazzCash Business / Merchant Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = jcAccountNumber,
                                onValueChange = { jcAccountNumber = it; configSaveSuccess = false },
                                label = { Text("JazzCash Merchant Account Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = jcMerchantId,
                                onValueChange = { jcMerchantId = it; configSaveSuccess = false },
                                label = { Text("Merchant ID (e.g. MC895945)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = jcApiPassword,
                                onValueChange = { jcApiPassword = it; configSaveSuccess = false },
                                label = { Text("API Password") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = jcIntegritySalt,
                                onValueChange = { jcIntegritySalt = it; configSaveSuccess = false },
                                label = { Text("Integrity Salt (HMAC-SHA256)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    onSaveJazzCashConfig(
                                        JazzCashConfigEntity(
                                            businessName = jcBusinessName,
                                            accountNumber = jcAccountNumber,
                                            merchantId = jcMerchantId,
                                            apiPassword = jcApiPassword,
                                            integritySalt = jcIntegritySalt,
                                            environment = paymentEnv,
                                            epStoreId = "",
                                            epHashKey = "",
                                            epAccountTitle = ""
                                        )
                                    )
                                    configSaveSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("SAVE JAZZCASH CONFIGURATION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: Games
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(games) { g ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(g.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${g.category} • Plays: ${g.playsCount}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = g.isEnabled, onCheckedChange = { onToggleGameEnabled(g.id, it) })
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // TAB 6: Users
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(users) { u ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(u.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("PKR ${String.format("%,.2f", u.pkrBalance)}", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onGrantCredits(u.id, 10000.0) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("+10k PKR", fontSize = 10.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onToggleBanUser(u.id, !u.isBanned) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(if (u.isBanned) "Unban" else "Ban", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                7 -> {
                    // TAB 7: Logs
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(auditLogs) { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "[${log.action}] ${log.details}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
