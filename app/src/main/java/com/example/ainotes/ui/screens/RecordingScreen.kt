// File: RecordingScreen.kt
package com.example.ainotes.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.RecordingViewModel
import kotlinx.coroutines.launch

@Composable
fun RecordingScreen(
    navController: NavController,
    viewModel: RecordingViewModel
) {
    val context = LocalContext.current
    val amplitude by viewModel.amplitude.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()

    // 1) Create the coroutine scope *here*, inside the composable body
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.startRecording(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Recording...", fontSize = 24.sp, color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            AudioWaveform(
                amplitude = amplitude,
                isRecording = isRecording,
                isTranscribing = isTranscribing
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // 2) Use the scope *here* in the onClick
                    scope.launch {
                        viewModel.stopRecording()
                        navController.navigate("transcription")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("STOP", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    viewModel.cancelRecording()
                    navController.popBackStack()
                }
            ) {
                Text("CANCEL", color = Color.Red)
            }
        }
    }
}

/**
 * Same waveform code as you have, but amplitude now comes from speech input's onRmsChanged.
 */
@Composable
fun AudioWaveform(amplitude: Int, isRecording: Boolean, isTranscribing: Boolean) {
    val transition = rememberInfiniteTransition()

    val scaleFactor = 25f   // tweak as needed for your style
    val minHeight = 5f
    val maxHeight = 150f

    val animatedAmplitude by transition.animateFloat(
        initialValue = minHeight,
        targetValue = (amplitude / scaleFactor).coerceIn(minHeight, maxHeight),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFF1E1E1E))
    ) {
        val barCount = 30
        val barWidth = size.width / barCount

        for (i in 0 until barCount) {
            drawLine(
                color = if (isRecording || isTranscribing) Color.Yellow else Color.Gray,
                start = androidx.compose.ui.geometry.Offset(
                    x = i * barWidth,
                    y = size.height / 2 - animatedAmplitude
                ),
                end = androidx.compose.ui.geometry.Offset(
                    x = i * barWidth,
                    y = size.height / 2 + animatedAmplitude
                ),
                strokeWidth = 6f
            )
        }
    }
}
