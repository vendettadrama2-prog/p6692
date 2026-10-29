package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class CryptoEzePaymentResult(
    val isNetworkSuccess: Boolean,
    val httpStatusCode: Int,
    val invoiceId: String,
    val paymentUrl: String,
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val responseMessage: String,
    val rawResponse: String,
    val endpointUsed: String,
    val channelUsed: String,
    val isApproved: Boolean
)

object CryptoEzeClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    const val SANDBOX_API_URL = "https://sandbox-api.cryptoeze.com/api/v1"
    const val LIVE_API_URL = "https://api.cryptoeze.com/api/v1"

    fun calculateHmacSignature(data: String, secretKey: String): String {
        if (secretKey.isBlank()) return ""
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(key)
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Creates an invoice with CryptoEze gateway targeting JazzCash or other supported channels.
     */
    suspend fun createInvoice(
        merchantId: String,
        apiKey: String,
        webhookSecret: String,
        amountPkr: Double,
        mobileNumber: String,
        customerEmail: String = "player@p6692.com",
        orderId: String,
        channel: String = "JAZZCASH",
        environment: String = "SANDBOX",
        forceSandboxApprove: Boolean = false
    ): CryptoEzePaymentResult = withContext(Dispatchers.IO) {
        val baseUrl = if (environment.equals("LIVE", ignoreCase = true)) LIVE_API_URL else SANDBOX_API_URL
        val endpoint = "$baseUrl/invoices"

        // Format clean Pakistani mobile number
        var cleanMobile = mobileNumber.trim().replace(Regex("[^0-9]"), "")
        if (cleanMobile.startsWith("92")) {
            cleanMobile = "0" + cleanMobile.substring(2)
        } else if (cleanMobile.length == 10 && cleanMobile.startsWith("3")) {
            cleanMobile = "0" + cleanMobile
        }
        if (cleanMobile.isBlank()) cleanMobile = "03001234567"

        val effectiveMerchantId = merchantId.trim().ifBlank { "merch_8bbc74b534e28efb54a3de42" }
        val effectiveApiKey = apiKey.trim().ifBlank { "pk_live_1bbea1f4b9cab098a5189af471d580e9" }
        val effectiveWebhookSec = webhookSecret.trim().ifBlank { "whsec_4a6112" }

        val payloadObj = JSONObject().apply {
            put("merchant_id", effectiveMerchantId)
            put("order_id", orderId)
            put("amount", amountPkr)
            put("currency", "PKR")
            put("payment_method", channel.uppercase()) // JAZZCASH
            put("customer_mobile", cleanMobile)
            put("customer_email", customerEmail)
            put("description", "P6692 Gaming Wallet Deposit via CryptoEze (JazzCash)")
            put("callback_url", "https://api.p6692.com/api/cryptoeze/callback")
            put("webhook_url", "https://p6692.com/webhooks/cryptoeze")
            put("timestamp", System.currentTimeMillis())
        }

        val payloadString = payloadObj.toString()
        val signature = calculateHmacSignature(payloadString, effectiveWebhookSec)

        // If forceSandboxApprove was selected by tester
        if (forceSandboxApprove) {
            val mockInvoiceId = "CE_INV_" + System.currentTimeMillis().toString().takeLast(8)
            val mockJson = JSONObject().apply {
                put("success", true)
                put("status", "SUCCESS")
                put("message", "Sandbox Direct Approval: PKR ${"%.2f".format(amountPkr)} credited to wallet from JazzCash $cleanMobile")
                put("invoice_id", mockInvoiceId)
                put("payment_method", channel)
                put("amount", amountPkr)
                put("gateway", "CryptoEze Unified Gateway")
            }
            return@withContext CryptoEzePaymentResult(
                isNetworkSuccess = true,
                httpStatusCode = 200,
                invoiceId = mockInvoiceId,
                paymentUrl = "https://pay.cryptoeze.com/invoice/$mockInvoiceId",
                status = "SUCCESS",
                responseMessage = "CryptoEze Sandbox: Payment of PKR ${"%.2f".format(amountPkr)} approved via JazzCash ($cleanMobile)",
                rawResponse = mockJson.toString(2),
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = true
            )
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payloadString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $effectiveApiKey")
                .header("x-api-key", effectiveApiKey)
                .header("x-merchant-id", effectiveMerchantId)
                .header("x-signature", signature)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val code = response.code

            var status = if (code in 200..299) "PENDING" else "FAILED"
            var msg = "CryptoEze HTTP $code"
            var invoiceId = "CE_" + System.currentTimeMillis().toString().takeLast(6)
            var payUrl = ""
            var approved = false

            if (responseBody.isNotBlank()) {
                try {
                    val resJson = JSONObject(responseBody)
                    if (resJson.has("message")) {
                        msg = resJson.getString("message")
                    } else if (resJson.has("msg")) {
                        msg = resJson.getString("msg")
                    }
                    if (resJson.has("invoice_id")) {
                        invoiceId = resJson.getString("invoice_id")
                    } else if (resJson.has("id")) {
                        invoiceId = resJson.getString("id")
                    }
                    if (resJson.has("payment_url")) {
                        payUrl = resJson.getString("payment_url")
                    } else if (resJson.has("checkout_url")) {
                        payUrl = resJson.getString("checkout_url")
                    }
                    if (resJson.has("status")) {
                        status = resJson.getString("status").uppercase()
                    }
                    if (status == "APPROVED" || status == "SUCCESS" || status == "PAID" || code == 200) {
                        approved = true
                    }
                } catch (e: Exception) {
                    msg = "Response received: ${responseBody.take(120)}"
                }
            }

            CryptoEzePaymentResult(
                isNetworkSuccess = response.isSuccessful,
                httpStatusCode = code,
                invoiceId = invoiceId,
                paymentUrl = payUrl.ifBlank { "https://pay.cryptoeze.com/checkout/$invoiceId" },
                status = status,
                responseMessage = msg,
                rawResponse = responseBody,
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = approved
            )
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Network connection error"
            val fallbackInvoiceId = "CE_LOC_" + System.currentTimeMillis().toString().takeLast(6)
            CryptoEzePaymentResult(
                isNetworkSuccess = false,
                httpStatusCode = 0,
                invoiceId = fallbackInvoiceId,
                paymentUrl = "https://pay.cryptoeze.com/offline",
                status = "FAILED",
                responseMessage = "CryptoEze Gateway: $errorMsg. (Tip: Use 'Test Sandbox Approve' or check your internet/keys)",
                rawResponse = """{"error": "$errorMsg", "tip": "Verify CryptoEze API Key and Merchant ID in settings"}""",
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = false
            )
        }
    }
}
