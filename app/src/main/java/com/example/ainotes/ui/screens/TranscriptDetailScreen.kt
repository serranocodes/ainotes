package com.example.ainotes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.TranscriptsViewModel

@Composable
fun TranscriptDetailScreen(navController: NavController, transcriptId: String, viewModel: TranscriptsViewModel) {
    val transcripts by viewModel.transcripts.collectAsState()
    val transcript = transcripts.find { it.id == transcriptId }
    var text = remember { mutableStateOf(transcript?.text ?: "") }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (transcript == null) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Transcript not found")
            }
            return@Surface
        }
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.updateTranscript(transcript.copy(text = text.value))
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
                Button(
                    onClick = {
                        viewModel.deleteTranscript(transcript.id)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) { Text("Delete", color = Color.White) }
            }
        }
    }
}
