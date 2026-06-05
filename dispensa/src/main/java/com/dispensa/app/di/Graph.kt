package com.dispensa.app.di

import android.content.Context
import com.dispensa.app.data.ai.OpenRouterClient
import com.dispensa.app.data.repository.SettingsRepository
import com.dispensa.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.first

/**
 * Tiny manual DI — no Hilt/Koin. Built once in DispensaApp.
 */
class Graph(context: Context) {
    val topicRepository = TopicRepository(context)
    val settingsRepository = SettingsRepository(context)
    val openRouter = OpenRouterClient(
        apiKeyProvider = { settingsRepository.apiKey.first() },
        modelProvider = { settingsRepository.model.first() },
    )
}
