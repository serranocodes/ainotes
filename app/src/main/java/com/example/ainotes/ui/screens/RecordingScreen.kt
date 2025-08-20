package com.example.ainotes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ainotes.viewmodel.RecordingViewModel

@Composable
fun RecordingScreen(
    navController: NavController,
    viewModel: RecordingViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val amplitude by viewModel.amplitude.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startRecording(context)
        }
    }

    val currentHasPermission by rememberUpdatedState(hasPermission)
    val currentPermissionLauncher by rememberUpdatedState(permissionLauncher)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (currentHasPermission) {
                        viewModel.startRecording(context)
                    } else {
                        currentPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
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
        }
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude.toFloat(),
        label = "amplitude"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = isTranscribing, label = "transcribing") { transcribing ->
                    if (transcribing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Transcribing…", color = Color.White)
                        }
                    } else {
                        AudioWaveform(
                            amplitude = animatedAmplitude,
                            isActive = isRecording || isTranscribing,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (recognizedText.isNotEmpty()) {
                Text(
                    text = recognizedText,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        if (hasPermission) {
                            viewModel.startRecording(context)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            ) {
                Text(if (isRecording) "Stop" else "Start")
            }

            if (!isRecording && recognizedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("transcription") }) {
                    Text("View transcription")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    viewModel.cancelRecording()
                    navController.popBackStack()
                }
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun AudioWaveform(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
    ) {
        val barCount = 30
        val barWidth = size.width / barCount
        val normalized = (amplitude / 32767f) * (size.height / 2f)
        for (i in 0 until barCount) {
            val x = i * barWidth + barWidth / 2
            drawLine(
                color = if (isActive) Color.Yellow else Color.Gray,
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2 - normalized),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2 + normalized),
                strokeWidth = barWidth * 0.8f
            )
        }
    }
}

