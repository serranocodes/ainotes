package com.example.ainotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ainotes.data.OnboardingPreferences
import com.example.ainotes.ui.AppNavigation
import com.example.ainotes.ui.theme.AinotesTheme
import com.example.ainotes.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AinotesTheme {
                // Create ONE shared instance of AuthViewModel
                val authViewModel: AuthViewModel = viewModel()

                val completed by OnboardingPreferences
                    .isOnboardingCompleted(applicationContext)
                    .collectAsState(initial = false)

                AppNavigation(
                    startWithOnboarding = !completed,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
