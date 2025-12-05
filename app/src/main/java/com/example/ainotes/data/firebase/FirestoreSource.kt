package com.example.ainotes.data.firebase

import com.example.ainotes.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Simple abstraction over Firestore access for note documents. All notes are
 * stored under the path `users/{uid}/notes` where `uid` is the currently
 * authenticated user's id. The class exposes flows for realtime updates as
 * well as suspend functions for creating, updating and deleting notes.
 */

class FirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** Returns the collection reference for the current user's notes. */
    private fun notesCollection(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("notes")
    }

    /**
     * Observe all notes for the current user. The flow emits whenever the
     * underlying Firestore snapshot changes.
     */
    fun getNotes(): Flow<List<Note>> = callbackFlow {
        val collection = notesCollection()
        if (collection == null) {
            trySend(emptyList())
            // If there is no authenticated user we close the flow with an error
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }

        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val notes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Note::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(notes)
        }

        awaitClose { registration.remove() }
    }

    /** Add a new note document. */
    suspend fun addNote(note: Note) {
        note.rawText = note.content
        note.cleanText = note.content

        notesCollection()
            ?.document(note.id)
            ?.set(note)
            ?.await()
    }

    /** Update an existing note document. */
    suspend fun updateNote(note: Note) {
        note.rawText = note.content
        note.cleanText = note.content

        notesCollection()
            ?.document(note.id)
            ?.set(note)
            ?.await()
    }

    /** Delete the note with the given id. */
    suspend fun deleteNote(id: String) {
        notesCollection()
            ?.document(id)
            ?.delete()
            ?.await()
    }
}