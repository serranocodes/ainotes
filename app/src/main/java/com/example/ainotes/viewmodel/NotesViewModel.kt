package com.example.ainotes.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.model.Note
import com.example.ainotes.data.repository.NotesRepository
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository = NotesRepository()
) : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes

    init {
        // Collect notes from repository and keep local state in sync
        viewModelScope.launch {
            repository.getNotes().collect { fetched ->
                _notes.clear()
                _notes.addAll(fetched)
            }
        }
    }

    fun addNote(content: String) {
        val note = Note(content = content)
        _notes.add(note)
        viewModelScope.launch { repository.addNote(note) }
    }

    fun getNoteById(id: String): Note? = _notes.find { it.id == id }

    fun toggleFavorite(id: String) {
        val index = _notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = _notes[index].copy(isFavorite = !_notes[index].isFavorite)
            _notes[index] = updated
            viewModelScope.launch { repository.updateNote(updated) }
        }
    }

    fun updateNote(id: String, newContent: String) {
        val index = _notes.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = _notes[index].copy(content = newContent)
            _notes[index] = updated
            viewModelScope.launch { repository.updateNote(updated) }
        }
    }

    fun deleteNote(id: String) {
        _notes.removeAll { it.id == id }
        viewModelScope.launch { repository.deleteNote(id) }
    }
}
