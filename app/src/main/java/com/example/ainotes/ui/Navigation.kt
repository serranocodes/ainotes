package com.example.ainotes.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ainotes.ui.screens.*
import com.example.ainotes.viewmodel.AuthViewModel
import com.example.ainotes.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(startWithOnboarding: Boolean, authViewModel: AuthViewModel) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (startWithOnboarding) "onboarding" else "login"

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
                    navController.navigate("main") { // Navigate to MainScreen on successful login
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
                    navController.navigate("main") { // Navigate to MainScreen on successful sign-up
                        popUpTo("email_sign_up") { inclusive = true }
                    }
                }
            )
        }

        // Main Screen
        composable("main") {
            MainScreen(navController = navController)
        }

        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel() // Provide the ViewModel

            SettingsScreen(
                onBackPressed = { navController.popBackStack() },
                onLogoutClicked = {
                    authViewModel.signOut() // Clear user session
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true } // Clear backstack to ensure logout
                    }
                },
                viewModel = settingsViewModel // Pass ViewModel to SettingsScreen
            )
        }
    }
}
