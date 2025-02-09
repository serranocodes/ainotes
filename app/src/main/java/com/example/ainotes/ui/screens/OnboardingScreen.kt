package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.ainotes.data.OnboardingPreferences
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track the current onboarding screen
    var currentPage by remember { mutableStateOf(0) }

    // List of onboarding screens
    val onboardingScreens = listOf(
        "Note-Taking Made Simple",
        "Organize Your Ideas Effortlessly",
        "Stay Synced Across Devices",
        "Get Started Today!"
    )

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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Onboarding Text
            Text(
                text = onboardingScreens[currentPage],
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Progress Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                onboardingScreens.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(4.dp)
                            .background(
                                color = if (index == currentPage) Color.White else Color.Gray,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Continue Button
            Button(
                onClick = {
                    if (currentPage < onboardingScreens.lastIndex) {
                        // Move to the next screen
                        currentPage++
                    } else {
                        // Save onboarding completion and navigate to login
                        coroutineScope.launch {
                            OnboardingPreferences.setOnboardingCompleted(context)
                            onComplete()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (currentPage < onboardingScreens.lastIndex) "Continue" else "Get Started",
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
