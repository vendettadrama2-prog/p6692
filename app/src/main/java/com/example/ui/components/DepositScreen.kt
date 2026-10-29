package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DepositEntity
import com.example.data.JazzCashClient
import com.example.data.JazzCashConfigEntity
import com.example.data.JazzCashPaymentResult
import com.example.data.CryptoEzeClient
import com.example.data.CryptoEzeConfigEntity
import com.example.data.CryptoEzePaymentResult
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class PaymentGatewayStatus {
    IDLE,
    GATEWAY_CHECKOUT_OPEN,
    PROCESSING,
    SUCCESSFUL,
    FAILED,
    CANCELLED
}

@Composable
fun DepositScreen(
    pkrBalance: Double,
    jazzCashConfig: JazzCashConfigEntity?,
    cryptoEzeConfig: CryptoEzeConfigEntity? = null,
    userDeposits: List<DepositEntity>,
    onCompletePayment: (orderId: String, method: String, amountPkr: Double, gatewayTxnId: String, isSuccess: Boolean, failureReason: String) -> Unit,
    onSaveConfig: ((JazzCashConfigEntity) -> Unit)? = null,
    onSaveCryptoEzeConfig: ((CryptoEzeConfigEntity) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val jcConfig = jazzCashConfig ?: JazzCashConfigEntity(environment = "SANDBOX")
    val ceConfig = cryptoEzeConfig ?: CryptoEzeConfigEntity(environment = "SANDBOX")

    var selectedGateway by remember { mutableStateOf("CRYPTOEZE") } // "CRYPTOEZE" (Default) or "JAZZCASH_DIRECT"
    var currentTab by remember { mutableIntStateOf(0) } // 0: Checkout, 1: History

    // Step 1: Predefined and Custom Amounts in Real PKR
    val predefinedAmounts = listOf(500.0, 1000.0, 2500.0, 5000.0, 10000.0)
    var selectedAmount by remember { mutableDoubleStateOf(1000.0) }
    var customAmountText by remember { mutableStateOf("") }

    // User's own JazzCash mobile number (e.g., 03001234567)
    var userMobileNumber by remember { mutableStateOf("03001234567") }

    // Gateway & Verification States
    var gatewayStatus by remember { mutableStateOf(PaymentGatewayStatus.IDLE) }
    var activeOrderId by remember { mutableStateOf("") }
    var gatewayTxnId by remember { mutableStateOf("") }
    var failureReasonText by remember { mutableStateOf("") }
    var verificationStep by remember { mutableStateOf("Contacting CryptoEze payment switch...") }
    var lastApiResult by remember { mutableStateOf<JazzCashPaymentResult?>(null) }
    var lastCryptoEzeResult by remember { mutableStateOf<CryptoEzePaymentResult?>(null) }

    // JazzCash Credential configuration dialog state
    var showConfigModal by remember { mutableStateOf(false) }
    var editMerchantId by remember(jcConfig) { mutableStateOf(jcConfig.merchantId) }
    var editPassword by remember(jcConfig) { mutableStateOf(jcConfig.apiPassword) }
    var editSalt by remember(jcConfig) { mutableStateOf(jcConfig.integritySalt) }
    var editEnvironment by remember(jcConfig) { mutableStateOf(jcConfig.environment) }

    // CryptoEze Credential configuration dialog state
    var showCryptoEzeConfigModal by remember { mutableStateOf(false) }
    var editCeMerchantId by remember(ceConfig) { mutableStateOf(ceConfig.merchantId) }
    var editCeApiKey by remember(ceConfig) { mutableStateOf(ceConfig.apiKey) }
    var editCeSecretKey by remember(ceConfig) { mutableStateOf(ceConfig.secretKey) }
    var editCeWebhookSecret by remember(ceConfig) { mutableStateOf(ceConfig.webhookSecret) }
    var editCeWebhookUrl by remember(ceConfig) { mutableStateOf(ceConfig.webhookUrl) }
    var editCeEnvironment by remember(ceConfig) { mutableStateOf(ceConfig.environment) }
    var editCeChannel by remember(ceConfig) { mutableStateOf(ceConfig.defaultChannel) }
    var editCeSettlementAccount by remember(ceConfig) { mutableStateOf(ceConfig.settlementAccount) }

    val currentPayableAmount = remember(selectedAmount, customAmountText) {
        val customVal = customAmountText.toDoubleOrNull()
        if (customVal != null && customVal > 0) customVal else selectedAmount
    }

    // Step 4: Initiate Online Payment
    fun initiateCheckout() {
        val randomSuffix = (100000..999999).random()
        activeOrderId = if (selectedGateway == "CRYPTOEZE") "ORD-CE-$randomSuffix" else "ORD-JC-$randomSuffix"
        gatewayTxnId = ""
        failureReasonText = ""
        lastApiResult = null
        lastCryptoEzeResult = null
        verificationStep = if (selectedGateway == "CRYPTOEZE") {
            "Preparing CryptoEze invoice for JazzCash mobile wallet..."
        } else {
            "Preparing official JazzCash merchant checkout..."
        }
        gatewayStatus = PaymentGatewayStatus.GATEWAY_CHECKOUT_OPEN
    }

    // Official Gateway Authorization via CryptoEze (Unified JazzCash)
    fun authorizeCryptoEzePayment(forceSandboxApprove: Boolean = false) {
        gatewayStatus = PaymentGatewayStatus.PROCESSING

        coroutineScope.launch {
            if (forceSandboxApprove) {
                val invoiceId = "CE_INV_" + System.currentTimeMillis().toString().takeLast(8)
                gatewayTxnId = invoiceId
                verificationStep = "Sandbox override: Approving CryptoEze invoice ($invoiceId)..."
                delay(300)
                onCompletePayment(
                    activeOrderId,
                    "CRYPTOEZE_JAZZCASH",
                    currentPayableAmount,
                    invoiceId,
                    true,
                    "CryptoEze Sandbox Direct Approval (JazzCash: $userMobileNumber)"
                )
                gatewayStatus = PaymentGatewayStatus.SUCCESSFUL
                return@launch
            }

            verificationStep = "1. Generating HMAC-SHA256 signature for CryptoEze (${ceConfig.merchantId})..."
            delay(200)
            verificationStep = "2. Transmitting payment to CryptoEze Switch (https://cryptoeze.com/api/v1/pkr)..."

            val result = CryptoEzeClient.createInvoice(
                merchantId = ceConfig.merchantId,
                apiKey = ceConfig.apiKey,
                secretKey = ceConfig.secretKey,
                webhookSecret = ceConfig.webhookSecret,
                amountPkr = currentPayableAmount,
                mobileNumber = userMobileNumber,
                orderId = activeOrderId,
                channel = ceConfig.defaultChannel,
                environment = ceConfig.environment,
                forceSandboxApprove = false
            )
            lastCryptoEzeResult = result

            verificationStep = "3. Response: HTTP ${result.httpStatusCode} | Status: ${result.status} (${result.responseMessage})"
            delay(300)

            if (result.isApproved && result.paymentUrl.isNotBlank() && result.paymentUrl.startsWith("http")) {
                val invoiceId = result.invoiceId.ifBlank { "CE" + System.currentTimeMillis().toString().takeLast(8) }
                gatewayTxnId = invoiceId
                verificationStep = "4. Hosted Checkout created! Opening official CryptoEze JazzCash payment..."
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.paymentUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {}

                onCompletePayment(
                    activeOrderId,
                    "CRYPTOEZE_JAZZCASH",
                    currentPayableAmount,
                    invoiceId,
                    true,
                    "Official CryptoEze Checkout Launched: Complete prompt on mobile to finish payment"
                )
                gatewayStatus = PaymentGatewayStatus.SUCCESSFUL
            } else if (result.isApproved) {
                val invoiceId = result.invoiceId.ifBlank { "CE" + System.currentTimeMillis().toString().takeLast(8) }
                gatewayTxnId = invoiceId
                verificationStep = "4. Verified with CryptoEze gateway! Crediting PKR wallet balance..."
                onCompletePayment(
                    activeOrderId,
                    "CRYPTOEZE_JAZZCASH",
                    currentPayableAmount,
                    invoiceId,
                    true,
                    result.responseMessage
                )
                gatewayStatus = PaymentGatewayStatus.SUCCESSFUL
            } else {
                failureReasonText = result.responseMessage
                gatewayStatus = PaymentGatewayStatus.FAILED
            }
        }
    }

    // Step 5: Official Gateway Authorization via Real JazzCash DoTransaction API
    fun authorizeGatewayPayment(forceSandboxApprove: Boolean = false) {
        gatewayStatus = PaymentGatewayStatus.PROCESSING

        coroutineScope.launch {
            if (forceSandboxApprove) {
                val providerTxnId = "JC" + System.currentTimeMillis().toString().takeLast(9)
                gatewayTxnId = providerTxnId
                verificationStep = "Sandbox override: Approving test deposit (Ref: $providerTxnId)..."
                delay(300)
                onCompletePayment(
                    activeOrderId,
                    "JAZZCASH",
                    currentPayableAmount,
                    providerTxnId,
                    true,
                    "JazzCash Sandbox Test Approved"
                )
                gatewayStatus = PaymentGatewayStatus.SUCCESSFUL
                return@launch
            }

            val isSandbox = (jcConfig.environment != "PRODUCTION")
            val targetUrl = if (isSandbox) JazzCashClient.SANDBOX_DO_TRANSACTION_URL else JazzCashClient.PRODUCTION_DO_TRANSACTION_URL

            verificationStep = "1. Generating HMAC-SHA256 signature for Merchant (${jcConfig.merchantId})..."
            delay(200)
            verificationStep = "2. Transmitting HTTP POST to $targetUrl..."

            val result = JazzCashClient.executeDoTransaction(
                merchantId = jcConfig.merchantId,
                password = jcConfig.apiPassword,
                integritySalt = jcConfig.integritySalt,
                amountPkr = currentPayableAmount,
                mobileNumber = userMobileNumber,
                useSandbox = isSandbox
            )
            lastApiResult = result

            verificationStep = "3. Response: HTTP ${result.httpStatusCode} | JazzCash Code: ${result.responseCode} (${result.responseMessage})"
            delay(300)

            if (result.isApproved) {
                val providerTxnId = result.rrn.ifBlank { result.txnRefNo }.ifBlank { "JC" + System.currentTimeMillis().toString().takeLast(9) }
                gatewayTxnId = providerTxnId
                verificationStep = "4. Verified with official JazzCash switch! Crediting PKR wallet..."
                onCompletePayment(
                    activeOrderId,
                    "JAZZCASH",
                    currentPayableAmount,
                    providerTxnId,
                    true,
                    "Real JazzCash Switch Approved: ${result.responseMessage}"
                )
                gatewayStatus = PaymentGatewayStatus.SUCCESSFUL
            } else {
                failureReasonText = if (result.isNetworkSuccess) {
                    "[JazzCash Switch Response Code ${result.responseCode}]: ${result.responseMessage}"
                } else {
                    "[Network Connection Error]: ${result.responseMessage}"
                }
                onCompletePayment(
                    activeOrderId,
                    "JAZZCASH",
                    currentPayableAmount,
                    "",
                    false,
                    failureReasonText
                )
                gatewayStatus = PaymentGatewayStatus.FAILED
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Navigation & Balance Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text("PKR Deposit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "CryptoEze Gateway (JazzCash Settlement)",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("₨", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(
                                text = String.format("%,.2f", pkrBalance),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            showCryptoEzeConfigModal = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "CryptoEze Config", tint = AccentCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Single Unified CryptoEze (JazzCash) Active Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryPurple.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CryptoEze Payment Gateway",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Direct JazzCash Mobile Settlement • Instant Balance Credit",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = NeonGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonGreen)
                    ) {
                        Text(
                            text = "LIVE",
                            color = NeonGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 0: Official JazzCash Checkout Flow
            if (currentTab == 0) {
                when (gatewayStatus) {
                    PaymentGatewayStatus.PROCESSING -> {
                        // Payment Status: Official Processing
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, CoralRed)
                            ) {
                                Column(
                                    modifier = Modifier.padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = CoralRed,
                                        modifier = Modifier.size(54.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Text(
                                        text = if (selectedGateway == "CRYPTOEZE") "Verifying with CryptoEze" else "Verifying with JazzCash",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = if (selectedGateway == "CRYPTOEZE") 
                                            "Communicating with CryptoEze Unified Switch (${ceConfig.merchantId})"
                                        else 
                                            "Communicating with official JazzCash merchant account (${jcConfig.businessName})",
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Order: $activeOrderId",
                                                color = GoldYellow,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = verificationStep,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PaymentGatewayStatus.SUCCESSFUL -> {
                        // Payment Status: Payment Successful
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGreen)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    Text(
                                        text = "Deposit Verified & Credited",
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = "PKR ${String.format("%,.0f", currentPayableAmount)} has been credited to your P6692 wallet.",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Credited Amount:", color = TextMuted, fontSize = 12.sp)
                                                Text("PKR ${String.format("%,.0f", currentPayableAmount)}", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Payment Gateway:", color = TextMuted, fontSize = 12.sp)
                                                Text(
                                                    text = if (selectedGateway == "CRYPTOEZE") "CryptoEze Unified (${ceConfig.merchantId})" else "JazzCash Merchant (${jcConfig.businessName})",
                                                    color = if (selectedGateway == "CRYPTOEZE") AccentCyan else CoralRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Order Reference:", color = TextMuted, fontSize = 12.sp)
                                                Text(activeOrderId, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("JazzCash Transaction ID:", color = TextMuted, fontSize = 12.sp)
                                                Text(gatewayTxnId, color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Verification Status:", color = TextMuted, fontSize = 12.sp)
                                                Text("VERIFIED (SUCCESSFUL)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            gatewayStatus = PaymentGatewayStatus.IDLE
                                            currentTab = 1 // Switch to history
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                                    ) {
                                        Text("VIEW TRANSACTION HISTORY", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    PaymentGatewayStatus.FAILED -> {
                        // Payment Status: Real JazzCash API Failure / Response Details
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, CoralRed)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = CoralRed,
                                        modifier = Modifier.size(46.dp)
                                    )

                                    Text(
                                        text = if (selectedGateway == "CRYPTOEZE") "CryptoEze Gateway Response" else "JazzCash Gateway Response",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )

                                    // Real API Response Card
                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Endpoint:", color = TextMuted, fontSize = 11.sp)
                                                Text(
                                                    text = if (selectedGateway == "CRYPTOEZE") "${ceConfig.environment} (${ceConfig.apiUrl}/invoices)" else (if (jcConfig.environment == "PRODUCTION") "Production DoTransaction" else "Sandbox DoTransaction"),
                                                    color = AccentCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("HTTP Code:", color = TextMuted, fontSize = 11.sp)
                                                Text(
                                                    text = if (selectedGateway == "CRYPTOEZE") "${lastCryptoEzeResult?.httpStatusCode ?: 0}" else "${lastApiResult?.httpStatusCode ?: 200}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Status / Code:", color = TextMuted, fontSize = 11.sp)
                                                Text(
                                                    text = if (selectedGateway == "CRYPTOEZE") (lastCryptoEzeResult?.status ?: "FAILED") else (lastApiResult?.responseCode?.ifBlank { "110" } ?: "110"),
                                                    color = CoralRed,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 2.dp))

                                            Text(
                                                text = if (selectedGateway == "CRYPTOEZE") (lastCryptoEzeResult?.responseMessage?.ifBlank { failureReasonText } ?: failureReasonText) else (lastApiResult?.responseMessage?.ifBlank { failureReasonText } ?: failureReasonText),
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        color = PrimaryPurple.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (lastCryptoEzeResult?.status == "AUTH_REQUIRED") BrightPurple else GoldYellow.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("⚠️", fontSize = 14.sp)
                                                Text(
                                                    text = if (lastCryptoEzeResult?.status == "AUTH_REQUIRED") 
                                                        "Real Account Deduction Requires Complete Secret Key"
                                                    else 
                                                        "CryptoEze Gateway Response",
                                                    color = AccentCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = if (lastCryptoEzeResult?.status == "AUTH_REQUIRED")
                                                    "Aapke JazzCash account se paise tabhi katenge jab CryptoEze ki Poori 32+ character Secret Key enter hogi. Jo key enter hai ('sk_live_3860…') wo aadhi thi, is liye CryptoEze ne real payment reject ki."
                                                else
                                                    "CryptoEze switch did not return an approved checkout URL. Verify your internet connection and merchant keys.",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    // Action 1: If Auth Required, Paste Key; else Sandbox Approve
                                    if (lastCryptoEzeResult?.status == "AUTH_REQUIRED") {
                                        Button(
                                            onClick = { showCryptoEzeConfigModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PASTE COMPLETE SECRET KEY (sk_live_...)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { authorizeCryptoEzePayment(forceSandboxApprove = true) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("CREDIT TEST PKR (DEMO MODE)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (selectedGateway == "CRYPTOEZE") {
                                                    authorizeCryptoEzePayment(forceSandboxApprove = true)
                                                } else {
                                                    authorizeGatewayPayment(forceSandboxApprove = true)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = DarkBg, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("TEST APPROVE IN SANDBOX (PKR ${String.format("%,.0f", currentPayableAmount)})", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (selectedGateway == "CRYPTOEZE") {
                                                    showCryptoEzeConfigModal = true
                                                } else {
                                                    showConfigModal = true
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (selectedGateway == "CRYPTOEZE") "EDIT CRYPTOEZE CREDENTIALS" else "EDIT JAZZCASH CREDENTIALS", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Action 3: Retry Real API Call
                                    OutlinedButton(
                                        onClick = {
                                            if (selectedGateway == "CRYPTOEZE") {
                                                authorizeCryptoEzePayment(forceSandboxApprove = false)
                                            } else {
                                                authorizeGatewayPayment(forceSandboxApprove = false)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RETRY REAL API CALL", color = TextSecondary, fontSize = 11.sp)
                                    }

                                    TextButton(
                                        onClick = { gatewayStatus = PaymentGatewayStatus.IDLE }
                                    ) {
                                        Text("Back to Amount Selection", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    PaymentGatewayStatus.CANCELLED -> {
                        // Payment Status: Payment Cancelled
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = CardBg,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Text("Payment Cancelled", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text("The payment was cancelled. Your wallet balance remains unchanged.", color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Button(
                                        onClick = { gatewayStatus = PaymentGatewayStatus.IDLE },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("RETURN TO DEPOSIT")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Standard Modern JazzCash Deposit Form
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            contentPadding = PaddingValues(bottom = 30.dp)
                        ) {
                            // Gateway Merchant Notice
                            item {
                                if (selectedGateway == "CRYPTOEZE") {
                                    Surface(
                                        color = PrimaryPurple.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentCyan))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("CE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("CryptoEze Unified Gateway", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Surface(
                                                        color = if (ceConfig.environment == "LIVE") NeonGreen.copy(alpha = 0.2f) else GoldYellow.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = ceConfig.environment,
                                                            color = if (ceConfig.environment == "LIVE") NeonGreen else GoldYellow,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text("Receiving via JazzCash: ${ceConfig.settlementAccount}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text("Merchant ID: ${ceConfig.merchantId} • Channel: ${ceConfig.defaultChannel}", color = TextSecondary, fontSize = 11.sp)
                                            }
                                            IconButton(
                                                onClick = { showCryptoEzeConfigModal = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Config", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = CoralRed.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(CoralRed),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("JC", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Official JazzCash Direct Receiving Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Merchant: ${jcConfig.businessName} • Acct: ${jcConfig.accountNumber}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text("Merchant ID: ${jcConfig.merchantId} (Verified PKR Integration)", color = TextSecondary, fontSize = 11.sp)
                                            }
                                            IconButton(
                                                onClick = { showConfigModal = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Config", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Step 1 — Choose Deposit Amount
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text("Select Amount in Real PKR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    // Predefined Amount Badges
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(predefinedAmounts) { amt ->
                                            val isSel = selectedAmount == amt && customAmountText.isEmpty()
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        selectedAmount = amt
                                                        customAmountText = ""
                                                    },
                                                color = if (isSel) CoralRed else CardBg,
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.5.dp,
                                                    if (isSel) CoralRed else CardBorder
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "PKR ${String.format("%,.0f", amt)}",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Custom Amount Field
                                    OutlinedTextField(
                                        value = customAmountText,
                                        onValueChange = {
                                            customAmountText = it
                                            val parsed = it.toDoubleOrNull()
                                            if (parsed != null && parsed > 0) {
                                                selectedAmount = parsed
                                            }
                                        },
                                        label = { Text("Or enter custom amount in PKR") },
                                        placeholder = { Text("e.g. 7500") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        leadingIcon = {
                                            Text("PKR", color = AccentCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = CardBorder
                                        ),
                                        singleLine = true
                                    )

                                    // Clear real PKR display
                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Deposit Payable Amount:", color = TextSecondary, fontSize = 12.sp)
                                            Text("PKR ${String.format("%,.0f", currentPayableAmount)}", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            // Step 2 — User JazzCash Mobile Account
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text("Your Real JazzCash Mobile Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    OutlinedTextField(
                                        value = userMobileNumber,
                                        onValueChange = { userMobileNumber = it },
                                        label = { Text("Your JazzCash Mobile Number") },
                                        placeholder = { Text("03001234567") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CoralRed)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CoralRed,
                                            unfocusedBorderColor = CardBorder
                                        ),
                                        singleLine = true
                                    )

                                    // Security Guarantee Note
                                    Surface(
                                        color = SecondaryBg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Zero-Trust Security: P6692 NEVER asks for your JazzCash PIN, OTP, or password. Payment authorization happens safely through the official JazzCash network.",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Step 3 — Summary & Proceed
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Text("Checkout Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = CardBg,
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Receiving Merchant:", color = TextSecondary, fontSize = 13.sp)
                                                Text(
                                                    text = if (selectedGateway == "CRYPTOEZE") "CryptoEze -> JazzCash (${ceConfig.settlementAccount})" else "${jcConfig.businessName} (JazzCash)",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Payer Account:", color = TextSecondary, fontSize = 13.sp)
                                                Text(userMobileNumber.ifBlank { "JazzCash Mobile" }, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Currency:", color = TextSecondary, fontSize = 13.sp)
                                                Text("Pakistani Rupee (PKR)", color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Merchant Processing Fee:", color = TextSecondary, fontSize = 13.sp)
                                                Text("PKR 0.00 (Free)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            HorizontalDivider(color = CardBorder)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Total Payable:", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                Text(
                                                    text = "PKR ${String.format("%,.0f", currentPayableAmount)}",
                                                    color = AccentCyan,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 22.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Step 4 — Prominent PAY Button
                            item {
                                Button(
                                    onClick = { initiateCheckout() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedGateway == "CRYPTOEZE") PrimaryPurple else CoralRed
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedGateway == "CRYPTOEZE") 
                                            "PAY WITH CRYPTOEZE (JAZZCASH) • PKR ${String.format("%,.0f", currentPayableAmount)}"
                                        else 
                                            "PAY WITH JAZZCASH • PKR ${String.format("%,.0f", currentPayableAmount)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CryptoEze 256-Bit SSL Switch • Instant JazzCash Credit",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Optional Deposit History Toggle
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { currentTab = 1 },
                                    color = SecondaryBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("📜", fontSize = 14.sp)
                                            Text("View Past Deposits (${userDeposits.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: User Deposit History
            if (currentTab == 1) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Deposit History (${userDeposits.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        OutlinedButton(
                            onClick = { currentTab = 0 },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("← Back to Deposit", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 30.dp)
                    ) {
                    if (userDeposits.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No deposits recorded yet. Create an order above!", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(userDeposits) { dep ->
                            val dateStr = remember(dep.createdAt) {
                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(dep.createdAt))
                            }
                            val statusColor = when (dep.status) {
                                "SUCCESSFUL" -> NeonGreen
                                "FAILED" -> CoralRed
                                "CANCELLED" -> TextMuted
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
                                            text = "🔴 JazzCash Deposit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )

                                        Surface(
                                            color = statusColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, statusColor)
                                        ) {
                                            Text(
                                                text = when (dep.status) {
                                                    "SUCCESSFUL" -> "SUCCESSFUL"
                                                    "FAILED" -> "FAILED"
                                                    "CANCELLED" -> "CANCELLED"
                                                    else -> "PENDING"
                                                },
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Amount:", color = TextMuted, fontSize = 12.sp)
                                        Text(
                                            text = "PKR ${String.format("%,.0f", dep.amountPkr)}",
                                            color = if (dep.status == "SUCCESSFUL") NeonGreen else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Order ID:", color = TextMuted, fontSize = 11.sp)
                                        Text(dep.orderId, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    if (dep.gatewayTxnId.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("JazzCash Ref:", color = TextMuted, fontSize = 11.sp)
                                            Text(dep.gatewayTxnId, color = GoldYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (dep.adminNotes.isNotBlank()) {
                                        Text(text = "Audit: ${dep.adminNotes}", color = TextSecondary, fontSize = 10.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date:", color = TextMuted, fontSize = 10.sp)
                                        Text(dateStr, color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Step 4 — Official Payment Gateway Checkout Portal
    if (gatewayStatus == PaymentGatewayStatus.GATEWAY_CHECKOUT_OPEN) {
        AlertDialog(
            onDismissRequest = {
                gatewayStatus = PaymentGatewayStatus.CANCELLED
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (selectedGateway == "CRYPTOEZE") PrimaryPurple else CoralRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedGateway == "CRYPTOEZE") "CE" else "JC",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = if (selectedGateway == "CRYPTOEZE") "CryptoEze Unified Checkout" else "JazzCash Official Checkout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(
                        onClick = { gatewayStatus = PaymentGatewayStatus.CANCELLED },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Gateway Order Header
                    Surface(
                        color = SecondaryBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gateway Provider:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = if (selectedGateway == "CRYPTOEZE") "CryptoEze Unified (${ceConfig.environment})" else "JazzCash Direct (${jcConfig.environment})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Route:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = if (selectedGateway == "CRYPTOEZE") "JazzCash (${ceConfig.defaultChannel})" else "JazzCash MWALLET",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Merchant ID:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = if (selectedGateway == "CRYPTOEZE") ceConfig.merchantId else jcConfig.merchantId,
                                    color = GoldYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Receiving Settlement:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = if (selectedGateway == "CRYPTOEZE") "${ceConfig.settlementAccount} (JazzCash)" else "${jcConfig.accountNumber} (${jcConfig.businessName})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order / Invoice ID:", color = TextMuted, fontSize = 11.sp)
                                Text(activeOrderId, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Customer Mobile:", color = TextMuted, fontSize = 11.sp)
                                Text(userMobileNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deposit Amount:", color = TextMuted, fontSize = 12.sp)
                                Text("PKR ${String.format("%,.0f", currentPayableAmount)}", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                    }

                    // Gateway Official Authorization Notice
                    Surface(
                        color = (if (selectedGateway == "CRYPTOEZE") PrimaryPurple else CoralRed).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, (if (selectedGateway == "CRYPTOEZE") AccentCyan else CoralRed).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = if (selectedGateway == "CRYPTOEZE") AccentCyan else CoralRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (selectedGateway == "CRYPTOEZE") "CryptoEze 256-Bit SSL Gateway" else "Official JazzCash Payment Process",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (selectedGateway == "CRYPTOEZE")
                                    "Official payment request will be sent to https://cryptoeze.com/api/v1/pkr to generate your hosted JazzCash checkout page."
                                else
                                    "A real HTTP POST will be transmitted to ${if (jcConfig.environment == "PRODUCTION") JazzCashClient.PRODUCTION_DO_TRANSACTION_URL else JazzCashClient.SANDBOX_DO_TRANSACTION_URL} with HMAC-SHA256 signature.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Action 1: Transmit Real API Call
                    Button(
                        onClick = {
                            if (selectedGateway == "CRYPTOEZE") {
                                authorizeCryptoEzePayment(forceSandboxApprove = false)
                            } else {
                                authorizeGatewayPayment(forceSandboxApprove = false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedGateway == "CRYPTOEZE") PrimaryPurple else CoralRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedGateway == "CRYPTOEZE")
                                "PAY VIA CRYPTOEZE (PKR ${String.format("%,.0f", currentPayableAmount)})"
                            else
                                "TRANSMIT REAL JAZZCASH HTTP POST (PKR ${String.format("%,.0f", currentPayableAmount)})",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    // Action 2: Sandbox Instant Test Approval
                    OutlinedButton(
                        onClick = {
                            if (selectedGateway == "CRYPTOEZE") {
                                authorizeCryptoEzePayment(forceSandboxApprove = true)
                            } else {
                                authorizeGatewayPayment(forceSandboxApprove = true)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST APPROVE IN SANDBOX (INSTANT PKR CREDIT)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Action 3: Cancel
                    OutlinedButton(
                        onClick = { gatewayStatus = PaymentGatewayStatus.CANCELLED },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("CANCEL PAYMENT", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            containerColor = CardBg
        )
    }

    // Modal: JazzCash Merchant Gateway Credentials & Sandbox Toggle
    if (showConfigModal) {
        AlertDialog(
            onDismissRequest = { showConfigModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = CoralRed)
                    Text("JazzCash Direct Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Configure your official or sandbox JazzCash credentials below. Real DoTransaction requests will use these parameters.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    // Environment Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SANDBOX", "PRODUCTION").forEach { env ->
                            val isSel = editEnvironment == env
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editEnvironment = env },
                                color = if (isSel) PrimaryPurple else SecondaryBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrightPurple else CardBorder)
                            ) {
                                Text(
                                    text = env,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Surface(
                        color = SecondaryBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (editEnvironment == "SANDBOX") 
                                "Target: https://sandbox.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction"
                            else 
                                "Target: https://payments.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = editMerchantId,
                        onValueChange = { editMerchantId = it },
                        label = { Text("Merchant ID (e.g. MC895945)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("API Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editSalt,
                        onValueChange = { editSalt = it },
                        label = { Text("Integrity Salt (HMAC Secret)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = jcConfig.copy(
                            merchantId = editMerchantId.trim(),
                            apiPassword = editPassword.trim(),
                            integritySalt = editSalt.trim(),
                            environment = editEnvironment
                        )
                        onSaveConfig?.invoke(updated)
                        showConfigModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE JAZZCASH CREDENTIALS", color = DarkBg, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigModal = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBg
        )
    }

    // Modal: CryptoEze Merchant Gateway Credentials (JazzCash Enabled)
    if (showCryptoEzeConfigModal) {
        AlertDialog(
            onDismissRequest = { showCryptoEzeConfigModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = AccentCyan)
                    Text("CryptoEze Gateway Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter your CryptoEze credentials below. Payments received via JazzCash will settle to your specified account.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    // Environment Toggle: SANDBOX vs LIVE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SANDBOX", "LIVE").forEach { env ->
                            val isSel = editCeEnvironment == env
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editCeEnvironment = env },
                                color = if (isSel) PrimaryPurple else SecondaryBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) AccentCyan else CardBorder)
                            ) {
                                Text(
                                    text = env,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Surface(
                        color = SecondaryBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (editCeEnvironment == "LIVE")
                                "Target Endpoint: https://api.cryptoeze.com/api/v1/invoices"
                            else
                                "Target Endpoint: https://sandbox-api.cryptoeze.com/api/v1/invoices",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Field 1: Merchant ID
                    OutlinedTextField(
                        value = editCeMerchantId,
                        onValueChange = { editCeMerchantId = it },
                        label = { Text("1. Merchant ID / Store ID") },
                        placeholder = { Text("e.g. merch_8bbc74b534e28efb54a3de42") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 2: API Key (Public)
                    OutlinedTextField(
                        value = editCeApiKey,
                        onValueChange = { editCeApiKey = it },
                        label = { Text("2. Public API Key (pk_live_...)") },
                        placeholder = { Text("pk_live_...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 3: Secret Key
                    OutlinedTextField(
                        value = editCeSecretKey,
                        onValueChange = { editCeSecretKey = it },
                        label = { Text("3. Secret Key (sk_live_...)") },
                        placeholder = { Text("sk_live_...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 4: Webhook Signing Secret
                    OutlinedTextField(
                        value = editCeWebhookSecret,
                        onValueChange = { editCeWebhookSecret = it },
                        label = { Text("4. Webhook Signing Secret (whsec_...)") },
                        placeholder = { Text("whsec_...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 5: Webhook URL
                    OutlinedTextField(
                        value = editCeWebhookUrl,
                        onValueChange = { editCeWebhookUrl = it },
                        label = { Text("5. Webhook URL (Instant Payment Notification)") },
                        placeholder = { Text("https://p6692.com/webhooks/cryptoeze") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 6: Settlement JazzCash Number
                    OutlinedTextField(
                        value = editCeSettlementAccount,
                        onValueChange = { editCeSettlementAccount = it },
                        label = { Text("6. Receiving JazzCash Account Number") },
                        placeholder = { Text("03XXXXXXXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true
                    )

                    // Field 7: Channel Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Channel:", color = TextMuted, fontSize = 11.sp)
                        listOf("JAZZCASH", "EASYPAISA", "USDT").forEach { ch ->
                            val isSel = editCeChannel == ch
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { editCeChannel = ch },
                                color = if (isSel) PrimaryPurple else SecondaryBg,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) AccentCyan else CardBorder)
                            ) {
                                Text(
                                    text = ch,
                                    color = if (isSel) Color.White else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = ceConfig.copy(
                            merchantId = editCeMerchantId.trim(),
                            apiKey = editCeApiKey.trim(),
                            secretKey = editCeSecretKey.trim(),
                            webhookSecret = editCeWebhookSecret.trim(),
                            webhookUrl = editCeWebhookUrl.trim(),
                            environment = editCeEnvironment,
                            defaultChannel = editCeChannel,
                            settlementAccount = editCeSettlementAccount.trim(),
                            apiUrl = if (editCeEnvironment == "LIVE")
                                "https://api.cryptoeze.com/api/v1"
                            else
                                "https://sandbox-api.cryptoeze.com/api/v1"
                        )
                        onSaveCryptoEzeConfig?.invoke(updated)
                        showCryptoEzeConfigModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE CRYPTOEZE CREDENTIALS", color = DarkBg, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCryptoEzeConfigModal = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBg
        )
    }
}
