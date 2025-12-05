package com.example.ainotes.data.ai

import android.util.Log
import com.example.ainotes.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
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
            return@withContext cleanWithGemini(text)
        }

        try {
            val availability = runCatching { client.checkFeatureStatus().await() }
                .getOrElse { throwable ->
                    Log.w(TAG, "Failed to check proofreading availability", throwable)
                    return@withContext cleanWithGemini(text)
                }

            val isAvailable = availability?.toString() == FEATURE_AVAILABLE
            if (!isAvailable) {
                return@withContext cleanWithGemini(text)
            }

            val request = ProofreadRequest.fromText(text)
            val result = runCatching { client.proofread(request).await() }
                .getOrElse { throwable ->
                    Log.w(TAG, "Proofreading failed", throwable)
                    return@withContext cleanWithGemini(text)
                }

            result.correctedText?.takeIf { it.isNotBlank() } ?: cleanWithGemini(text)
        } catch (t: Throwable) {
            Log.w(TAG, "Error cleaning text; falling back to Gemini", t)
            cleanWithGemini(text)
        } finally {
            try {
                client.close()
            } catch (_: Throwable) {
                // Ignore close failures to keep cleanup best-effort.
            }
        }
    }

    private suspend fun cleanWithGemini(text: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return text

        val model = runCatching {
            GenerativeModel(
                modelName = GEMINI_MODEL_NAME,
                apiKey = apiKey
            )
        }.getOrElse { throwable ->
            Log.w(TAG, "Unable to create Gemini model", throwable)
            return text
        }

        return runCatching {
            model.generateContent(
                """
                You are improving note text captured by speech-to-text. Fix grammar and punctuation. Keep the same language as the input. Do not change meaning or remove information. Return only the corrected text.
                
                $text
                """.trimIndent()
            ).text?.takeIf { it.isNotBlank() } ?: text
        }.getOrElse { throwable ->
            Log.w(TAG, "Gemini cleaning failed; using original text", throwable)
            text
        }
    }

    private companion object {
        private const val TAG = "NoteTextCleaner"
        private const val GEMINI_MODEL_NAME = "gemini-1.5-flash"
        private const val FEATURE_AVAILABLE = "AVAILABLE"
    }
}
