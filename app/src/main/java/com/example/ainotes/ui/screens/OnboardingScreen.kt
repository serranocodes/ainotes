// File: OnboardingScreen.kt
package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.data.OnboardingPreferences
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableIntStateOf(0) }
    val pages = listOf(
        "Note-Taking Made Simple",
        "Voice Your Ideas",
        "Stay Synced Across Devices",
        "Get Started Today!"
    )

    // Palette (matches the rest of the app)
    val bg = Color(0xFF0D0F13)
    val onBg = Color(0xFFECEDEF)
    val sub = Color(0xFF9AA4B2)

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
                    .navigationBarsPadding(), // keep button off the gesture bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f, fill = true))

                // Title
                Text(
                    text = pages[currentPage],
                    color = onBg,
                    fontSize = 26.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (currentPage) {
                        0 -> "Capture thoughts instantly and keep them organized."
                        1 -> "Hands-free dictation with clean, readable transcripts."
                        2 -> "Your notes, available on all your devices."
                        else -> "Ready when you are."
                    },
                    color = sub,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(28.dp))

                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .fillMaxWidth(fraction = 0f) // ignore width; we size by height below
                                .background(
                                    color = if (i == currentPage) onBg else onBg.copy(alpha = 0.28f),
                                    shape = CircleShape
                                )
                                .let {
                                    // make them circles by explicit size
                                    it.then(Modifier.height(10.dp))
                                }
                                .then(Modifier
                                    .background(
                                        color = if (i == currentPage) onBg else onBg.copy(alpha = 0.28f),
                                        shape = CircleShape
                                    )
                                )
                                .then(Modifier
                                    .padding(horizontal = 0.dp)
                                ),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Continue / Get Started
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            scope.launch {
                                OnboardingPreferences.setOnboardingCompleted(context)
                                onComplete()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = onBg),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (currentPage < pages.lastIndex) "Continue" else "Get Started",
                        color = Color(0xFF1E3A8A)
                    )
                }

                Spacer(Modifier.weight(1f, fill = true))
            }
        }
    }
}
