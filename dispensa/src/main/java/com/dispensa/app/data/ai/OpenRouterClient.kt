package com.dispensa.app.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
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
 * Minimal OpenRouter chat-completions client with vision support.
 *
 * Photos are downscaled and re-encoded to JPEG to keep the payload small
 * (free models on OpenRouter have strict request-size limits).
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

    suspend fun generateDispensa(
        topicTitle: String,
        subject: String,
        userNotes: String,
        photos: List<File>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKeyProvider().trim()
            require(key.isNotEmpty()) { "MISSING_API_KEY" }
            val model = modelProvider().trim().ifEmpty { "google/gemini-2.0-flash-exp:free" }

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

            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code}: ${text.take(500)}")
                }
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

    private fun encodePhotoAsDataUrl(file: File, maxEdge: Int = 1280, quality: Int = 78): String? {
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
