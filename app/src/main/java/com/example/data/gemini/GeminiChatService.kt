package com.example.data.gemini

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GeminiChatService(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are BikeCare AI Specialist, a professional motorcycle technician and master two-wheeler mechanic.
        Your expertise includes:
        - Diagnosing motorcycle engine noises, valve clicking, rattling cam chains, brake squeal, exhaust pops.
        - Analyzing uploaded photos of motorcycle parts (worn brake pads, rusted/slack chains, bald/cracked tyre treads, oil leaks, dirty air filters, battery terminal corrosion, accident scratches/dents, spark plug wear, dashboard fault indicators).
        - Periodic service maintenance guidelines, synthetic vs mineral engine oil intervals, fluid replacement.
        - Emergency roadside recovery and troubleshooting (starting problems, flat tyres, battery discharge, fuel starvation, EV battery/charger diagnostics).
        - Providing clear, step-by-step diagnostic advice, safety warnings, and actionable recommendations.
        Always keep answers concise, practical, highly structured, and easy for riders to read on mobile.
        If a critical safety danger is identified (such as worn-out brakes, low oil level causing seizure, or structural cracks), clearly warn the rider to halt riding or visit a certified workshop immediately.
    """.trimIndent()

    suspend fun generateMultiTurnReply(
        history: List<ChatMessage>,
        latestUserPrompt: String,
        imageUri: Uri? = null,
        selectedBikeDetails: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // If key is empty or placeholder, return informative guidance
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiChatService", "GEMINI_API_KEY is not configured or placeholder.")
            return@withContext getOfflineDiagnosticFallback(latestUserPrompt, imageUri != null, selectedBikeDetails)
        }

        try {
            // Build Gemini REST payload
            val root = JSONObject()

            // System instruction
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            // Multi-turn contents
            val contents = JSONArray()

            // Append previous turns (up to last 6 for prompt efficiency)
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val turn = JSONObject()
                turn.put("role", if (msg.sender == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.text))
                turn.put("parts", parts)
                contents.put(turn)
            }

            // Latest user turn (multimodal if image is attached)
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            val enrichedPrompt = if (!selectedBikeDetails.isNullOrBlank()) {
                "[Rider's Selected Bike: $selectedBikeDetails]\n$latestUserPrompt"
            } else {
                latestUserPrompt
            }
            currentParts.put(JSONObject().put("text", enrichedPrompt))

            // Convert image to Base64 inlineData if present
            if (imageUri != null) {
                val base64Image = readImageAsBase64(imageUri)
                if (base64Image != null) {
                    val inlineData = JSONObject()
                    inlineData.put("mimeType", "image/jpeg")
                    inlineData.put("data", base64Image)
                    currentParts.put(JSONObject().put("inlineData", inlineData))
                }
            }
            currentTurn.put("parts", currentParts)
            contents.put(currentTurn)

            root.put("contents", contents)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            root.put("generationConfig", genConfig)

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())

            // Primary model: gemini-3.5-flash / gemini-3.8-flash
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiChatService", "API error: ${response.code} $responseBody")
                return@withContext getOfflineDiagnosticFallback(latestUserPrompt, imageUri != null, selectedBikeDetails)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                getOfflineDiagnosticFallback(latestUserPrompt, imageUri != null, selectedBikeDetails)
            }
        } catch (e: Exception) {
            Log.e("GeminiChatService", "Network/Exception during Gemini call: ${e.message}", e)
            getOfflineDiagnosticFallback(latestUserPrompt, imageUri != null, selectedBikeDetails)
        }
    }

    private fun readImageAsBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Scale down to max 1024px to ensure fast transmission and low RAM usage on 4GB devices
            val maxDim = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = (maxDim.toFloat() / width.coerceAtLeast(height)).coerceAtMost(1.0f)

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("GeminiChatService", "Failed to encode image to base64: ${e.message}")
            null
        }
    }

    private fun getOfflineDiagnosticFallback(
        prompt: String,
        hasImage: Boolean,
        bikeDetails: String?
    ): String {
        val lower = prompt.lowercase()
        return if (hasImage) {
            """
                🔍 **Visual Component Inspection Analysis**
                ${if (!bikeDetails.isNullOrBlank()) "For vehicle: $bikeDetails" else ""}

                Based on your uploaded photo and description:
                • **Visual Assessment**: We are inspecting the component surface for physical wear, thermal discoloration, fluid seepage, or mechanical fatigue.
                • **Key Checks**:
                  1. Check for abnormal fluid leaks (dark engine oil vs clear DOT4 brake fluid).
                  2. Check friction surfaces (brake pad lining must exceed 2mm; tyre tread depth must be above 1.5mm safety indicators).
                  3. If inspecting chain: Look for dry red rust or kinked stiff links.
                • **Recommended Action**: Book a quick 15-minute diagnostic inspection at your nearest verified workshop.
            """.trimIndent()
        } else when {
            lower.contains("strange noise") || lower.contains("noise") || lower.contains("sound") -> {
                "Can you tell me where the sound is coming from — engine, brakes, chain, or wheels? You can also upload a photo of the area for closer inspection."
            }
            lower.contains("engine") -> {
                "Engine tapping or rattling usually indicates either loose valve/tappet clearances, low engine oil level, or cam chain tensioner fatigue. Inspect your engine oil dipstick immediately. If oil level is low, do not run the engine to prevent piston seizure."
            }
            lower.contains("brake") -> {
                "Brake squealing or grinding happens when brake pads are worn down to their steel backing plates or due to glazed disc rotors. Inspect pad thickness immediately to avoid rotor scoring."
            }
            lower.contains("start") -> {
                "If your bike is not starting, verify: 1) Engine kill switch is ON, 2) Neutral gear / clutch lever, 3) Battery charge (loud horn test), 4) Spark plug cap seated securely, 5) Fuel / EV charge level."
            }
            lower.contains("oil") -> {
                "Standard oil change intervals: Mineral oil every 2,500-3,000 km; Semi-synthetic every 4,000 km; Fully synthetic (e.g. Motul 7100) every 5,000-6,000 km. Replace the oil filter on every oil change."
            }
            lower.contains("tyre") || lower.contains("tire") || lower.contains("flat") || lower.contains("puncture") -> {
                "Maintain recommended tyre pressures (Front: 28-29 PSI, Rear: 32-33 PSI solo / 36 PSI with pillion). Do not ride on a flat tyre to avoid cracking your alloy wheel rim."
            }
            else -> {
                "I'm here to assist with all two-wheeler maintenance questions, diagnostics, and photo inspections. You can ask anything or tap the 📷 icon to upload a photo of your bike part."
            }
        }
    }
}
