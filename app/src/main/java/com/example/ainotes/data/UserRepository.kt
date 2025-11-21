package com.example.ainotes.data

import com.example.ainotes.viewmodel.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun getUserData(): Flow<UserData?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(null)
            close(Exception("No user logged in"))
            return@callbackFlow
        }
        val userRef = firestore.collection("users").document(userId)
        val listener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val preferences = snapshot.get("preferences") as? Map<String, Any>
                val data = if (preferences != null) {
                    UserData(
                        name = snapshot.getString("name") ?: "Unknown Name",
                        email = snapshot.getString("email") ?: "Unknown Email",
                        transcriptionLanguage = preferences["transcriptionLanguage"] as? String ?: "English",
                        autoDeleteNotes = preferences["autoDeleteNotes"] as? Boolean ?: true,
                        categoryDetection = preferences["categoryDetection"] as? Boolean ?: true,
                        smartSummaries = preferences["smartSummaries"] as? Boolean ?: true,
                        transcriptionEnabled = preferences["transcriptionEnabled"] as? Boolean ?: true,
                        autoTitleSummary = preferences["autoTitleSummary"] as? Boolean ?: false,
                        autoNoteSummary = preferences["autoNoteSummary"] as? Boolean ?: false
                    )
                } else {
                    // If preferences don't exist, create default preferences.
                    val defaultPreferences = mapOf(
                        "transcriptionLanguage" to "English",
                        "autoDeleteNotes" to true,
                        "categoryDetection" to true,
                        "smartSummaries" to true,
                        "transcriptionEnabled" to true,
                        "autoTitleSummary" to false,
                        "autoNoteSummary" to false
                    )
                    userRef.update("preferences", defaultPreferences)
                    UserData(
                        name = snapshot.getString("name") ?: "Unknown Name",
                        email = snapshot.getString("email") ?: "Unknown Email",
                        transcriptionLanguage = "English",
                        autoDeleteNotes = true,
                        categoryDetection = true,
                        smartSummaries = true,
                        transcriptionEnabled = true,
                        autoTitleSummary = false,
                        autoNoteSummary = false
                    )
                }
                trySend(data)
            } else {
                // Document does not exist—create it with default values.
                val defaultPreferences = mapOf(
                    "transcriptionLanguage" to "English",
                    "autoDeleteNotes" to true,
                    "categoryDetection" to true,
                    "smartSummaries" to true,
                    "transcriptionEnabled" to true,
                    "autoTitleSummary" to false,
                    "autoNoteSummary" to false
                )
                val newUserData = UserData(
                    name = "Unknown Name",
                    email = "Unknown Email",
                    transcriptionLanguage = "English",
                    autoDeleteNotes = true,
                    categoryDetection = true,
                    smartSummaries = true,
                    transcriptionEnabled = true,
                    autoTitleSummary = false,
                    autoNoteSummary = false
                )
                userRef.set(
                    mapOf(
                        "name" to newUserData.name,
                        "email" to newUserData.email,
                        "preferences" to defaultPreferences
                    )
                )
                trySend(newUserData)
            }
        }
        awaitClose { listener.remove() }
    }
}
