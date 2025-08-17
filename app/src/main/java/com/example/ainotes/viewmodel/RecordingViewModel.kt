package com.example.ainotes.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordingViewModel : ViewModel() {

    // Used to animate your waveform (from onRmsChanged)
    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    // Indicates if speech recognition is active
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // Indicates if transcription is in progress
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing

    // Holds the final recognized text
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    // SpeechRecognizer instance
    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Starts "recording" (i.e. speech recognition) by creating and starting a SpeechRecognizer.
     * If a recognizer is already active, it stops it first.
     */
    fun startRecording(context: Context) {
        viewModelScope.launch {
            // Stop any previous recognition if active
            stopRecordingInternal()

            // Create a new SpeechRecognizer instance and set its listener
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecording.value = true
                        Log.d("RecordingViewModel", "Ready for speech")
                    }
                    override fun onBeginningOfSpeech() {
                        Log.d("RecordingViewModel", "User started speaking")
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        // Scale the RMS value for the waveform (tweak multiplier as needed)
                        val amp = (rmsdB * 2000).toInt().coerceIn(0, 32767)
                        _amplitude.value = amp
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isRecording.value = false
                        _isTranscribing.value = true
                        Log.d("RecordingViewModel", "Speech ended")
                    }
                    override fun onError(error: Int) {
                        _isRecording.value = false
                        _isTranscribing.value = false
                        Log.e("RecordingViewModel", "Speech recognition error: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _recognizedText.value = matches[0]
                            Log.d("RecordingViewModel", "Recognized text: ${matches[0]}")
                        }
                        _isTranscribing.value = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            // Create the speech recognition intent correctly using Intent constructor
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    3500
                )
            }

            // Start listening
            speechRecognizer?.startListening(intent)
        }
    }

    /**
     * Stops the speech recognition.
     * Calls an internal suspend function inside a coroutine.
     */
    fun stopRecording() {
        viewModelScope.launch {
            stopRecordingInternal()
        }
    }

    /**
     * Cancels speech recognition and clears the recognized text.
     */
    fun cancelRecording() {
        viewModelScope.launch {
            stopRecordingInternal()
            _recognizedText.value = ""
            _amplitude.value = 0
            Log.d("RecordingViewModel", "Speech recognition canceled.")
        }
    }

    /**
     * Private helper that stops the SpeechRecognizer if active.
     */
    private suspend fun stopRecordingInternal() {
        if (_isRecording.value) {
            Log.d("RecordingViewModel", "Stopping speech recognition...")
            _isRecording.value = false
            speechRecognizer?.stopListening()
            // Wait briefly for final results (if needed)
            delay(200L)
        }
        _isTranscribing.value = false
        _amplitude.value = 0
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}