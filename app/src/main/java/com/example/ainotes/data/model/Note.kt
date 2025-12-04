package com.example.ainotes.data.model

import java.util.Locale
import java.util.UUID

/**
 * Representation of a note stored in Firestore.
 * A public no-arg constructor and mutable properties
 * are required for Firestore deserialization.
 */
data class Note(
    var id: String = UUID.randomUUID().toString(),
    var timestamp: Long = System.currentTimeMillis(),
    var title: String = "",
    var content: String = "",
    var isFavorite: Boolean = false,
    var rawText: String? = null,
    var cleanText: String? = null
) {
    // Empty constructor for Firestore
    constructor() : this("", 0L, "", "", false, null, null)

    val resolvedTitle: String
        get() = if (title.isNotBlank()) title else fallbackTitleFromContent(content)

    companion object {
        const val MAX_TITLE_LENGTH = 60

        fun fallbackTitleFromContent(content: String): String {
            val normalized = content.replace(Regex("\\s+"), " ").trim()
            if (normalized.isEmpty()) return "Untitled note"

            val candidate = normalized.take(MAX_TITLE_LENGTH)
            val cleaned = candidate.trimEnd('.', ',', ';', ':', '\u2026')

            return cleaned.ifEmpty { candidate }.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
    }
}
