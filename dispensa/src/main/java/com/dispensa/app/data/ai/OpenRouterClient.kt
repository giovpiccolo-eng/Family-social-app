package com.dispensa.app.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Minimal OpenRouter chat-completions client with vision support and
 * automatic fallback across a list of free vision models.
 *
 * Free OpenRouter models share strict rate limits and occasionally drop off
 * the endpoint list. When the user's primary model returns 429 (rate limit),
 * 404 (model retired) or a 5xx, we transparently try the next free vision
 * model. The first model that returns a usable response wins.
 */
class OpenRouterClient(
    private val apiKeyProvider: suspend () -> String,
    private val modelProvider: suspend () -> String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Curated free vision models on OpenRouter as of 2026-06. */
    private val FALLBACK_CHAIN = listOf(
        "google/gemma-4-31b-it:free",
        "google/gemma-4-26b-a4b-it:free",
        "moonshotai/kimi-k2.6:free",
        "nvidia/nemotron-nano-12b-v2-vl:free",
    )

    suspend fun generateDispensa(
        topicTitle: String,
        subject: String,
        userNotes: String,
        photos: List<File>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKeyProvider().trim()
            require(key.isNotEmpty()) { "MISSING_API_KEY" }

            val primary = modelProvider().trim().ifEmpty { FALLBACK_CHAIN.first() }
            val chain = (listOf(primary) + FALLBACK_CHAIN).distinct()

            val userContent = buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("text", DispensaPrompt.userPrompt(topicTitle, subject, userNotes))
                })
                photos.forEach { photo ->
                    val dataUrl = encodePhotoAsDataUrl(photo) ?: return@forEach
                    add(buildJsonObject {
                        put("type", "image_url")
                        put("image_url", buildJsonObject { put("url", dataUrl) })
                    })
                }
            }

            var lastError: String? = null
            for (model in chain) {
                val outcome = tryModel(key, model, userContent)
                outcome.onSuccess { return@runCatching it }
                outcome.onFailure { e ->
                    lastError = "[$model] ${e.message ?: "errore"}"
                    if (!shouldFallback(e.message)) throw RuntimeException(lastError)
                }
            }
            throw RuntimeException("Tutti i modelli gratuiti hanno fallito. Ultimo errore: $lastError")
        }
    }

    private suspend fun tryModel(
        key: String,
        model: String,
        userContent: kotlinx.serialization.json.JsonArray,
    ): Result<String> = runCatching {
        val body = buildJsonObject {
            put("model", model)
            put("temperature", 0.4)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", DispensaPrompt.SYSTEM)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userContent)
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("HTTP-Referer", "https://github.com/giovpiccolo-eng/family-social-app")
            .header("X-Title", "Dispensa")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        executeWithRateLimitRetry(request).use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${text.take(400)}")
            val parsed = json.parseToJsonElement(text).jsonObject
            val choices = parsed["choices"]?.jsonArray
                ?: throw RuntimeException("Risposta inattesa: ${text.take(300)}")
            val first = choices.firstOrNull()?.jsonObject
                ?: throw RuntimeException("Nessuna scelta nella risposta.")
            val content = first["message"]?.jsonObject?.get("content")
                ?: throw RuntimeException("Contenuto mancante nella risposta.")
            val html = when {
                content.jsonPrimitive.contentOrNull != null -> content.jsonPrimitive.content
                else -> content.jsonArray.joinToString("") {
                    it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                }
            }
            cleanHtml(html)
        }
    }

    /**
     * Decide whether the failure of one model should make us try the next one
     * in the chain. We fall back on rate limits, retired/missing models and
     * transient server errors, but NOT on hard auth errors (401/403) or
     * caller mistakes (missing api key).
     */
    private fun shouldFallback(msg: String?): Boolean {
        if (msg == null) return false
        if (msg.contains("MISSING_API_KEY")) return false
        return msg.contains("HTTP 429") ||
            msg.contains("HTTP 404") ||
            msg.contains("HTTP 408") ||
            msg.contains("HTTP 5") ||
            msg.contains("No endpoints found", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true)
    }

    /**
     * On 429 (rate limit) honour Retry-After (or back off ~6s then ~15s) and
     * retry up to twice on the same model. If still 429 after that, the
     * caller will switch to the next model in the chain.
     */
    private suspend fun executeWithRateLimitRetry(request: Request): okhttp3.Response {
        var attempt = 0
        while (true) {
            val resp = client.newCall(request).execute()
            if (resp.code != 429 || attempt >= 2) return resp
            val waitSec = resp.header("Retry-After")?.toLongOrNull()
                ?: if (attempt == 0) 6L else 15L
            resp.close()
            kotlinx.coroutines.delay(waitSec.coerceIn(1, 30) * 1000)
            attempt++
        }
    }

    /** Drop markdown fences if the model accidentally adds them. */
    private fun cleanHtml(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.substringAfter('\n', "")
            if (s.endsWith("```")) s = s.substringBeforeLast("```")
        }
        val i = s.indexOf("<!DOCTYPE", ignoreCase = true).coerceAtLeast(0)
        return s.substring(i).trim()
    }

    private fun encodePhotoAsDataUrl(file: File, maxEdge: Int = 1024, quality: Int = 70): String? {
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val sample = computeInSampleSize(opts.outWidth, opts.outHeight, maxEdge)
        val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
        }) ?: return null
        val scaled = scaleToMaxEdge(decoded, maxEdge)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun computeInSampleSize(w: Int, h: Int, maxEdge: Int): Int {
        var inSample = 1
        var cw = w; var ch = h
        while (cw / 2 >= maxEdge && ch / 2 >= maxEdge) {
            cw /= 2; ch /= 2; inSample *= 2
        }
        return inSample
    }

    private fun scaleToMaxEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longer = maxOf(src.width, src.height)
        if (longer <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longer
        val nw = (src.width * ratio).toInt().coerceAtLeast(1)
        val nh = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
