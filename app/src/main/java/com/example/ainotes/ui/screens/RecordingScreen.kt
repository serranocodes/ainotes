// File: RecordingScreen.kt
package com.example.ainotes.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.RecordingViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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

    // Initial permission check (best-effort)
    LaunchedEffect(Unit) {
        hasMicPermission = if (Build.VERSION.SDK_INT >= 23) {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Auto-start once permission is granted
    LaunchedEffect(hasMicPermission) {
        if (hasMicPermission && !viewModel.isRecording.value) {
            viewModel.startRecording(context)
        }
    }

    // Stop when app goes background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stopRecording()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopRecording()
        }
    }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto-scroll the transcript while recording
    LaunchedEffect(recognizedText, isRecording) {
        if (isRecording) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0F13)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ===== TOP: status + waveform =====
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = when {
                        !hasMicPermission -> "Microphone permission required"
                        isRecording -> "Listening…"
                        isTranscribing -> "Transcribing…"
                        else -> "Idle"
                    },
                    fontSize = 22.sp,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                val showOverlay = !isRecording && isTranscribing

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AuroraRibbonWaveform(
                        amplitude = amplitude,
                        active = isRecording
                    )
                    if (showOverlay) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Transcribing…", color = Color.White)
                        }
                    }
                }
            }

            // ===== MIDDLE: transcript scroll box =====
            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                tonalElevation = 3.dp,
                color = Color(0x151FFFFFF)
            ) {
                SelectionContainer {
                    Text(
                        text = if (recognizedText.isNotBlank()) recognizedText
                        else "Start speaking to see your transcript here…",
                        color = Color(0xFFECECEC),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            // ===== BOTTOM: ICON-ONLY actions (Start/Stop • Edit->direct edit • Save • Cancel) =====
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start/Stop
                FilledIconButton(
                    onClick = {
                        if (isRecording) viewModel.stopRecording()
                        else viewModel.startRecording(context)
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) Color(0xFFFF6B6B) else Color(0xFF3A86FF),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Start"
                    )
                }

                // Edit -> navigate directly to edit mode
                OutlinedIconButton(
                    enabled = !isTranscribing && recognizedText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            viewModel.stopRecording()
                            // NOTE: we pass mode=edit so the destination opens in edit mode immediately
                            navController.navigate("transcription?mode=edit")
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }

                // Save (save & go back)
                FilledTonalIconButton(
                    enabled = recognizedText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            viewModel.stopRecording()
                            val text = recognizedText
                            viewModel.saveTranscription(text) { ok ->
                                if (ok) {
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save transcription",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0x3332D74B),
                        contentColor = Color(0xFF32D74B)
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                }

                // Cancel (discard & go back)
                FilledTonalIconButton(
                    onClick = {
                        viewModel.cancelRecording()
                        navController.popBackStack()
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0x33FF6B6B),
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
        }
    }
}

/* ---------- Aurora Ribbon Waveform + helpers ---------- */

@Composable
fun AuroraRibbonWaveform(
    amplitude: Int,
    active: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
) {
    val level = amplitudeToLevel(amplitude, sensitivity = 12000f, active = active)

    val energy by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(100, easing = LinearEasing),
        label = "aurora-energy"
    )

    val envelope by animateFloatAsState(
        targetValue = 0.6f + 0.4f * level,
        animationSpec = tween(600, easing = LinearEasing),
        label = "aurora-env"
    )

    val transition = rememberInfiniteTransition(label = "aurora-phase")
    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "t2"
    )

    Canvas(
        modifier = modifier.background(Color(0xFF0D0F13))
    ) {
        val w = size.width
        val h = size.height
        val mid = h / 2f

        fun yFor(x: Float, baseAmp: Float, phase: Float, f1: Float, f2: Float): Float {
            val p = x / w
            val bell = 0.4f + 0.6f * (1f - abs(p * 2f - 1f))
            val s1 = sin((p * f1 * 2f * PI + phase).toFloat())
            val s2 = sin((p * f2 * 2f * PI + phase * 0.6f + 0.7f).toFloat())
            val sum = 0.65f * s1 + 0.35f * s2
            val maxHeight = h * 0.38f * envelope
            return mid - sum * maxHeight * baseAmp * bell
        }

        val pointsCount = 18
        val xs = (0..pointsCount).map { i -> i / pointsCount.toFloat() * w }

        val primaryPts = xs.map { x ->
            Offset(x, yFor(x, baseAmp = 1f * energy.coerceAtLeast(0.1f), phase = t1, f1 = 2.1f, f2 = 4.2f))
        }
        val primaryPath = catmullRomPath(primaryPts, tension = 0.5f)

        val secondaryPts = xs.map { x ->
            Offset(x, yFor(x, baseAmp = 0.75f * (0.5f + energy/2f), phase = t2, f1 = 1.6f, f2 = 3.3f))
        }
        val secondaryPath = catmullRomPath(secondaryPts, tension = 0.5f)

        val aurora = Brush.horizontalGradient(
            0f to Color(0xFF7DF9FF),
            0.5f to Color(0xFFB9FFE8),
            1f to Color(0xFF7AA8FF)
        )
        val echo = Brush.horizontalGradient(
            0f to Color(0x66B9FFE8),
            1f to Color(0x667AA8FF)
        )

        val mainStroke = (6.dp.toPx() + 10.dp.toPx() * energy).coerceAtMost(14.dp.toPx())
        val echoStroke = (3.dp.toPx() + 6.dp.toPx() * energy).coerceAtMost(9.dp.toPx())

        if (active) {
            drawPath(
                path = primaryPath,
                brush = aurora,
                style = Stroke(width = mainStroke * 1.9f, cap = Stroke.DefaultCap),
                alpha = 0.06f
            )
            drawPath(
                path = primaryPath,
                brush = aurora,
                style = Stroke(width = mainStroke * 1.4f, cap = Stroke.DefaultCap),
                alpha = 0.08f
            )
        }

        drawPath(
            path = secondaryPath,
            brush = echo,
            style = Stroke(width = echoStroke, cap = Stroke.DefaultCap),
            alpha = if (active) 0.9f else 0.4f
        )

        drawPath(
            path = primaryPath,
            brush = aurora,
            style = Stroke(width = mainStroke, cap = Stroke.DefaultCap),
            alpha = if (active) 1f else 0.6f
        )

        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, mid),
            end = Offset(w, mid),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun catmullRomPath(points: List<Offset>, tension: Float = 0.5f): Path {
    val n = points.size
    val path = Path()
    if (n == 0) return path
    path.moveTo(points[0].x, points[0].y)
    if (n == 1) return path

    for (i in 0 until n - 1) {
        val p0 = if (i == 0) points[i] else points[i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < n) points[i + 2] else points[i + 1]

        val t = tension / 6f
        val cp1 = p1 + (p2 - p0) * t
        val cp2 = p2 - (p3 - p1) * t

        path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
    }
    return path
}

/* vector helpers */
private operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
private operator fun Offset.times(s: Float) = Offset(x * s, y * s)

/* amplitude mapping */
@Composable
private fun amplitudeToLevel(amplitude: Int, sensitivity: Float, active: Boolean): Float {
    val lvl = (amplitude / sensitivity).coerceIn(0f, 1f)
    return if (active) lvl else 0f
}
