// File: TranscriptionScreen.kt
package com.example.ainotes.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ainotes.data.AiSummaryPreferences
import com.example.ainotes.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    navController: NavController,
    viewModel: RecordingViewModel,
    startInEdit: Boolean = false,
) {
    val recognizedText by viewModel.recognizedText.collectAsState()

    // Seed editor state from startInEdit; persist across config changes
    var isEditing by rememberSaveable { mutableStateOf(startInEdit) }
    var editableText by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    val autoTitleSummary by AiSummaryPreferences
        .autoTitleEnabled(context)
        .collectAsState(initial = false)
    val autoNoteSummary by AiSummaryPreferences
        .autoNoteEnabled(context)
        .collectAsState(initial = false)

    // Initialize editor content on first composition
    LaunchedEffect(Unit) {
        editableText = recognizedText
    }

    // Keep editor in sync with latest recognition when NOT editing
    LaunchedEffect(recognizedText) {
        if (!isEditing) editableText = recognizedText
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
                        colors = listOf(
                            Color(0xFF1E3A8A),
                            Color(0xFF1E40AF),
                            Color(0xFF2563EB)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header + transcription card
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Transcribed Text",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp)) {
                            if (isEditing) {
                                TextField(
                                    value = editableText,
                                    onValueChange = { editableText = it },
                                    textStyle = TextStyle(fontSize = 20.sp, color = Color.Black),
                                    colors = TextFieldDefaults.textFieldColors(
                                        containerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = editableText,
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // ICON-ONLY actions: Cancel • Save  (Edit removed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel -> discard draft changes & go back to Recording screen
                    FilledTonalIconButton(
                        onClick = {
                            // revert any edits and just pop back; do NOT clear VM transcript
                            isEditing = false
                            editableText = recognizedText
                            navController.popBackStack() // back to "recording"
                        },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x33FF6B6B),
                            contentColor = Color(0xFFFF6B6B)
                        )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }

                    // Save -> persist & go to Main (keep as before)
                    FilledIconButton(
                        onClick = {
                            viewModel.saveTranscription(
                                context = context,
                                text = editableText,
                                useAiTitle = autoTitleSummary,
                                useAiContentSummary = autoNoteSummary
                            ) { success ->
                                if (success) {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save transcription",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            isEditing = false
                        },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF32D74B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                }
            }
        }
    }
}
