package com.example.ainotes.data.ai

import android.content.Context
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading

/**
 * Central place to construct NoteTextCleaner with the proper ML Kit client.
 *
 * ViewModels don't have a Context, so we build the cleaner here and
 * inject it via a ViewModelFactory.
 */
object NoteTextCleanerProvider {

    fun create(context: Context, languageTag: String): NoteTextCleaner {
        val clientProvider = {
            val options = ProofreaderOptions.builder(context)
                // VOICE because your text comes from speech
                .setInputType(ProofreaderOptions.InputType.VOICE)
                // Map language tag (es-ES, en-US, etc.) to ML Kit language enum
                .setLanguage(languageFromTag(languageTag))
                .build()

            Proofreading.getClient(options)
        }

        return NoteTextCleaner(clientProvider)
    }

    private fun languageFromTag(tag: String): Int {
        val lower = tag.lowercase()
        return when {
            lower.startsWith("es") -> ProofreaderOptions.Language.SPANISH
            lower.startsWith("en") -> ProofreaderOptions.Language.ENGLISH
            lower.startsWith("fr") -> ProofreaderOptions.Language.FRENCH
            lower.startsWith("de") -> ProofreaderOptions.Language.GERMAN
            lower.startsWith("it") -> ProofreaderOptions.Language.ITALIAN
            lower.startsWith("ja") -> ProofreaderOptions.Language.JAPANESE
            lower.startsWith("ko") -> ProofreaderOptions.Language.KOREAN
            else -> ProofreaderOptions.Language.ENGLISH
        }
    }
}
