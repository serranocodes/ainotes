package com.example.ainotes.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ainotes.data.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        val now = System.currentTimeMillis()
        _notes.value = listOf(
            Note(id = "1", timestamp = now - 60_000, content = "First sample note"),
            Note(id = "2", timestamp = now, content = "Second sample note")
        )
    }

    fun addNote(note: Note) {
        _notes.update { current -> listOf(note) + current }
    }

    fun getNoteById(id: String): Note? = _notes.value.find { it.id == id }

    fun toggleFavorite(id: String) {
        _notes.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    fun updateNote(id: String, newContent: String) {
        _notes.update { list ->
            list.map { if (it.id == id) it.copy(content = newContent) else it }
        }
    }

    fun deleteNote(id: String) {
        _notes.update { list -> list.filterNot { it.id == id } }
    }
}
