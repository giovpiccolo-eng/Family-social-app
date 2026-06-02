package com.familynest.app.ui.reader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderState(
    val documentTitle: String = "",
    val words: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val wpm: Int = 250,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loadGeneration: Int = 0,
) {
    val hasDocument: Boolean get() = words.isNotEmpty()
    val progress: Float get() = if (words.size <= 1) 0f else currentIndex.toFloat() / (words.size - 1)
    val currentWord: String get() = words.getOrElse(currentIndex) { "" }
    val prevWord: String get() = words.getOrElse(currentIndex - 1) { "" }
    val nextWord: String get() = words.getOrElse(currentIndex + 1) { "" }
    val minutesRemaining: Int get() {
        val wordsLeft = (words.size - currentIndex).coerceAtLeast(0)
        return (wordsLeft / wpm.toFloat()).toInt()
    }
}

/** Returns the 0-based index of the Optimal Recognition Point letter in [word]. */
fun orpIndex(word: String): Int {
    if (word.isEmpty()) return 0
    val idx = when (word.length) {
        1, 2, 3 -> 0
        4, 5, 6 -> 1
        7, 8, 9 -> 2
        10, 11, 12, 13 -> 3
        else -> 4
    }
    return idx.coerceAtMost(word.length - 1)
}

class SpeedReaderViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var playJob: Job? = null

    fun loadDocument(uri: Uri, context: Context) {
        playJob?.cancel()
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, isPlaying = false)
            try {
                val (title, text) = withContext(Dispatchers.IO) {
                    extractDocument(uri, context)
                }
                val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (words.isEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "No readable text found in this document.")
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        documentTitle = title,
                        words = words,
                        currentIndex = 0,
                        isPlaying = false,
                        loadGeneration = _state.value.loadGeneration + 1,
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Could not read document: ${e.localizedMessage}",
                )
            }
        }
    }

    private fun extractDocument(uri: Uri, context: Context): Pair<String, String> {
        val contentResolver = context.contentResolver
        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && col >= 0) cursor.getString(col) else "Document"
        } ?: "Document"

        val mimeType = contentResolver.getType(uri)
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open file")

        val rawText = inputStream.use { stream ->
            if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                PDDocument.load(stream).use { doc -> PDFTextStripper().getText(doc) }
            } else {
                stream.bufferedReader().readText()
            }
        }

        val title = fileName.substringBeforeLast('.')
        return title to rawText
    }

    fun playPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    private fun play() {
        val words = _state.value.words
        if (words.isEmpty()) return
        val startIdx = if (_state.value.currentIndex >= words.size - 1) 0 else _state.value.currentIndex
        _state.value = _state.value.copy(currentIndex = startIdx, isPlaying = true)
        playJob?.cancel()
        playJob = viewModelScope.launch {
            var idx = startIdx
            while (idx < _state.value.words.size - 1) {
                delay(60_000L / _state.value.wpm)
                idx++
                _state.value = _state.value.copy(currentIndex = idx)
            }
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun pause() {
        playJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun setWpm(wpm: Int) {
        _state.value = _state.value.copy(wpm = wpm.coerceIn(60, 1000))
    }

    fun applyWpm() {
        if (_state.value.isPlaying) play()
    }

    fun seek(delta: Int) {
        val s = _state.value
        _state.value = s.copy(
            currentIndex = (s.currentIndex + delta).coerceIn(0, (s.words.size - 1).coerceAtLeast(0))
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
    }
}
