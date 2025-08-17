package com.example.ainotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.TranscriptsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptsScreen(navController: NavController, viewModel: TranscriptsViewModel) {
    val transcripts by viewModel.transcripts.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        if (transcripts.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("No transcripts saved yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transcripts) { transcript ->
                    Column(
                        modifier = Modifier
                            .clickable { navController.navigate("transcript_detail/${transcript.id}") }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = formatDate(transcript.timestamp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(transcript.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun formatDate(time: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}
