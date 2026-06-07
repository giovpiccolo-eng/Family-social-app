package com.dispensa.app.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jsoup.Jsoup
import java.util.Locale
import java.util.UUID

/**
 * Wraps Android's TextToSpeech for the dispensa viewer.
 *
 * Each instance is tied to a Context (so we can shut down the underlying
 * engine when the screen leaves the composition) and exposes a small
 * StateFlow surface that Compose can observe.
 *
 * Long dispense are chunked into sentences and queued, because TTS has a
 * ~4000-char per-utterance limit and shorter chunks recover more cleanly
 * if the user pauses.
 */
class TtsController(context: Context) {

    enum class State { Idle, Loading, Playing, Paused, Error }

    private val _state = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        _state.value = if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ITALIAN
            State.Idle
        } else {
            State.Error
        }
    }.also { engine ->
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _state.value = State.Playing }
            override fun onDone(utteranceId: String?) {
                if (engine.isSpeaking) return
                _state.value = State.Idle
            }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) {
                _state.value = State.Error
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = State.Error
            }
        })
    }

    fun speakDispensa(html: String) {
        val chunks = sentencesFromHtml(html)
        if (chunks.isEmpty()) return
        tts.stop()
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunk, mode, null, "dispensa-${UUID.randomUUID()}")
        }
        _state.value = State.Playing
    }

    fun pauseResume(html: String) {
        when (_state.value) {
            State.Playing -> { tts.stop(); _state.value = State.Paused }
            State.Paused, State.Idle -> speakDispensa(html)
            else -> Unit
        }
    }

    fun stop() {
        tts.stop()
        _state.value = State.Idle
    }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }

    companion object {
        /**
         * Strip HTML to readable Italian and split into sentences. Skips
         * sections that are obviously diagrams (Mermaid) or hidden answers
         * (<details>) — those don't belong in the spoken audio.
         */
        fun sentencesFromHtml(html: String): List<String> {
            val doc = Jsoup.parse(html)
            doc.select("script, style, .mermaid, details").remove()
            val raw = doc.body()?.text().orEmpty()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (raw.isEmpty()) return emptyList()
            // Split on sentence boundaries; keep each chunk under ~600 chars.
            val sentences = raw.split(Regex("(?<=[.!?])\\s+"))
            val out = mutableListOf<String>()
            val buf = StringBuilder()
            for (s in sentences) {
                if (buf.length + s.length + 1 > 600) {
                    if (buf.isNotEmpty()) { out += buf.toString(); buf.clear() }
                }
                if (buf.isNotEmpty()) buf.append(' ')
                buf.append(s)
            }
            if (buf.isNotEmpty()) out += buf.toString()
            return out
        }
    }
}
