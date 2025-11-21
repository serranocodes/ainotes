package com.example.ainotes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ai_summary_prefs")

object AiSummaryPreferences {
    private val KEY_AUTO_TITLE = booleanPreferencesKey("auto_title_summary")
    private val KEY_AUTO_NOTE = booleanPreferencesKey("auto_note_summary")

    fun autoTitleEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_AUTO_TITLE] ?: false }

    fun autoNoteEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_AUTO_NOTE] ?: false }

    suspend fun setAutoTitle(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_TITLE] = enabled }
    }

    suspend fun setAutoNote(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_NOTE] = enabled }
    }
}
