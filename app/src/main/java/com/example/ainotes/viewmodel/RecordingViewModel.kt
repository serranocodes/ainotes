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

/**
 * ViewModel that manages continuous speech recognition. The recognizer is restarted
 * whenever speech ends or an error occurs so that the user can speak for as long as
 * needed until they explicitly stop the recording.
 */
class RecordingViewModel : ViewModel() {

    // Used to animate the waveform
    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    // Indicates if speech recognition is active
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // Accumulates the recognized text
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    /**
     * Begin listening for speech. The SpeechRecognizer is (re)created each time to avoid
     * lingering state from a previous session.
     */
    fun startRecording(context: Context) {
        viewModelScope.launch {
            stopRecordingInternal()

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
                        val amp = (rmsdB * 2000).toInt().coerceIn(0, 32767)
                        _amplitude.value = amp
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        Log.d("RecordingViewModel", "Speech ended")
                        if (_isRecording.value) restartListening()
                    }
                    override fun onError(error: Int) {
                        Log.e("RecordingViewModel", "Speech recognition error: $error")
                        if (_isRecording.value) restartListening()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _recognizedText.value = (_recognizedText.value + " " + text).trim()
                            Log.d("RecordingViewModel", "Recognized text: $text")
                        }
                        if (_isRecording.value) restartListening()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _recognizedText.value = (_recognizedText.value + " " + text).trim()
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Increase silence timeout so users can pause without stopping recognition
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
            }

            startListening()
        }
    }

    /** Stop listening and release the SpeechRecognizer. */
    fun stopRecording() {
        viewModelScope.launch {
            stopRecordingInternal()
        }
    }

    /** Cancel the current session and clear any accumulated text. */
    fun cancelRecording() {
        viewModelScope.launch {
            stopRecordingInternal()
            _recognizedText.value = ""
            _amplitude.value = 0
            Log.d("RecordingViewModel", "Speech recognition canceled.")
        }
    }

    private suspend fun stopRecordingInternal() {
        if (_isRecording.value) {
            _isRecording.value = false
            speechRecognizer?.stopListening()
            // Allow time for any final results to be returned
            delay(200L)
        }
        _amplitude.value = 0
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun startListening() {
        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun restartListening() {
        speechRecognizer?.cancel()
        startListening()
    }
}

