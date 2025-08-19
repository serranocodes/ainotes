package com.example.ainotes.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.ainotes.data.model.Note

class NotesViewModel : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes

    init {
        // Sample data
        val now = System.currentTimeMillis()
        _notes.addAll(
            listOf(
                Note(id = "1", timestamp = now - 60_000, content = "First sample note"),
                Note(id = "2", timestamp = now, content = "Second sample note")
            )
        )
    }

    fun getNoteById(id: String): Note? = _notes.find { it.id == id }

    fun toggleFavorite(id: String) {
        val index = _notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val note = _notes[index]
            _notes[index] = note.copy(isFavorite = !note.isFavorite)
        }
    }

    fun updateNote(id: String, newContent: String) {
        val index = _notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val note = _notes[index]
            _notes[index] = note.copy(content = newContent)
        }
    }

    fun deleteNote(id: String) {
        _notes.removeAll { it.id == id }
    }
}