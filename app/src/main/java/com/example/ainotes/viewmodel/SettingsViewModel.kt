package com.example.ainotes.viewmodel

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

    // Mutable state to hold user data
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _userData.value = UserData(
                        name = document.getString("name") ?: "Unknown Name",
                        email = document.getString("email") ?: "Unknown Email",
                        translationLanguage = document.getString("translationLanguage") ?: "English"
                    )
                }
            }
    }

    fun updateUserData(name: String, email: String, translationLanguage: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "name" to name,
            "email" to email,
            "translationLanguage" to translationLanguage
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                _userData.value = UserData(name, email, translationLanguage)
            }
    }
}

data class UserData(
    val name: String = "Loading...",
    val email: String = "Loading...",
    val translationLanguage: String = "Loading..."
)
