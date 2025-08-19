package com.example.ainotes.data.repository

import com.example.ainotes.data.firebase.FirestoreSource
import com.example.ainotes.data.model.Note
import kotlinx.coroutines.flow.Flow

/** Repository that exposes CRUD operations for [Note]s backed by Firestore. */
class NotesRepository(
    private val firestoreSource: FirestoreSource = FirestoreSource()
) {

    /** Observe all notes for the current user. */
    fun getNotes(): Flow<List<Note>> = firestoreSource.getNotes()

    /** Add a new note. */
    suspend fun addNote(note: Note) = firestoreSource.addNote(note)

    /** Update an existing note. */
    suspend fun updateNote(note: Note) = firestoreSource.updateNote(note)

    /** Delete the note with the given id. */
    suspend fun deleteNote(id: String) = firestoreSource.deleteNote(id)
}
