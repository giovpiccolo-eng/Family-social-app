package com.dispensa.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dispensa.app.DispensaApp
import com.dispensa.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    val apiKey: StateFlow<String> = repo.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val model: StateFlow<String> = repo.model
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_MODEL)

    fun save(apiKey: String, model: String) {
        viewModelScope.launch {
            repo.setApiKey(apiKey)
            repo.setModel(model)
        }
    }

    companion object {
        fun factory(app: DispensaApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(app.graph.settingsRepository) }
        }
    }
}
