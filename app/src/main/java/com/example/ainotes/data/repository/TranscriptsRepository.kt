package com.example.ainotes.data.repository

import android.util.Log
import com.example.ainotes.data.model.Transcript
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles persistence of transcripts in Firebase Firestore.
 */
class TranscriptsRepository {
    private val collection = Firebase.firestore.collection("transcripts")

    /**
     * Stream the list of transcripts, ordered by timestamp descending.
     */
    fun getTranscripts(): Flow<List<Transcript>> = callbackFlow {
        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TranscriptsRepository", "Listen failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val data = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transcript::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(data)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTranscript(text: String) {
        val transcript = Transcript(text = text)
        try {
            collection.add(transcript).await()
        } catch (e: Exception) {
            Log.e("TranscriptsRepository", "Failed to add transcript", e)
        }
    }

    suspend fun updateTranscript(transcript: Transcript) {
        try {
            collection.document(transcript.id).set(transcript).await()
        } catch (e: Exception) {
            Log.e("TranscriptsRepository", "Failed to update transcript", e)
        }
    }

    suspend fun deleteTranscript(id: String) {
        try {
            collection.document(id).delete().await()
        } catch (e: Exception) {
            Log.e("TranscriptsRepository", "Failed to delete transcript", e)
        }
    }
}
