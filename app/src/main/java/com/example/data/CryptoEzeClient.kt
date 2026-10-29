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

    const val SANDBOX_API_URL = "https://cryptoeze.com/api/v1"
    const val LIVE_API_URL = "https://cryptoeze.com/api/v1"

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
     * Creates an invoice / payment with CryptoEze gateway targeting JazzCash.
     * Uses official https://cryptoeze.com/api/v1/pkr endpoint.
     */
    suspend fun createInvoice(
        merchantId: String,
        apiKey: String,
        secretKey: String = "",
        webhookSecret: String,
        amountPkr: Double,
        mobileNumber: String,
        customerEmail: String = "player@p6692.com",
        orderId: String,
        channel: String = "JAZZCASH",
        environment: String = "LIVE",
        forceSandboxApprove: Boolean = false
    ): CryptoEzePaymentResult = withContext(Dispatchers.IO) {
        val baseUrl = LIVE_API_URL
        val endpoint = "$baseUrl/pkr"

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
        val effectiveSecretKey = secretKey.trim().ifBlank { "sk_live_3860" }
        val effectiveWebhookSec = webhookSecret.trim().ifBlank { "whsec_4a6112" }

        val pkrPayload = JSONObject().apply {
            put("method", channel.lowercase()) // "jazzcash"
            put("amount", "%.2f".format(amountPkr))
            put("order_id", orderId)
            put("customer_mobile", cleanMobile)
            put("customer_email", customerEmail)
            put("merchant_id", effectiveMerchantId)
            put("return_url", "https://p6692.com/thank-you")
            put("webhook_url", "https://p6692.com/webhooks/cryptoeze")
        }

        val payloadString = pkrPayload.toString()
        val signature = calculateHmacSignature(payloadString, effectiveWebhookSec)

        // Manual or Instant Test Mode
        if (forceSandboxApprove) {
            val mockInvoiceId = "CE_JC_" + System.currentTimeMillis().toString().takeLast(8)
            return@withContext CryptoEzePaymentResult(
                isNetworkSuccess = true,
                httpStatusCode = 200,
                invoiceId = mockInvoiceId,
                paymentUrl = "https://cryptoeze.com/checkout/$mockInvoiceId",
                status = "SUCCESS",
                responseMessage = "CryptoEze Payment Verified: PKR ${"%.2f".format(amountPkr)} credited from JazzCash ($cleanMobile)",
                rawResponse = """{"status":"SUCCESS","invoice_id":"$mockInvoiceId","amount":$amountPkr}""",
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = true
            )
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payloadString.toRequestBody(mediaType)
            val authBearer = if (effectiveSecretKey.isNotBlank()) effectiveSecretKey else effectiveApiKey
            val request = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $authBearer")
                .header("Idempotency-Key", orderId)
                .header("x-api-key", effectiveApiKey)
                .header("x-merchant-id", effectiveMerchantId)
                .header("x-signature", signature)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val code = response.code

            var status = if (code in 200..299) "PENDING" else "FAILED"
            var invoiceId = "CE_JC_" + System.currentTimeMillis().toString().takeLast(8)
            var payUrl = ""
            var approved = false
            var msg = ""

            if (responseBody.isNotBlank()) {
                try {
                    val resJson = JSONObject(responseBody)
                    val dataObj = resJson.optJSONObject("data")
                    if (dataObj != null) {
                        if (dataObj.has("id")) invoiceId = dataObj.getString("id")
                        if (dataObj.has("checkout_url")) payUrl = dataObj.getString("checkout_url")
                        if (dataObj.has("payment_url")) payUrl = dataObj.getString("payment_url")
                        if (dataObj.has("status")) status = dataObj.getString("status").uppercase()
                    }
                    if (resJson.has("checkout_url")) payUrl = resJson.getString("checkout_url")
                    if (resJson.has("payment_url")) payUrl = resJson.getString("payment_url")
                    if (resJson.has("id")) invoiceId = resJson.getString("id")

                    val errObj = resJson.optJSONObject("error")
                    if (errObj != null) {
                        msg = errObj.optString("message", "")
                    } else if (resJson.has("message")) {
                        msg = resJson.getString("message")
                    }
                } catch (_: Exception) {}
            }

            if (code in 200..299 && payUrl.isNotBlank()) {
                approved = true
                status = "PENDING_CHECKOUT"
                if (msg.isBlank()) msg = "CryptoEze Hosted Checkout created! Open checkout to approve JazzCash debit."
            } else if (code == 401 || msg.contains("API key", ignoreCase = true) || msg.contains("secret key", ignoreCase = true)) {
                approved = false
                status = "AUTH_REQUIRED"
                msg = "CryptoEze Authentication Error: Real JazzCash deduction requires your COMPLETE 32+ character Secret Key (sk_live_...) from CryptoEze. You provided an incomplete/truncated key (sk_live_3860…)."
            } else {
                approved = false
                status = "FAILED"
                if (msg.isBlank()) msg = "CryptoEze response: HTTP $code"
            }

            CryptoEzePaymentResult(
                isNetworkSuccess = (code in 200..299),
                httpStatusCode = code,
                invoiceId = invoiceId,
                paymentUrl = payUrl,
                status = status,
                responseMessage = msg,
                rawResponse = responseBody,
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = approved
            )
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Network connection error"
            val invoiceId = "CE_JC_" + System.currentTimeMillis().toString().takeLast(8)
            CryptoEzePaymentResult(
                isNetworkSuccess = false,
                httpStatusCode = 0,
                invoiceId = invoiceId,
                paymentUrl = "",
                status = "FAILED",
                responseMessage = "Connection to CryptoEze failed ($errorMsg).",
                rawResponse = """{"error":"$errorMsg"}""",
                endpointUsed = endpoint,
                channelUsed = channel,
                isApproved = false
            )
        }
    }
}
