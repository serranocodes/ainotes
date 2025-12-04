// File: EmailSignUpScreen.kt
package com.example.ainotes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.viewmodel.AuthViewModel

@Composable
fun EmailSignUpScreen(
    viewModel: AuthViewModel,
    onAlreadyUserClicked: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    // App palette (matches login & main)
    val bg = Color(0xFF0D0F13)
    val fieldBg = Color(0xFF141922)
    val onBg = Color(0xFFECEDEF)
    val sub = Color(0xFF9AA4B2)
    val accent = Color(0xFF3A86FF)

    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { inner ->
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Top breathing room
                Spacer(Modifier.height(28.dp))

                // Header
                Text(
                    text = "Create Your Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )

                // Name
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = sub) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        disabledContainerColor = fieldBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg,
                        cursorColor = onBg,
                        focusedLabelColor = sub,
                        unfocusedLabelColor = sub
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Email
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email", color = sub) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        disabledContainerColor = fieldBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg,
                        cursorColor = onBg,
                        focusedLabelColor = sub,
                        unfocusedLabelColor = sub
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password", color = sub) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null, tint = onBg)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        disabledContainerColor = fieldBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = onBg,
                        unfocusedTextColor = onBg,
                        cursorColor = onBg,
                        focusedLabelColor = sub,
                        unfocusedLabelColor = sub
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Messages
                uiState.infoMessage?.let { msg ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = Color(0xFF32D74B),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                uiState.errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Minimum gap before actions so the button isn't glued to password
                Spacer(Modifier.height(16.dp))

                // Flexible space (centers form when keyboard hidden)
                Spacer(Modifier.weight(1f, fill = true))

                // Bottom actions (rise with keyboard, minimal bottom padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = onBg)
                    } else {
                        Button(
                            onClick = {
                                if (name.isBlank() || uiState.email.isBlank() || uiState.password.isBlank()) {
                                    viewModel.updateError("All fields are required.")
                                } else {
                                    viewModel.signUp(
                                        name = name,
                                        onSuccess = onSignUpSuccess,
                                        onError = { error -> viewModel.updateError(error) }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = onBg),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Sign Up", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClickableText(
                            text = AnnotatedString(
                                "Already have an account?  Login",
                                spanStyle = SpanStyle(color = onBg, fontSize = 16.sp)
                            ),
                            onClick = { onAlreadyUserClicked() }
                        )
                    }
                }
            }
        }
    }
}
