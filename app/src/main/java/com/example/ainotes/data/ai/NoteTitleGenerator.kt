package com.example.ainotes.data.ai

import android.util.Log
import com.example.ainotes.BuildConfig
import com.example.ainotes.data.model.Note
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for generating concise note titles using Gemini. The implementation
 * gracefully falls back to a heuristic title when the API key is missing or the
 * network request fails so that legacy Firestore documents continue to render.
 */
object NoteTitleGenerator {
    private const val TAG = "NoteTitleGenerator"
    private const val MODEL_NAME = "gemini-1.5-flash"

    suspend fun generateTitle(transcript: String): String = withContext(Dispatchers.IO) {
        val normalized = transcript.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) {
            return@withContext Note.fallbackTitleFromContent(transcript)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Note.fallbackTitleFromContent(normalized)
        }

        try {
            val prompt = content {
                text("You are naming voice notes.")
                text("Write a short descriptive title (max 8 words) for the transcript below.")
                text("Do not use quotation marks or punctuation at the end.")
                text("Transcript:\n$normalized")
            }

            val model = GenerativeModel(modelName = MODEL_NAME, apiKey = apiKey)
            val response = model.generateContent(prompt)
            val rawTitle = response.candidates.orEmpty()
                .flatMap { candidate -> candidate.content?.parts.orEmpty() }
                .filterIsInstance<TextPart>()
                .joinToString(separator = " ") { it.text }
                .lineSequence()
                .firstOrNull()
                ?.trim()
                .orEmpty()

            val cleaned = clean(rawTitle)
            if (cleaned.isNotEmpty()) cleaned else Note.fallbackTitleFromContent(normalized)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to generate title with Gemini; using fallback", t)
            Note.fallbackTitleFromContent(normalized)
        }
    }

    private fun clean(input: String): String {
        if (input.isBlank()) return ""
        val collapsed = input.replace(Regex("\\s+"), " ").trim()
        val withoutQuotes = collapsed.trim('"', '\'', '“', '”', '„')
        val withoutTrailingPunctuation = withoutQuotes.trimEnd('.', ',', ';', ':', '!', '?', '\u2026')
        return withoutTrailingPunctuation.take(Note.MAX_TITLE_LENGTH).trim()
    }
}
