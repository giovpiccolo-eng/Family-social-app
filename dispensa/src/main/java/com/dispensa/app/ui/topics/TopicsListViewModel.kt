package com.dispensa.app.ui.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dispensa.app.DispensaApp
import com.dispensa.app.data.model.Topic
import com.dispensa.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TopicsListViewModel(private val repo: TopicRepository) : ViewModel() {
    val topics: StateFlow<List<Topic>> = repo.topics

    fun create(title: String, subject: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.createTopic(title, subject) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteTopic(id) }
    }

    companion object {
        fun factory(app: DispensaApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { TopicsListViewModel(app.graph.topicRepository) }
        }
    }
}
