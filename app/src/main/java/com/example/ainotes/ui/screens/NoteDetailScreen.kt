package com.example.ainotes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ainotes.viewmodel.NotesViewModel

@Composable
fun NoteDetailScreen(noteId: String, notesViewModel: NotesViewModel, navController: NavController) {
    val notes by notesViewModel.notes.collectAsState()
    val note = notes.find { it.id == noteId } ?: return
    var text by remember(note) { mutableStateOf(note.content) }
    var isEditing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isEditing) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
            )
        } else {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { notesViewModel.toggleFavorite(noteId) }) {
                Icon(
                    imageVector = if (note.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
            IconButton(onClick = {
                notesViewModel.deleteNote(noteId)
                navController.popBackStack()
            }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
            }
            IconButton(onClick = {
                if (isEditing) {
                    notesViewModel.updateNote(noteId, text)
                    navController.popBackStack()
                }
                isEditing = !isEditing
            }) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    }
}
