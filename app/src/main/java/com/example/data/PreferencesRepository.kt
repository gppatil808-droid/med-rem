package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.medCueDataStore: DataStore<Preferences> by preferencesDataStore(name = "medcue_settings")

data class ReminderSettings(
    val autoSpeakReminders: Boolean = true,
    val ttsSpeechRate: Float = 0.95f,
    val ttsPitch: Float = 1.0f,
    val includeInstructionsInSpeech: Boolean = true,
    val snoozeMinutes: Int = 10,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_reminders")
        val SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("tts_pitch")
        val INCLUDE_INSTRUCTIONS = booleanPreferencesKey("include_instructions_in_speech")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
    }

    val settingsFlow: Flow<ReminderSettings> = context.medCueDataStore.data.map { prefs ->
        ReminderSettings(
            autoSpeakReminders = prefs[Keys.AUTO_SPEAK] ?: true,
            ttsSpeechRate = prefs[Keys.SPEECH_RATE] ?: 0.95f,
            ttsPitch = prefs[Keys.SPEECH_PITCH] ?: 1.0f,
            includeInstructionsInSpeech = prefs[Keys.INCLUDE_INSTRUCTIONS] ?: true,
            snoozeMinutes = prefs[Keys.SNOOZE_MINUTES] ?: 10,
        )
    }

    suspend fun currentSettings(): ReminderSettings = settingsFlow.first()

    suspend fun setAutoSpeakReminders(enabled: Boolean) {
        context.medCueDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.medCueDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.5f) }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.medCueDataStore.edit { it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.75f, 1.3f) }
    }

    suspend fun setIncludeInstructionsInSpeech(include: Boolean) {
        context.medCueDataStore.edit { it[Keys.INCLUDE_INSTRUCTIONS] = include }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.medCueDataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes.coerceIn(5, 60) }
    }
}
