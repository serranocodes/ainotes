package com.example.ainotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define DataStore for auth preferences
private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Represents basic authentication information persisted locally.
 */
data class AuthInfo(
    val userId: String = "",
    val email: String = ""
)

/**
 * Helper object for reading and writing authentication info using DataStore.
 */
object AuthPreferences {
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val EMAIL_KEY = stringPreferencesKey("email")

    /**
     * Stream authentication info as a [Flow].
     */
    fun authInfoFlow(context: Context): Flow<AuthInfo> {
        return context.dataStore.data.map { preferences ->
            AuthInfo(
                userId = preferences[USER_ID_KEY] ?: "",
                email = preferences[EMAIL_KEY] ?: ""
            )
        }
    }

    /**
     * Persist new authentication information.
     */
    suspend fun saveAuthInfo(context: Context, authInfo: AuthInfo) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = authInfo.userId
            prefs[EMAIL_KEY] = authInfo.email
        }
    }

    /**
     * Clear stored authentication information.
     */
    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}