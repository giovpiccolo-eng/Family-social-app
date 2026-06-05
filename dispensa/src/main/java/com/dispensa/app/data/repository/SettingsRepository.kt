package com.dispensa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("dispensa_settings")

class SettingsRepository(private val context: Context) {

    private val keyApi = stringPreferencesKey("openrouter_api_key")
    private val keyModel = stringPreferencesKey("openrouter_model")

    val apiKey: Flow<String> = context.dataStore.data.map { it[keyApi].orEmpty() }
    val model: Flow<String> = context.dataStore.data.map { it[keyModel] ?: DEFAULT_MODEL }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[keyApi] = value.trim() }
    }

    suspend fun setModel(value: String) {
        context.dataStore.edit { it[keyModel] = value.trim().ifBlank { DEFAULT_MODEL } }
    }

    companion object {
        /**
         * Free vision-capable model on OpenRouter as of 2026. User can change
         * this in Settings if OpenRouter rotates which models are free.
         */
        const val DEFAULT_MODEL = "google/gemini-2.0-flash-exp:free"
    }
}
