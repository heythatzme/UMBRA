package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.model.EditState
import com.example.model.NoirPresetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object GeminiDarkroomAssistant {

    data class DarkroomAnalysis(
        val recommendedPreset: NoirPresetId,
        val rationale: String,
        val dynamicRangeEvaluation: String,
        val tonalBalance: String,
        val proposedEdits: EditState
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes image tonal profile using Gemini 2.5 Flash if API key is present,
     * or uses advanced local photographic histogram analysis as an instant offline darkroom engine.
     */
    suspend fun analyzePhoto(
        previewBitmap: Bitmap,
        currentEditState: EditState
    ): DarkroomAnalysis = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val cloudAnalysis = callGeminiVision(previewBitmap, apiKey)
                if (cloudAnalysis != null) return@withContext cloudAnalysis
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: Local darkroom computational histogram & luminance analyzer
        analyzeLocally(previewBitmap, currentEditState)
    }

    private fun callGeminiVision(bitmap: Bitmap, apiKey: String): DarkroomAnalysis? {
        // Downscale to 512px for rapid vision inference
        val maxDim = 512
        val scale = min(1f, maxDim.toFloat() / max(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap

        val byteStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, byteStream)
        val base64Data = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            You are a master monochrome fine-art darkroom printer.
            Analyze this photograph's tonal distribution, contrast, highlights, shadows, and mood.
            Recommend the most compelling NOIR preset among:
            PURE, DEEP_NOIR, OBSCURA, SILVER, HARD_NOIR, SOFT_NOIR, FILM_NOIR, LOW_KEY, HIGH_KEY, COLD_NOIR, VINTAGE_BW, PORTRAIT_NOIR.
            
            Respond strictly in valid JSON format with:
            {
              "preset": "DEEP_NOIR",
              "rationale": "High textural contrast in the subject benefits from rich, deep blacks while preserving fine highlights.",
              "dynamicRange": "Rich wide midtone spectrum with deep shadows",
              "tonalBalance": "Shadow dominant (65%)",
              "contrast": 25,
              "blacks": -20,
              "whites": 15,
              "fade": 0,
              "vignette": 15
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            val textPart = JSONObject().apply { put("text", prompt) }
            val imagePart = JSONObject().apply {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Data)
                }
                put("inlineData", inlineData)
            }

            parts.put(textPart)
            parts.put(imagePart)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val root = JSONObject(responseBody)
        val text = root.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        // Parse JSON output from Gemini
        val cleanJson = text.substringAfter("{").substringBeforeLast("}")
        val parsed = JSONObject("{$cleanJson}")

        val presetName = parsed.optString("preset", "DEEP_NOIR")
        val preset = try {
            NoirPresetId.valueOf(presetName)
        } catch (_: Exception) {
            NoirPresetId.DEEP_NOIR
        }

        val baseEdit = EditState.fromPreset(preset).copy(
            contrast = parsed.optInt("contrast", 20),
            blacks = parsed.optInt("blacks", -15),
            whites = parsed.optInt("whites", 15),
            fade = parsed.optInt("fade", 0),
            vignetteAmount = parsed.optInt("vignette", 15)
        )

        return DarkroomAnalysis(
            recommendedPreset = preset,
            rationale = parsed.optString("rationale", "Optimal tonal separation for this composition."),
            dynamicRangeEvaluation = parsed.optString("dynamicRange", "Balanced tonal spectrum"),
            tonalBalance = parsed.optString("tonalBalance", "Cinematic monochrome balance"),
            proposedEdits = baseEdit
        )
    }

    /**
     * High-speed photographic histogram analyzer for local on-device intelligence.
     */
    private fun analyzeLocally(bitmap: Bitmap, current: EditState): DarkroomAnalysis {
        val w = bitmap.width
        val h = bitmap.height
        val sampleStep = max(1, (w * h) / 10000)

        var totalLuma = 0.0
        var darkCount = 0
        var midCount = 0
        var brightCount = 0
        var sampleCount = 0

        val pixels = IntArray(min(w * h, 10000))
        var idx = 0

        for (y in 0 until h step max(1, h / 100)) {
            for (x in 0 until w step max(1, w / 100)) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalLuma += luma
                sampleCount++

                when {
                    luma < 64 -> darkCount++
                    luma > 192 -> brightCount++
                    else -> midCount++
                }
            }
        }

        val avgLuma = if (sampleCount > 0) totalLuma / sampleCount else 128.0
        val darkRatio = darkCount.toDouble() / max(1, sampleCount)
        val brightRatio = brightCount.toDouble() / max(1, sampleCount)

        val recommendedPreset = when {
            darkRatio > 0.45 -> NoirPresetId.LOW_KEY
            brightRatio > 0.35 -> NoirPresetId.HIGH_KEY
            avgLuma < 100 -> NoirPresetId.DEEP_NOIR
            avgLuma > 160 -> NoirPresetId.SILVER
            darkRatio > 0.25 && brightRatio > 0.20 -> NoirPresetId.HARD_NOIR
            else -> NoirPresetId.FILM_NOIR
        }

        val rationale = when (recommendedPreset) {
            NoirPresetId.LOW_KEY -> "Strong shadows dominate this frame. LOW KEY accentuates atmospheric mood while keeping key highlights pristine."
            NoirPresetId.HIGH_KEY -> "Luminous composition detected. HIGH KEY creates an ethereal, airy monochromatic look with gentle gradation."
            NoirPresetId.DEEP_NOIR -> "High dynamic range with rich midtone architecture. DEEP NOIR yields cinematic depth and rich blacks."
            NoirPresetId.HARD_NOIR -> "High contrast scenes with prominent edges. HARD NOIR yields intense graphical separation."
            NoirPresetId.SILVER -> "Even, nuanced lighting. SILVER delivers gentle tonal transitions with luminous silver highlights."
            else -> "Classic tonal balance. FILM NOIR brings organic texture, subtle vignette, and 35mm grain."
        }

        val dynamicRange = when {
            darkRatio > 0.2 && brightRatio > 0.2 -> "Extended High Dynamic Range (Shadows & Highlights present)"
            brightRatio > 0.4 -> "High-Key Luminous Distribution"
            darkRatio > 0.4 -> "Low-Key Deep Shadow Profile"
            else -> "Balanced Midtone Centric Range"
        }

        val tonalBalance = String.format(
            java.util.Locale.US,
            "Shadows: %d%% | Midtones: %d%% | Highlights: %d%%",
            (darkRatio * 100).toInt(),
            ((midCount.toDouble() / max(1, sampleCount)) * 100).toInt(),
            (brightRatio * 100).toInt()
        )

        return DarkroomAnalysis(
            recommendedPreset = recommendedPreset,
            rationale = rationale,
            dynamicRangeEvaluation = dynamicRange,
            tonalBalance = tonalBalance,
            proposedEdits = EditState.fromPreset(recommendedPreset)
        )
    }
}
