package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JazzCashPaymentResult(
    val isNetworkSuccess: Boolean,
    val httpStatusCode: Int,
    val responseCode: String,
    val responseMessage: String,
    val txnRefNo: String,
    val rrn: String,
    val secureHash: String,
    val rawResponse: String,
    val endpointUsed: String,
    val sentPayloadSummary: String,
    val isApproved: Boolean
)

object JazzCashClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    const val SANDBOX_DO_TRANSACTION_URL =
        "https://sandbox.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction"
    const val PRODUCTION_DO_TRANSACTION_URL =
        "https://payments.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction"

    fun calculateSecureHash(params: Map<String, String>, integritySalt: String): String {
        if (integritySalt.isBlank()) return ""
        val sortedKeys = params.filter { (k, v) ->
            k != "pp_SecureHash" && v.isNotBlank()
        }.keys.sorted()

        val sb = StringBuilder(integritySalt)
        for (k in sortedKeys) {
            val v = params[k] ?: ""
            sb.append("&").append(v)
        }

        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(integritySalt.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hashBytes = mac.doFinal(sb.toString().toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02X".format(it) }
    }

    suspend fun executeDoTransaction(
        merchantId: String,
        password: String,
        integritySalt: String,
        amountPkr: Double,
        mobileNumber: String,
        cnic: String = "",
        useSandbox: Boolean = true
    ): JazzCashPaymentResult = withContext(Dispatchers.IO) {
        val endpoint = if (useSandbox) SANDBOX_DO_TRANSACTION_URL else PRODUCTION_DO_TRANSACTION_URL

        val now = Date()
        val dtFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        val formattedDateTime = dtFormat.format(now)
        val expiryDateTime = dtFormat.format(Date(now.time + 3600 * 1000))
        val txnRefNo = "T" + formattedDateTime

        // Clean Pakistani mobile number (03XXXXXXXXX)
        var cleanMobile = mobileNumber.trim().replace(Regex("[^0-9]"), "")
        if (cleanMobile.startsWith("92")) {
            cleanMobile = "0" + cleanMobile.substring(2)
        } else if (cleanMobile.length == 10 && cleanMobile.startsWith("3")) {
            cleanMobile = "0" + cleanMobile
        }
        if (cleanMobile.isBlank()) cleanMobile = "03001234567"

        val amountInPaisa = (amountPkr * 100).toLong().toString()
        val cleanBillRef = "billRef" + System.currentTimeMillis().toString().takeLast(8)

        val cleanMerchantId = merchantId.trim().ifBlank { "MC895945" }
        val cleanPassword = password.trim().ifBlank { "z84t9fx8c8" }
        val cleanSalt = integritySalt.trim().ifBlank { "498z5u0th4" }

        val params = mutableMapOf<String, String>(
            "pp_Version" to "1.1",
            "pp_TxnType" to "MWALLET",
            "pp_Language" to "EN",
            "pp_MerchantID" to cleanMerchantId,
            "pp_SubMerchantID" to "",
            "pp_Password" to cleanPassword,
            "pp_BankID" to "TBANK",
            "pp_ProductID" to "REWP",
            "pp_TxnRefNo" to txnRefNo,
            "pp_Amount" to amountInPaisa,
            "pp_TxnCurrency" to "PKR",
            "pp_TxnDateTime" to formattedDateTime,
            "pp_BillReference" to cleanBillRef,
            "pp_Description" to "P6692 Real PKR Deposit",
            "pp_TxnExpiryDateTime" to expiryDateTime,
            "pp_ReturnURL" to "https://sandbox.jazzcash.com.pk/api/callback",
            "pp_MobileNumber" to cleanMobile,
            "ppmpf_1" to cleanMobile,
            "ppmpf_2" to "2",
            "ppmpf_3" to "3",
            "ppmpf_4" to "4",
            "ppmpf_5" to "5"
        )

        val cleanCnic = cnic.trim().replace(Regex("[^0-9]"), "")
        if (cleanCnic.length >= 6) {
            params["pp_CNIC"] = cleanCnic.takeLast(6)
        }

        val secureHash = calculateSecureHash(params, cleanSalt)
        params["pp_SecureHash"] = secureHash

        val jsonObject = JSONObject()
        for ((k, v) in params) {
            jsonObject.put(k, v)
        }
        val requestBodyJson = jsonObject.toString()

        try {
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val httpCode = response.code
            val bodyString = response.body?.string().orEmpty()

            var respCode = ""
            var respMsg = ""
            var rrn = ""
            var returnedRef = txnRefNo
            var retHash = ""

            try {
                val jsonResp = JSONObject(bodyString)
                respCode = jsonResp.optString("pp_ResponseCode", "")
                respMsg = jsonResp.optString("pp_ResponseMessage", "")
                rrn = jsonResp.optString("pp_RetreivalReferenceNo", "")
                returnedRef = jsonResp.optString("pp_TxnRefNo", txnRefNo)
                retHash = jsonResp.optString("pp_SecureHash", "")
            } catch (e: Exception) {
                respMsg = bodyString.take(200)
            }

            val isApproved = (respCode == "000")

            JazzCashPaymentResult(
                isNetworkSuccess = true,
                httpStatusCode = httpCode,
                responseCode = respCode,
                responseMessage = respMsg,
                txnRefNo = returnedRef,
                rrn = rrn,
                secureHash = retHash,
                rawResponse = bodyString,
                endpointUsed = endpoint,
                sentPayloadSummary = "Ref: $txnRefNo | PKR $amountPkr ($amountInPaisa paisa) | Mobile: $cleanMobile | Hash: ${secureHash.take(12)}...",
                isApproved = isApproved
            )
        } catch (e: Exception) {
            JazzCashPaymentResult(
                isNetworkSuccess = false,
                httpStatusCode = 0,
                responseCode = "NET_ERR",
                responseMessage = e.message ?: "Network Connection Failure",
                txnRefNo = txnRefNo,
                rrn = "",
                secureHash = "",
                rawResponse = e.toString(),
                endpointUsed = endpoint,
                sentPayloadSummary = "Ref: $txnRefNo | PKR $amountPkr | Mobile: $cleanMobile",
                isApproved = false
            )
        }
    }
}
