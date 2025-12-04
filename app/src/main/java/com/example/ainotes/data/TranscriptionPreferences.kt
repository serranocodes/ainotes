package com.example.ainotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore by preferencesDataStore("transcription_prefs")

object TranscriptionPreferences {
    private val KEY_LANGUAGE_TAG = stringPreferencesKey("transcription_language_tag")

    fun languageTagFlow(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_LANGUAGE_TAG] ?: Locale.getDefault().toLanguageTag()
        }

    suspend fun setLanguageTag(context: Context, tag: String) {
        context.dataStore.edit { it[KEY_LANGUAGE_TAG] = tag }
    }
}
