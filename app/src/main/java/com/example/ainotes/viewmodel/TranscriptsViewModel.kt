package com.example.ainotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.model.Transcript
import com.example.ainotes.data.repository.TranscriptsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel wrapping [TranscriptsRepository].
 */
class TranscriptsViewModel : ViewModel() {
    private val repository = TranscriptsRepository()

    private val _transcripts = MutableStateFlow<List<Transcript>>(emptyList())
    val transcripts: StateFlow<List<Transcript>> = _transcripts

    init {
        viewModelScope.launch {
            repository.getTranscripts().collectLatest { list ->
                _transcripts.value = list
            }
        }
    }

    fun addTranscript(text: String) {
        viewModelScope.launch { repository.addTranscript(text) }
    }

    fun updateTranscript(transcript: Transcript) {
        viewModelScope.launch { repository.updateTranscript(transcript) }
    }

    fun deleteTranscript(id: String) {
        viewModelScope.launch { repository.deleteTranscript(id) }
    }
}
