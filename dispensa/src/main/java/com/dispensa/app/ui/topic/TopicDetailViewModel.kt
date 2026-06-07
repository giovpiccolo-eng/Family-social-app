package com.dispensa.app.ui.topic

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dispensa.app.DispensaApp
import com.dispensa.app.data.ai.OpenRouterClient
import com.dispensa.app.data.model.Photo
import com.dispensa.app.data.model.Topic
import com.dispensa.app.data.repository.TopicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val MAX_PDF_PAGES = 25
private const val PDF_RENDER_TARGET_WIDTH = 1600

data class TopicDetailUi(
    val topic: Topic? = null,
    val generating: Boolean = false,
    val importingPdf: Boolean = false,
    val infoMessage: String? = null,
    val generationError: String? = null,
)

class TopicDetailViewModel(
    private val topicId: String,
    private val repo: TopicRepository,
    private val openRouter: OpenRouterClient,
) : ViewModel() {

    private val local = MutableStateFlow(TopicDetailUi())

    val state: StateFlow<TopicDetailUi> =
        combine(repo.topics, local) { topics, ui ->
            ui.copy(topic = topics.firstOrNull { it.id == topicId })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TopicDetailUi())

    fun newPhotoFile(): File = repo.newPhotoFile(topicId)

    fun onPhotoTaken(file: File) {
        viewModelScope.launch {
            if (file.exists() && file.length() > 0) repo.addPhoto(topicId, file)
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch { repo.removePhoto(topicId, photo.id) }
    }

    fun photoFile(photo: Photo): File = repo.photoFile(topicId, photo)

    fun deleteDispensa(dispensaId: String) {
        viewModelScope.launch { repo.deleteDispensa(topicId, dispensaId) }
    }

    fun generate(notes: String, onReady: (String) -> Unit) {
        val topic = repo.topicById(topicId) ?: return
        if (topic.photos.isEmpty()) return
        viewModelScope.launch {
            local.value = local.value.copy(generating = true, generationError = null)
            val started = repo.startDispensa(topicId, notes)
            val files = topic.photos.map { repo.photoFile(topicId, it) }
            openRouter.generateDispensa(
                topicTitle = topic.title,
                subject = topic.subject,
                userNotes = notes,
                photos = files,
            ).onSuccess { html ->
                repo.completeDispensa(topicId, started.id, html)
                local.value = TopicDetailUi(generating = false)
                onReady(started.id)
            }.onFailure { e ->
                repo.failDispensa(topicId, started.id, e.message ?: "errore")
                local.value = local.value.copy(
                    generating = false,
                    generationError = friendly(e.message),
                )
            }
        }
    }

    fun clearError() {
        local.value = local.value.copy(generationError = null)
    }

    fun clearInfo() {
        local.value = local.value.copy(infoMessage = null)
    }

    /**
     * Render each page of the picked PDF as a JPEG and add it to the topic as
     * a regular photo. The generation flow then treats them like any other
     * source image.
     */
    fun importPdf(contentResolver: ContentResolver, uri: Uri, cacheDir: File) {
        viewModelScope.launch {
            local.value = local.value.copy(importingPdf = true, generationError = null, infoMessage = null)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val tempPdf = File(cacheDir, "import_${UUID.randomUUID()}.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        tempPdf.outputStream().use { input.copyTo(it) }
                    } ?: throw IllegalStateException("Cannot open PDF stream")

                    val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
                    val totalPages: Int
                    val rendered: Int
                    PdfRenderer(pfd).use { renderer ->
                        totalPages = renderer.pageCount
                        rendered = minOf(totalPages, MAX_PDF_PAGES)
                        for (i in 0 until rendered) {
                            renderer.openPage(i).use { page ->
                                val ratio = PDF_RENDER_TARGET_WIDTH.toFloat() / page.width
                                val targetW = PDF_RENDER_TARGET_WIDTH
                                val targetH = (page.height * ratio).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                Canvas(bmp).drawColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val outFile = repo.newPhotoFile(topicId)
                                outFile.outputStream().use {
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, it)
                                }
                                bmp.recycle()
                                repo.addPhoto(topicId, outFile)
                            }
                        }
                    }
                    pfd.close()
                    tempPdf.delete()
                    rendered to totalPages
                }
            }
            result.onSuccess { (imported, total) ->
                val msg = if (total > imported)
                    "PDF con troppe pagine: importate solo le prime $imported."
                else
                    "Importate $imported pagine dal PDF."
                local.value = local.value.copy(importingPdf = false, infoMessage = msg)
            }.onFailure {
                local.value = local.value.copy(
                    importingPdf = false,
                    generationError = "Impossibile aprire il PDF: ${it.message?.take(120) ?: ""}",
                )
            }
        }
    }

    private fun friendly(msg: String?): String {
        if (msg == null) return "Errore sconosciuto."
        return when {
            msg.contains("MISSING_API_KEY") -> "Imposta prima la tua API key OpenRouter nelle Impostazioni."
            msg.contains("401") -> "Chiave API non valida. Verifica nelle Impostazioni."
            msg.contains("429") -> "Limite di richieste raggiunto. Riprova fra qualche minuto."
            msg.contains("UnknownHost", true) || msg.contains("timeout", true) ->
                "Problema di rete. Verifica la connessione."
            else -> "Errore: ${msg.take(180)}"
        }
    }

    companion object {
        fun factory(app: DispensaApp, topicId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TopicDetailViewModel(topicId, app.graph.topicRepository, app.graph.openRouter)
            }
        }
    }
}
