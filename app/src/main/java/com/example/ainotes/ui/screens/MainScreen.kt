package com.example.ainotes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.NotesViewModel
import com.example.ainotes.viewmodel.RecordingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size

@Composable
fun MainScreen(
    navController: NavController,
    recordingViewModel: RecordingViewModel,
    notesViewModel: NotesViewModel
) {
    val context = LocalContext.current
    var hasAudioPermission by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            navigateToRecordingIfNotBusy(navController, recordingViewModel, notesViewModel)
        } else {
            Log.e("MainScreen", "Microphone permission denied!")
        }
    }

    LaunchedEffect(Unit) {
        hasAudioPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(Unit) { notesViewModel.startCollectingNotes() }

    // Palette (matches your dark recording screen)
    val bg = Color(0xFF0D0F13)
    val card = Color(0xFF141922)
    val onCard = Color(0xFFECEDEF)
    val subText = Color(0xFF9AA4B2)
    val hairline = Color(0x22FFFFFF)

    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets.safeDrawing // status & nav bars handled here
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner) // respect system bars once, globally
        ) {
            if (notesViewModel.notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Welcome to NotesApp.\nTap record to create your first note.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onCard.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 96.dp), // space for the record button
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp) // keeps first card off the very top
                ) {
                    items(notesViewModel.notes.sortedByDescending { it.timestamp }) { note ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp) // ~3 short sentences
                                .shadow(8.dp, shape = MaterialTheme.shapes.medium, clip = false)
                                .clickable { navController.navigate("note_detail/${note.id}") },
                            shape = MaterialTheme.shapes.medium,
                            color = card,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = SolidColor(hairline)
                            )
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = note.resolvedTitle,
                                    color = onCard,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (note.aiSummary.isNotBlank()) note.aiSummary else note.content,
                                    color = onCard,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = SimpleDateFormat(
                                        "MMM dd, yyyy  HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(note.timestamp)),
                                    fontSize = 12.sp,
                                    color = subText
                                )
                            }
                        }
                    }
                }
            }

            // Record Button (no extra nav-bar padding; Scaffold already applied insets)
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B),
                    modifier = Modifier
                        .size(80.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (!hasAudioPermission) {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    navigateToRecordingIfNotBusy(navController, recordingViewModel, notesViewModel)
                                }
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp)) {}
                    }
                }
            }

            // Settings Button (also relies on Scaffold insets)
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = onCard.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun navigateToRecordingIfNotBusy(
    navController: NavController,
    recordingViewModel: RecordingViewModel,
    notesViewModel: NotesViewModel
) {
    if (!recordingViewModel.isRecording.value) {
        navController.navigate("recording")
        Log.d("MainScreen", "Navigating to RecordingScreen...")
    } else {
        Log.e("MainScreen", "Recording already in progress!")
    }
}
