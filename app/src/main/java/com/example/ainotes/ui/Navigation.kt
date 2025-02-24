package com.example.ainotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ainotes.ui.screens.*
import com.example.ainotes.viewmodel.AuthViewModel
import com.example.ainotes.viewmodel.MainViewModel
import com.example.ainotes.viewmodel.RecordingViewModel
import com.example.ainotes.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(startWithOnboarding: Boolean, authViewModel: AuthViewModel) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (startWithOnboarding) "onboarding" else "login"

    // Provide ViewModels at the NavHost level to persist across screens
    val mainViewModel: MainViewModel = viewModel()
    val recordingViewModel: RecordingViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        // Onboarding Screen
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("sign_up_options") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Sign-Up Options Screen
        composable("sign_up_options") {
            SignUpOptionsScreen(navController)
        }

        // Login Screen
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNeedAccountClicked = { navController.navigate("email_sign_up") },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Email Sign-Up Screen
        composable("email_sign_up") {
            EmailSignUpScreen(
                viewModel = authViewModel,
                onAlreadyUserClicked = { navController.navigate("login") },
                onSignUpSuccess = {
                    navController.navigate("main") {
                        popUpTo("email_sign_up") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                navController = navController,
                recordingViewModel = recordingViewModel
            )
        }

        // Recording Screen
        composable("recording") {
            RecordingScreen(
                navController = navController,
                viewModel = recordingViewModel
            )
        }

        composable("transcription") {
            TranscriptionScreen(navController, recordingViewModel)
        }


        // Settings Screen
        composable("settings") {
            SettingsScreen(
                onBackPressed = { navController.popBackStack() },
                onLogoutClicked = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                viewModel = settingsViewModel // Inject SettingsViewModel
            )
        }
    }
}
