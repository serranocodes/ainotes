package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ainotes.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SignUpOptionsScreen(navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.ainotes.R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                authViewModel.googleSignIn(idToken, onSuccess = {
                    navController.navigate("main") {
                        popUpTo("sign_up_options") { inclusive = true }
                    }
                }, onError = { authViewModel.updateError(it) })
            }
        } catch (e: ApiException) {
            authViewModel.updateError(e.localizedMessage ?: "Google sign-in failed")
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF), Color(0xFF2563EB))
                )
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Text
            Text(
                text = "Start Now",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Create an Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Button: Sign Up with Google
            Button(
                onClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Sign up with Google", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
            }

            // Button: Sign Up with Other Email
            Button(
                onClick = {
                    navController.navigate("email_sign_up") // Navigate to email sign-up screen
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Sign Up with email", fontWeight = FontWeight.Bold)
            }
        }
    }
}
