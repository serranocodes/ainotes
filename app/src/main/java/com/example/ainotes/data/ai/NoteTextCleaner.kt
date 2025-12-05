package com.example.ainotes.data.ai

import android.util.Log
import com.google.mlkit.nl.proofreader.ProofreadOptions
import com.google.mlkit.nl.proofreader.ProofreadRequest
import com.google.mlkit.nl.proofreader.Proofreader
import com.google.mlkit.nl.proofreader.ProofreaderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cleans note text using ML Kit's GenAI proofreading capability. The cleaner
 * falls back to the original text when the feature is unavailable or any
 * error occurs so existing flows remain stable.
 */
class NoteTextCleaner(
    private val clientProvider: () -> ProofreaderClient = {
        Proofreader.getClient(
            ProofreadOptions.Builder()
                .build()
        )
    }
) {

    suspend fun clean(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        val client = runCatching { clientProvider() }.getOrElse { throwable ->
            Log.w(TAG, "Unable to create ML Kit proofread client", throwable)
            return@withContext text
        }

        try {
            val availability = runCatching { client.checkAvailability().await() }
                .getOrElse { throwable ->
                    Log.w(TAG, "Failed to check proofreading availability", throwable)
                    return@withContext text
                }

            if (!availability.isSupported) {
                return@withContext text
            }

            val request = ProofreadRequest.fromText(text)
            val result = runCatching { client.proofread(request).await() }
                .getOrElse { throwable ->
                    Log.w(TAG, "Proofreading failed", throwable)
                    return@withContext text
                }

            result.correctedText?.takeIf { it.isNotBlank() } ?: text
        } catch (t: Throwable) {
            Log.w(TAG, "Error cleaning text; using original", t)
            text
        } finally {
            try {
                client.close()
            } catch (_: Throwable) {
                // Ignore close failures to keep cleanup best-effort.
            }
        }
    }

    private companion object {
        private const val TAG = "NoteTextCleaner"
    }
}
