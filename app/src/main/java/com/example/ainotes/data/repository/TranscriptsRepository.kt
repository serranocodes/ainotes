package com.example.ainotes.data.repository

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
                    close(error)
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
        collection.add(transcript).await()
    }

    suspend fun updateTranscript(transcript: Transcript) {
        collection.document(transcript.id).set(transcript).await()
    }

    suspend fun deleteTranscript(id: String) {
        collection.document(id).delete().await()
    }
}
