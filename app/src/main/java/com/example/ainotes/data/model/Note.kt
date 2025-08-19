package com.example.ainotes.data.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val content: String,
    val isFavorite: Boolean = false
