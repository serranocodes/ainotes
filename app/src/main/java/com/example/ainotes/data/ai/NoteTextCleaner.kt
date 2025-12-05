package com.example.ainotes.data.ai

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.proofreading.ProofreadingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cleans note text using ML Kit's GenAI proofreading capability.
 *
 * - If the feature isn't available on the device, or any error happens,
 *   this will just return the original text.
 * - Safe to call from a coroutine; it does its work on Dispatchers.IO.
 *
 * NOTE: Right now this class is not wired into any ViewModel.
 */
class NoteTextCleaner(
    // Provider that creates a Proofreader instance when called.
    // We'll decide later where to pass context/options from.
    private val clientProvider: () -> Proofreader
) {

    suspend fun clean(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        val client = runCatching { clientProvider() }.getOrElse { throwable ->
            Log.w(TAG, "Unable to create ML Kit proofreader client", throwable)
            return@withContext text
        }

        try {
            // 1) Check if feature is actually available on this device.
            val status = runCatching {
                client.checkFeatureStatus().get()
            }.getOrElse { throwable ->
                Log.w(TAG, "Failed to check proofreading feature status", throwable)
                return@withContext text
            }

            if (status != FeatureStatus.AVAILABLE) {
                Log.d(TAG, "Proofreading feature not available (status=$status)")
                return@withContext text
            }

            // 2) Build the request with our input text.
            val request = ProofreadingRequest.builder(text).build()

            // 3) Run inference (non-streaming) and wait for result.
            val result = runCatching {
                client.runInference(request).get()
            }.getOrElse { throwable ->
                Log.w(TAG, "Proofreading inference failed", throwable)
                return@withContext text
            }

            // 4) Take the first suggestion, if any.
            val firstSuggestion = result.results.firstOrNull()
            val corrected = firstSuggestion?.text

            corrected?.takeIf { it.isNotBlank() } ?: text
        } catch (t: Throwable) {
            Log.w(TAG, "Error during proofreading; returning original text", t)
            text
        } finally {
            try {
                client.close()
            } catch (_: Throwable) {
                // Ignore close failures; best-effort cleanup.
            }
        }
    }

    private companion object {
        private const val TAG = "NoteTextCleaner"
    }
}

