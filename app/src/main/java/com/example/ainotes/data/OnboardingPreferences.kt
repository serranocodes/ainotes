package com.example.ainotes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define DataStore
private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")

object OnboardingPreferences {
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    // Function to check if onboarding is completed
    fun isOnboardingCompleted(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false // Default is false
        }
    }

    // Function to save onboarding as completed
    suspend fun setOnboardingCompleted(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}
