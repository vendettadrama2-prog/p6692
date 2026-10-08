package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Fast tactical advice & game analysis using gemini-3.5-flash
     */
    suspend fun getGameStrategy(gameTitle: String, userQuestion: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                "🎮 **P6692 Cyber AI Strategy: $gameTitle**\n\n" +
                "1. **Volatility Rhythm:** In this entertainment demo mode, multipliers cycle through low, medium, and high-wave bands.\n" +
                "2. **Risk Distribution:** Keep individual demo bets below 5% of your total demo wallet to withstand dry streaks.\n" +
                "3. **Optimal Bail Point:** For Rocket 92, exiting between 1.85x and 2.40x yields a steady demo win rate.\n" +
                "4. **Entertainment Rule:** Remember that all credits are for demo recreation only. Play smart and celebrate big combos!"
            )
        }

        try {
            val url = "${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey"
            val prompt = """
                You are the elite Cyber AI Game Advisor for P6692, a premium entertainment gaming platform.
                Provide tactical advice, volatility analysis, and entertainment gameplay tips for the game: "$gameTitle".
                User context: "$userQuestion".
                Keep it concise (3-4 bullet points), exciting, stylized with cyberpunk gaming terminology, and remind the player this is demo entertainment.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "No analysis generated."

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ultra-fast lucky cyber oracle forecast using gemini-3.1-flash-lite
     */
    suspend fun getLuckyForecast(category: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val luckyNum = (10..99).random()
            val luckyMult = String.format("%.2fx", (2.0 + Math.random() * 8.0))
            return@withContext Result.success(
                "⚡ **P6692 Cyber Oracle Forecast**\n" +
                "• Quantum Frequency: **#$luckyNum**\n" +
                "• Recommended Category: **$category**\n" +
                "• Projected Peak Multiplier: **$luckyMult**\n" +
                "• Cyber Alignment: High resonance detected in neon reels!"
            )
        }

        try {
            val url = "${BASE_URL}gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
            val prompt = "Generate a very brief, 3-line cyberpunk gaming fortune forecast for the '$category' section on P6692 platform. Include a lucky quantum number and target multiplier."
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                }
                put("contents", contents)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                ?: "Cyber Oracle online."

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * High-Quality Image Generation with gemini-3-pro-image-preview
     * Supports user specified size: "1K", "2K", "4K"
     */
    suspend fun generateAvatarImage(
        prompt: String,
        imageSize: String = "1K" // "1K", "2K", "4K"
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Please configure your Gemini API Key in AI Studio Secrets to generate high-resolution AI avatars."))
        }

        try {
            val url = "${BASE_URL}gemini-3-pro-image-preview:generateContent?key=$apiKey"

            val fullPrompt = "Cyberpunk gaming avatar badge, neon purple #8B5CF6 and cyan #22D3EE highlights, sleek futuristic portrait: $prompt"

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", fullPrompt))
                        })
                    })
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("IMAGE")
                    })
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", "1:1")
                        put("imageSize", imageSize) // 1K, 2K, 4K
                    })
                }
                put("generationConfig", generationConfig)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Image Generation API error code ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")

            var imageBase64: String? = null
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    if (inlineData != null) {
                        imageBase64 = inlineData.optString("data")
                        break
                    }
                }
            }

            if (imageBase64.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No image returned from Gemini model."))
            }

            val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ?: return@withContext Result.failure(Exception("Failed to decode image data."))

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
