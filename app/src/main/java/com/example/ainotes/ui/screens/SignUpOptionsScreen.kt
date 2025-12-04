// File: SignUpOptionsScreen.kt
package com.example.ainotes.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun SignUpOptionsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()

    // Navigate when the user becomes available
    LaunchedEffect(uiState.user) {
        if (uiState.user != null) {
            navController.navigate("main") {
                popUpTo("sign_up_options") { inclusive = true }
            }
        }
    }

    val googleSignInClient = remember(context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.ainotes.R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                authViewModel.googleSignIn(idToken)
            } else {
                authViewModel.updateError("Google sign-in failed")
            }
        } catch (e: ApiException) {
            authViewModel.updateError(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    // App palette (consistent with other screens)
    val bg = Color(0xFF0D0F13)
    val onBg = Color(0xFFECEDEF)
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
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(), // sits nicely above the nav bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Use symmetric weights to keep content comfortably centered
                Spacer(Modifier.weight(1f, fill = true))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header
                    Text(
                        text = "Start Now",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBg
                    )
                    Text(
                        text = "Create an Account",
                        fontSize = 18.sp,
                        color = onBg.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Sign up with Google
                    Button(
                        onClick = { launcher.launch(googleSignInClient.signInIntent) },
                        colors = ButtonDefaults.buttonColors(containerColor = onBg),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Sign up with Google", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                    }

                    // Sign up with Email
                    Button(
                        onClick = { navController.navigate("email_sign_up") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Sign up with email", fontWeight = FontWeight.Bold)
                    }

                    // Error (if any)
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.weight(1f, fill = true))
            }
        }
    }
}
