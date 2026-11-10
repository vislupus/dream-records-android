package com.vislupus.dream_records.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vislupus.dream_records.config.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User-changeable settings (persisted in the sandbox via DataStore). */
data class Settings(
    val languageCode: String = AppConfig.App.DEFAULT_LANGUAGE,
    val silenceTimeoutMin: Int = AppConfig.Recording.DEFAULT_SILENCE_TIMEOUT_MIN,
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val groqModel: String = AppConfig.Transcription.Groq.DEFAULT_MODEL,
    val elevenLabsApiKey: String = "",
    /** ISO-639-1 code passed to the transcription models; empty = auto-detect. */
    val spokenLanguage: String = "",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val SILENCE_MIN = intPreferencesKey("silence_timeout_min")
        val GEMINI = stringPreferencesKey("gemini_api_key")
        val GROQ = stringPreferencesKey("groq_api_key")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val ELEVEN = stringPreferencesKey("elevenlabs_api_key")
        val SPOKEN = stringPreferencesKey("spoken_language")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            languageCode = p[Keys.LANGUAGE] ?: AppConfig.App.DEFAULT_LANGUAGE,
            silenceTimeoutMin = p[Keys.SILENCE_MIN] ?: AppConfig.Recording.DEFAULT_SILENCE_TIMEOUT_MIN,
            geminiApiKey = p[Keys.GEMINI] ?: "",
            groqApiKey = p[Keys.GROQ] ?: "",
            groqModel = p[Keys.GROQ_MODEL] ?: AppConfig.Transcription.Groq.DEFAULT_MODEL,
            elevenLabsApiKey = p[Keys.ELEVEN] ?: "",
            spokenLanguage = p[Keys.SPOKEN] ?: "",
        )
    }

    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }
    suspend fun setSilenceTimeout(min: Int) = context.dataStore.edit { it[Keys.SILENCE_MIN] = min }
    suspend fun setGeminiKey(v: String) = context.dataStore.edit { it[Keys.GEMINI] = v.trim() }
    suspend fun setGroqKey(v: String) = context.dataStore.edit { it[Keys.GROQ] = v.trim() }
    suspend fun setGroqModel(v: String) = context.dataStore.edit { it[Keys.GROQ_MODEL] = v }
    suspend fun setElevenLabsKey(v: String) = context.dataStore.edit { it[Keys.ELEVEN] = v.trim() }
    suspend fun setSpokenLanguage(v: String) = context.dataStore.edit { it[Keys.SPOKEN] = v.trim().lowercase() }
}
