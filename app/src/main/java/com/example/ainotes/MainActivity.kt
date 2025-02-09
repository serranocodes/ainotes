package com.example.ainotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ainotes.data.OnboardingPreferences
import com.example.ainotes.ui.AppNavigation
import com.example.ainotes.ui.theme.AinotesTheme
import com.example.ainotes.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var startWithOnboarding = true

        // Check if onboarding is completed using DataStore
        lifecycleScope.launch {
            OnboardingPreferences.isOnboardingCompleted(applicationContext).collect { completed ->
                startWithOnboarding = !completed
            }
        }

        setContent {
            AinotesTheme {
                // Create ONE shared instance of AuthViewModel
                val authViewModel: AuthViewModel = viewModel()

                // Pass authViewModel to AppNavigation
                AppNavigation(
                    startWithOnboarding = startWithOnboarding,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
