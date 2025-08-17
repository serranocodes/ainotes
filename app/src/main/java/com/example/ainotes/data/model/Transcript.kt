package com.example.ainotes.data.model

/**
 * Represents a saved transcript.
 */
data class Transcript(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
