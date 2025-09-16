package com.example.ainotes.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.model.Note
import com.example.ainotes.data.repository.NotesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository = NotesRepository()
) : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes
    private val migratedNoteIds = mutableSetOf<String>()

    /**
     * Start collecting notes from the repository. This should only be called
     * after the user has successfully authenticated; otherwise Firestore will
     * close the flow with an [IllegalStateException].
     */
    fun startCollectingNotes() {
        viewModelScope.launch {
            repository.getNotes()
            .catch { e ->
                if (e is IllegalStateException && e.message == "User not authenticated") {
                    // User is not signed in – ignore or trigger login flow
                } else {
                    throw e
                }
            }
            .collect { fetched ->
                val sanitized = fetched.map { note ->
                    if (note.title.isBlank()) {
                        val fallback = Note.fallbackTitleFromContent(note.content)
                        val updated = note.copy(title = fallback)
                        if (note.id.isNotBlank() && migratedNoteIds.add(note.id)) {
                            viewModelScope.launch { repository.updateNote(updated) }
                        }
                        updated
                    } else note
                }
                _notes.clear()
                _notes.addAll(sanitized)
            }
        }
    }

    fun addNote(content: String) {
        val fallback = Note.fallbackTitleFromContent(content)
        val note = Note(content = content, title = fallback)
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
            val current = _notes[index]
            val updatedTitle = if (current.title.isNotBlank()) current.title
            else Note.fallbackTitleFromContent(newContent)
            val updated = current.copy(content = newContent, title = updatedTitle)
            _notes[index] = updated
            viewModelScope.launch { repository.updateNote(updated) }
        }
    }

    fun deleteNote(id: String) {
        _notes.removeAll { it.id == id }
        viewModelScope.launch { repository.deleteNote(id) }
    }
}
