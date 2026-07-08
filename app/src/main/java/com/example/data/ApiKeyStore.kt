package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.apiKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "dream_journal_settings")

class ApiKeyStore(private val context: Context) {
    private val apiKeyPreference = stringPreferencesKey("gemini_api_key")
    private val analyzeWithAiPreference = booleanPreferencesKey("analyze_with_ai_default")

    val apiKey: Flow<String> = context.apiKeyDataStore.data.map { prefs ->
        prefs[apiKeyPreference].orEmpty().trim()
    }

    val analyzeWithAiDefault: Flow<Boolean> = context.apiKeyDataStore.data.map { prefs ->
        prefs[analyzeWithAiPreference] ?: false
    }

    suspend fun saveApiKey(key: String) {
        context.apiKeyDataStore.edit { prefs ->
            prefs[apiKeyPreference] = key.trim()
        }
    }

    suspend fun readApiKey(): String {
        return apiKey.first()
    }

    suspend fun saveAnalyzeWithAiDefault(enabled: Boolean) {
        context.apiKeyDataStore.edit { prefs ->
            prefs[analyzeWithAiPreference] = enabled
        }
    }
}