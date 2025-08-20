// File: RecordingScreen.kt
package com.example.ainotes.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.RecordingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun RecordingScreen(
    navController: NavController,
    viewModel: RecordingViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // VM state
    val amplitude by viewModel.amplitude.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()

    // Permission
    var hasMicPermission by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // Compute current permission once (cheap best-effort; real source of truth is launcher result)
    LaunchedEffect(Unit) {
        val pm = context.packageManager
        hasMicPermission = if (Build.VERSION.SDK_INT >= 23) {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Start/stop with lifecycle (only when we have permission)
    DisposableEffect(lifecycleOwner, hasMicPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (hasMicPermission) viewModel.startRecording(context)
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.stopRecording()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Defensive stop in case we leave the screen
            viewModel.stopRecording()
        }
    }

    // Overlay show/hide with a small grace period to avoid flicker
    var showTranscribingOverlay by remember { mutableStateOf(false) }
    LaunchedEffect(isTranscribing) {
        if (isTranscribing) {
            showTranscribingOverlay = true
        } else {
            delay(400) // brief grace so overlay doesn’t flicker between restarts
            showTranscribingOverlay = false
        }
    }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when {
                    !hasMicPermission -> "Microphone permission required"
                    isRecording -> "Listening…"
                    isTranscribing -> "Transcribing…"
                    else -> "Idle"
                },
                fontSize = 22.sp,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showTranscribingOverlay, label = "overlay") { show ->
                    if (show) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Transcribing…", color = Color.White)
                        }
                    } else {
                        AudioWaveform(
                            amplitude = amplitude,
                            active = isRecording || isTranscribing
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Live text preview (nice UX while dictating)
            if (recognizedText.isNotBlank()) {
                Text(
                    recognizedText,
                    color = Color(0xFFEEEEEE),
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            if (!hasMicPermission) {
                Button(
                    onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF))
                ) {
                    Text("Grant microphone access", color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("CANCEL", color = Color(0xFFFF6B6B))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle button
                    Button(
                        onClick = {
                            if (isRecording) viewModel.stopRecording()
                            else viewModel.startRecording(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFFF6B6B) else Color(0xFF3A86FF)
                        )
                    ) {
                        Text(if (isRecording) "Stop" else "Start", color = Color.White)
                    }

                    // View transcription (only when recognizer is idle so we don’t leak listeners)
                    OutlinedButton(
                        enabled = !isRecording && !isTranscribing,
                        onClick = {
                            scope.launch {
                                viewModel.stopRecording()
                                navController.navigate("transcription")
                            }
                        }
                    ) {
                        Text("View transcription", color = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        viewModel.cancelRecording()
                        navController.popBackStack()
                    }
                ) {
                    Text("CANCEL", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun AudioWaveform(
    amplitude: Int,
    active: Boolean
) {
    // Smooth the raw amplitude into the canvas with a quick tween.
    val scale = 25f
    val minH = 4f
    val maxH = 140f
    val target = (amplitude / scale).coerceIn(minH, maxH)

    val animated by animateFloatAsState(
        targetValue = if (active) target else minH,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "amp"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFF1E1E1E))
    ) {
        val barCount = 40
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount

        repeat(barCount) { i ->
            val x = i * (barWidth + gap)
            // Simple falloff across bars for a nicer look
            val falloff = 1f - (i / (barCount - 1f))
            val h = max(minH, min(maxH, animated * (0.5f + 0.5f * falloff)))

            drawLine(
                color = if (active) Color(0xFFFFD166) else Color.Gray,
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2 - h),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2 + h),
                strokeWidth = barWidth
            )
        }
    }
}
