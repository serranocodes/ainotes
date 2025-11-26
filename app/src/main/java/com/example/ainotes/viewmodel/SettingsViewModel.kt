
package com.example.ainotes.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _appVersion = MutableStateFlow("Loading...")
    val appVersion: StateFlow<String> = _appVersion

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages

    init {
        // Listen for authentication state changes.
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                loadUserData() // Reload data when a user is available.
            } else {
                _userData.value = null // Clear data when there's no user.
            }
        }
        // Optionally, if a user is already signed in, load data immediately.
        if (auth.currentUser != null) {
            loadUserData()
        }
        loadAppVersion()
        loadAvailableLanguages()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val preferences = document.get("preferences") as? Map<String, Any>
                    if (preferences != null) {
                        _userData.value = UserData(
                            name = document.getString("name") ?: "Unknown Name",
                            email = document.getString("email") ?: "Unknown Email",
                            transcriptionLanguage = preferences["transcriptionLanguage"] as? String ?: "English",
                            autoDeleteNotes = preferences["autoDeleteNotes"] as? Boolean ?: true,
                            categoryDetection = preferences["categoryDetection"] as? Boolean ?: true,
                            smartSummaries = preferences["smartSummaries"] as? Boolean ?: true,
                            transcriptionEnabled = preferences["transcriptionEnabled"] as? Boolean ?: true,
                            autoTitleSummary = preferences["autoTitleSummary"] as? Boolean ?: false,
                            autoNoteSummary = preferences["autoNoteSummary"] as? Boolean ?: false
                        )
                    } else {
                        // Document exists but no preferences field – create defaults
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
                            .addOnSuccessListener {
                                Log.d("Firestore", "Default preferences stored in Firestore")
                            }
                            .addOnFailureListener {
                                Log.e("Firestore", "Error storing default preferences", it)
                            }
                        _userData.value = UserData(
                            name = document.getString("name") ?: "Unknown Name",
                            email = document.getString("email") ?: "Unknown Email",
                            transcriptionLanguage = "English",
                            autoDeleteNotes = true,
                            categoryDetection = true,
                            smartSummaries = true,
                            transcriptionEnabled = true,
                            autoTitleSummary = false,
                            autoNoteSummary = false
                        )
                    }
                } else {
                    // Document does not exist – create it with default preferences
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
                    ).addOnSuccessListener {
                        Log.d("Firestore", "New user document created")
                    }.addOnFailureListener {
                        Log.e("Firestore", "Error creating user document", it)
                    }
                    _userData.value = newUserData
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching user data", exception)
            }
    }

    private fun loadAppVersion() {
        firestore.collection("appData").document("VersionInfo").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val version = document.getString("appVersion") ?: "Unknown Version"
                    _appVersion.value = version
                } else {
                    _appVersion.value = "No Version Found"
                }
            }
            .addOnFailureListener { exception ->
                _appVersion.value = "Error Fetching Version"
                Log.e("Firestore", "Error fetching version", exception)
            }
    }

    fun updatePreference(key: String, value: Any) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("preferences.$key", value)
            .addOnSuccessListener {
                Log.d("Firestore", "Preference $key updated")
                // 🔥 Update local state
                _userData.value = when (key) {
                    "autoDeleteNotes" -> _userData.value?.copy(autoDeleteNotes = value as Boolean)
                    "categoryDetection" -> _userData.value?.copy(categoryDetection = value as Boolean)
                    "smartSummaries" -> _userData.value?.copy(smartSummaries = value as Boolean)
                    "transcriptionEnabled" -> _userData.value?.copy(transcriptionEnabled = value as Boolean)
                    "autoTitleSummary" -> _userData.value?.copy(autoTitleSummary = value as Boolean)
                    "autoNoteSummary" -> _userData.value?.copy(autoNoteSummary = value as Boolean)
                    else -> _userData.value
                }
            }
            .addOnFailureListener { Log.e("Firestore", "Error updating preference", it) }
    }

    fun clearUserData() {
        _userData.value = null
        _availableLanguages.value = emptyList()
    }

    private fun loadAvailableLanguages() {
        firestore.collection("appData").document("settings").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val languages = document.get("availableLanguages") as? List<String> ?: emptyList()
                    _availableLanguages.value = languages
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching available languages", exception)
            }
    }

    fun updateTranscriptionLanguage(selectedLanguage: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("preferences.transcriptionLanguage", selectedLanguage)
            .addOnSuccessListener {
                Log.d("Firestore", "Transcription language updated to $selectedLanguage")
                _userData.value = _userData.value?.copy(transcriptionLanguage = selectedLanguage)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error updating transcription language", exception)
            }
    }
}

data class UserData(
    val name: String = "Loading...",
    val email: String = "Loading...",
    val transcriptionLanguage: String = "English",
    val autoDeleteNotes: Boolean = true,
    val categoryDetection: Boolean = true,
    val smartSummaries: Boolean = true,
    val transcriptionEnabled: Boolean = true,
    val autoTitleSummary: Boolean = false,
    val autoNoteSummary: Boolean = false,
)