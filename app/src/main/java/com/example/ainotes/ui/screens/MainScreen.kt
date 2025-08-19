package com.example.ainotes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.NotesViewModel
import com.example.ainotes.viewmodel.RecordingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    navController: NavController,
    recordingViewModel: RecordingViewModel,
    notesViewModel: NotesViewModel
) {
    val context = LocalContext.current
    var hasAudioPermission by remember { mutableStateOf(false) }

    // 1) Contract for requesting record-audio permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAudioPermission = isGranted
        if (isGranted) {
            // As soon as user allows, go directly to Recording
            navigateToRecordingIfNotBusy(navController, recordingViewModel, notesViewModel)
        } else {
            Log.e("MainScreen", "Microphone permission denied!")
        }
    }

    // 2) Check current permission once on start
    LaunchedEffect(Unit) {
        hasAudioPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF), Color(0xFF2563EB))
                    )
                )
        ) {
            /** Notes List **/
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp)
            ) {
                items(notesViewModel.notes.sortedByDescending { it.timestamp }) { note ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { navController.navigate("note_detail/${note.id}") },
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = note.content,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
                                    Date(note.timestamp)
                                ),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            /** Record Button (Entire Circle Clickable) **/
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp)
            ) {
                // Use a single Surface for the red circle and place white circle inside it.
                // Disable the default ripple effect (black shadow) by setting indication to null.
                Surface(
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                Log.d("MainScreen", "Record button clicked")
                                if (!hasAudioPermission) {
                                    // Request permission; if user grants, we navigate inside the callback
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    // Already have permission? Go record
                                    navigateToRecordingIfNotBusy(navController, recordingViewModel, notesViewModel)
                                }
                            }
                        )
                ) {
                    // White circle in the center
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {}
                    }
                }
            }

            /** Settings Button **/
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Icon",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * Navigates to "recording" if we are not currently recording.
 */
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
