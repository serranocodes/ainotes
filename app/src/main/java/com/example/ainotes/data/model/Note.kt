package com.example.ainotes.data.model

import java.util.UUID

/**
 * Representation of a note stored in Firestore.
 * A public no-arg constructor and mutable properties
 * are required for Firestore deserialization.
 */
data class Note(
    var id: String = UUID.randomUUID().toString(),
    var timestamp: Long = System.currentTimeMillis(),
    var content: String = "",
    var isFavorite: Boolean = false
) {
    // Empty constructor for Firestore
    constructor() : this("", 0L, "", false)
}
