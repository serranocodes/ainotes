package com.example.ainotes.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData

    private val _appVersion = MutableStateFlow("Loading...")
    val appVersion: StateFlow<String> = _appVersion

    init {
        loadUserData()
        loadAppVersion()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val preferences = document.get("preferences") as? Map<String, Any> ?: emptyMap()
                    _userData.value = UserData(
                        name = document.getString("name") ?: "Unknown Name",
                        email = document.getString("email") ?: "Unknown Email",
                        transcriptionLanguage = preferences["transcriptionLanguage"] as? String ?: "English",
                        autoDeleteNotes = preferences["autoDeleteNotes"] as? Boolean ?: false,
                        categoryDetection = preferences["categoryDetection"] as? Boolean ?: true,
                        smartSummaries = preferences["smartSummaries"] as? Boolean ?: true,
                        transcriptionEnabled = preferences["transcriptionEnabled"] as? Boolean ?: true,
                        theme = preferences["theme"] as? String ?: "light"
                    )
                }
            }
    }

    // ✅ FIX: Added missing function to fetch app version from Firestore
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
            .addOnSuccessListener { Log.d("Firestore", "Preference $key updated") }
            .addOnFailureListener { Log.e("Firestore", "Error updating preference", it) }
    }
}

data class UserData(
    val name: String = "Loading...",
    val email: String = "Loading...",
    val transcriptionLanguage: String = "English",
    val autoDeleteNotes: Boolean = false,
    val categoryDetection: Boolean = true,
    val smartSummaries: Boolean = true,
    val transcriptionEnabled: Boolean = true,
    val theme: String = "light"
)
