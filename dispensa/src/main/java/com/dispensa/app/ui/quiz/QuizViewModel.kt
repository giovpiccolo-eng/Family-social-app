package com.dispensa.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dispensa.app.DispensaApp
import com.dispensa.app.data.quiz.QuizExtractor
import com.dispensa.app.data.repository.TopicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuizUi(
    val questions: List<QuizExtractor.QA> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val finished: Boolean = false,
)

class QuizViewModel(
    private val topicId: String,
    private val dispensaId: String,
    private val repo: TopicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUi())
    val state: StateFlow<QuizUi> = _state

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val qa = withContext(Dispatchers.IO) {
                val topic = repo.topicById(topicId) ?: return@withContext emptyList()
                val dispensa = topic.dispense.firstOrNull { it.id == dispensaId }
                    ?: return@withContext emptyList()
                val file = repo.dispensaFile(topicId, dispensa) ?: return@withContext emptyList()
                if (!file.exists()) return@withContext emptyList()
                QuizExtractor.extract(file.readText())
            }
            _state.value = QuizUi(questions = qa)
        }
    }

    fun toggleReveal() {
        _state.value = _state.value.copy(revealed = !_state.value.revealed)
    }

    fun next() {
        val s = _state.value
        if (s.index >= s.questions.lastIndex) {
            _state.value = s.copy(finished = true)
        } else {
            _state.value = s.copy(index = s.index + 1, revealed = false)
        }
    }

    fun prev() {
        val s = _state.value
        if (s.index == 0) return
        _state.value = s.copy(index = s.index - 1, revealed = false)
    }

    fun restart() {
        _state.value = _state.value.copy(index = 0, revealed = false, finished = false)
    }

    companion object {
        fun factory(app: DispensaApp, topicId: String, dispensaId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { QuizViewModel(topicId, dispensaId, app.graph.topicRepository) }
            }
    }
}
