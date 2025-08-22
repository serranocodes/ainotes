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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    ) { isGranted: Boolean ->
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

    LaunchedEffect(Unit) {
        notesViewModel.startCollectingNotes()
    }

    // Palette tuned to match Recording screen look
    val bg = Color(0xFF0D0F13)          // deep charcoal
    val card = Color(0xFF141922)        // dark surface
    val onCard = Color(0xFFECEDEF)      // high-contrast text
    val subText = Color(0xFF9AA4B2)     // secondary
    val hairline = Color(0x22FFFFFF)    // ultra subtle borders

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
    ) {
        Box(Modifier.fillMaxSize()) {
            if (notesViewModel.notes.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                // Notes list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notesViewModel.notes.sortedByDescending { it.timestamp }) { note ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp) // ~3 short sentences
                                .shadow(8.dp, shape = MaterialTheme.shapes.medium, clip = false)
                                .clickable { navController.navigate("note_detail/${note.id}") },
                            shape = MaterialTheme.shapes.medium,
                            color = card,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(hairline)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = note.content,
                                    color = onCard,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    maxLines = 6, // ~3 sentence preview
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
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

            /** Record Button **/
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4B4B), // a bit softer than pure red on black
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
