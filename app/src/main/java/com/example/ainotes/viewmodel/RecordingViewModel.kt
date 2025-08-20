package com.example.ainotes.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.model.Note
import com.example.ainotes.data.repository.NotesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordingViewModel : ViewModel() {

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var recognizerIntent: Intent
    private val finalText = StringBuilder()

    private val notesRepository: NotesRepository = NotesRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var currentTranscriptionId: String? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isRecording.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            val amp = (rmsdB * 2000).toInt().coerceIn(0, 32767)
            _amplitude.value = amp
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isRecording.value = false
            _isTranscribing.value = true
            scheduleRestart()
        }

        override fun onError(error: Int) {
            _isRecording.value = false
            _isTranscribing.value = false
            if (error == 5 || error == 6 || error == 7) {
                scheduleRestart()
            } else {
                Log.e("RecordingViewModel", "Speech recognition error: $error")
            }
        }

        override fun onResults(results: Bundle?) {
            val text =
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrEmpty()) {
                appendText(text, true)
                Log.d("RecordingViewModel", "Recognized text: $text")
            }
            _isTranscribing.value = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text =
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrEmpty()) {
                appendText(text, false)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onBeginningOfSpeech() {}
    }

    fun startRecording(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            stopRecordingInternal()
            resetTranscription()
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(recognitionListener)
                }
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra("android.speech.extra.ENABLE_FORMATTING", "quality")
                }
            }
            scheduleRestart(0)
        }
    }

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.Main) {
            stopRecordingInternal()
        }
    }

    fun cancelRecording() {
        viewModelScope.launch(Dispatchers.Main) {
            stopRecordingInternal()
            resetTranscription()
            _amplitude.value = 0
        }
    }

    fun updateRecognizedText(newText: String) {
        finalText.clear()
        finalText.append(newText)
        _recognizedText.value = newText
    }

    fun saveTranscription(text: String, onResult: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("RecordingViewModel", "Cannot save transcription: user not authenticated")
            onResult(false)
            return
        }

        val note = if (currentTranscriptionId == null) {
            Note(content = text)
        } else {
            Note(id = currentTranscriptionId!!, content = text)
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (currentTranscriptionId == null) {
                    notesRepository.addNote(note)
                } else {
                    notesRepository.updateNote(note)
                }
                currentTranscriptionId = note.id
                _recognizedText.value = text
                onResult(true)
                resetTranscription()
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Error saving transcription", e)
                onResult(false)
            }
        }
    }

    private fun appendText(text: String, isFinal: Boolean) {
        if (isFinal) {
            finalText.append(text).append(' ')
            _recognizedText.value = finalText.toString()
        } else {
            _recognizedText.value = finalText.toString() + text
        }
    }

    private fun resetTranscription() {
        currentTranscriptionId = null
        finalText.clear()
        _recognizedText.value = ""
    }

    private fun scheduleRestart(delay: Long = 500L) {
        handler.post {
            speechRecognizer?.stopListening()
        }
        handler.postDelayed({
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Failed to start listening", e)
            }
        }, delay)
    }

    private fun stopRecordingInternal() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isRecording.value = false
        _isTranscribing.value = false
        _amplitude.value = 0
    }
}

