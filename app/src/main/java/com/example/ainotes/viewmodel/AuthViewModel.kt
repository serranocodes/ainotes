package com.example.ainotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,       // Injected AuthRepository
    private val firestore: FirebaseFirestore      // Provided via FirebaseModule
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun resetInputFields() {
        _uiState.value = _uiState.value.copy(email = "", password = "", errorMessage = null)
    }

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun signIn() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password
            val result = authRepo.signInUser(email, password)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    errorMessage = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun signUp(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password
            val result = authRepo.signUpUser(email, password)
            result.onSuccess { user ->
                val uid = user.uid
                val userData = mapOf(
                    "email" to email,
                    "name" to name,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = user,
                            errorMessage = null
                        )
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onError("Failed to save user to Firestore: ${e.message}")
                    }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage
                )
                onError(e.localizedMessage ?: "An unknown error occurred.")
            }
        }
    }

    fun signOut() {
        authRepo.signOut()
        _uiState.value = AuthUiState()
    }

    fun updateError(error: String) {
        _uiState.value = _uiState.value.copy(errorMessage = error)
    }
}
