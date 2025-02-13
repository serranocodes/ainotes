package com.example.ainotes.ui.screens

import android.os.Environment
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.RecordingViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RecordingScreen(
    navController: NavController,
    viewModel: RecordingViewModel
) {
    val context = LocalContext.current

    // Observe state from the ViewModel
    val amplitude by viewModel.amplitude.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    // Automatically start recording when we land on this screen
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

            // 🎵 Animated Waveform
            AudioWaveform(amplitude = amplitude, isRecording = isRecording)

            Spacer(modifier = Modifier.height(32.dp))

            // Stop Button
            Button(
                onClick = {
                    // Launch a coroutine so we can await stopRecording() completion
                    CoroutineScope(Dispatchers.Main).launch {
                        viewModel.stopRecording()

                        // Use the file created in the view model
                        val internalFile = viewModel.getOutputFile()
                        if (internalFile != null && internalFile.exists()) {
                            val externalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), internalFile.name)
                            try {
                                internalFile.copyTo(externalFile, overwrite = true)
                                Log.d("Test", "Copied to external: ${externalFile.absolutePath}")
                            } catch (e: Exception) {
                                Log.e("Test", "Copy failed", e)
                            }
                        } else {
                            Log.e("Test", "Internal file is null or does not exist")
                        }

                        navController.popBackStack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("STOP", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Button
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

@Composable
fun AudioWaveform(amplitude: Int, isRecording: Boolean) {
    val transition = rememberInfiniteTransition()

    // Lower scaleFactor => bigger bars for the same amplitude
    val scaleFactor = 25f
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
                color = if (isRecording) Color.Yellow else Color.Gray,
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
