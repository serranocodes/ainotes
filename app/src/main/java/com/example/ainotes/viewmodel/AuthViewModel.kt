package com.example.ainotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // Firestore instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun resetInputFields() {
        _uiState.value = _uiState.value.copy(
            email = "",
            password = "",
            errorMessage = null,
            infoMessage = null
        )
    }

    // Update email and password fields as the user types
    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    // Sign in with email/password
    fun signIn() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password
            val result = authRepo.signInUser(email, password)
            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = null,
                        infoMessage = null
                    )
                } else {
                    authRepo.signOut()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = null,
                        errorMessage = "Email not verified. Please check your inbox.",
                        infoMessage = null
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage,
                    infoMessage = null
                )
            }
        }
    }

    // Sign up with email/password and save user data in Firestore
    fun signUp(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password
            val result = authRepo.signUpUser(email, password)
            result.onSuccess { user ->
                // Save user data to Firestore
                val uid = user.uid
                val userData = mapOf(
                    "email" to email,
                    "name" to name,
                    "createdAt" to System.currentTimeMillis(),
                )

                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        viewModelScope.launch {
                            try {
                                user.sendEmailVerification().await()
                                authRepo.signOut()
                                _uiState.value = AuthUiState(
                                    infoMessage = "Verification email sent. Please verify before logging in."
                                )
                                onSuccess()
                            } catch (e: Exception) {
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onError("Failed to send verification email: ${e.message}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onError("Failed to save user to Firestore: ${e.message}")
                    }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage,
                    infoMessage = null
                )
                onError(e.localizedMessage ?: "An unknown error occurred.")
            }
        }
    }

    // Sign out
    fun signOut() {
        authRepo.signOut()
        _uiState.value = AuthUiState()
    }

    // Update error message in UI state
    fun updateError(error: String) {
        _uiState.value = _uiState.value.copy(errorMessage = error)
    }
}
