package com.example.ainotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.ainotes.data.OnboardingPreferences
import com.example.ainotes.ui.AppNavigation
import com.example.ainotes.ui.theme.AinotesTheme
import com.example.ainotes.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Variable to hold the onboarding flag.
        // (Note: This is not reactive. In a real app consider using a state holder in a composable.)
        var startWithOnboarding = true

        // Launch a coroutine to collect the onboarding completion status.
        lifecycleScope.launch {
            OnboardingPreferences.isOnboardingCompleted(applicationContext).collect { completed ->
                // If onboarding is completed, we don't need to start with onboarding.
                startWithOnboarding = !completed
            }
        }

        setContent {
            AinotesTheme {
                // Obtain AuthViewModel via Hilt's hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                // Pass the onboarding flag and the AuthViewModel to AppNavigation.
                AppNavigation(
                    startWithOnboarding = startWithOnboarding,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
