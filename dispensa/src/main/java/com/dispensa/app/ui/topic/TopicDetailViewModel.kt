package com.dispensa.app.ui.topic

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class TopicDetailUi(
    val topic: Topic? = null,
    val generating: Boolean = false,
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
