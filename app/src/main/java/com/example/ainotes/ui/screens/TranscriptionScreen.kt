package com.example.ainotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.RecordingViewModel
import com.example.ainotes.viewmodel.NotesViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    navController: NavController,
    viewModel: RecordingViewModel,
    notesViewModel: NotesViewModel
) {
    // Collect the recognized text from the ViewModel
    val recognizedText by viewModel.recognizedText.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(recognizedText) {
        if (!isEditing) {
            editableText = recognizedText
        }
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
                // Header and transcription box
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            isEditing = false
                            editableText = recognizedText
                            viewModel.cancelRecording()
                            navController.navigate("main") {
                                popUpTo("main") { inclusive = false }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    Button(
                        onClick = { isEditing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Edit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveTranscription(editableText) { success ->
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }
        }
    }
}
