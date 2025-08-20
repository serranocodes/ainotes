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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.min

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
    private var lastPartial: String = ""
    private var restartPending = false
    private var controllerJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val notesRepository: NotesRepository = NotesRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var currentTranscriptionId: String? = null

    // ---- Public API ----

    fun startRecording(context: Context) {
        if (_isRecording.value) return

        controllerJob?.cancel()
        controllerJob = viewModelScope.launch {
            withContext(Dispatchers.Main) {
                if (speechRecognizer == null) {
                    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                        Log.e("RecordingVM", "Speech recognition not available on this device")
                        return@withContext
                    }
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(recognitionListener)
                    }
                }
                resetTranscription(keepText = true)
                _isRecording.value = true
                _isTranscribing.value = true
                restartPending = false
                startListeningInternal()
            }
        }
    }

    fun stopRecording() {
        controllerJob?.cancel()
        viewModelScope.launch(Dispatchers.Main) {
            _isRecording.value = false
            _isTranscribing.value = false
            _amplitude.value = 0
            restartPending = false
            lastPartial = "" // important: clear tail between sessions
            try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
        }
    }

    fun cancelRecording() {
        stopRecording()
        resetTranscription(keepText = false)
    }

    fun updateRecognizedText(newText: String) {
        _recognizedText.value = normalize(newText)
    }

    fun saveTranscription(text: String, onResult: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("RecordingVM", "Cannot save transcription: user not authenticated")
            onResult(false); return
        }
        val normalized = normalize(text)
        val note = if (currentTranscriptionId == null) {
            Note(content = normalized)
        } else {
            Note(id = currentTranscriptionId!!, content = normalized)
        }
        viewModelScope.launch {
            try {
                if (currentTranscriptionId == null) notesRepository.addNote(note)
                else notesRepository.updateNote(note)
                currentTranscriptionId = note.id
                _recognizedText.value = normalized
                onResult(true)
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Error saving transcription", e)
                onResult(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.Main) {
            try { speechRecognizer?.destroy() } catch (_: Throwable) {}
            speechRecognizer = null
        }
    }

    // ---- Internals ----

    private fun resetTranscription(keepText: Boolean) {
        currentTranscriptionId = null
        lastPartial = ""
        if (!keepText) _recognizedText.value = ""
    }

    private fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Best-effort hints; some engines ignore:
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
            putExtra("android.speech.extra.ENABLE_FORMATTING", "quality")
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (t: Throwable) {
            Log.e("RecordingVM", "startListening failed", t)
            scheduleRestart()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("RecordingVM", "Ready for speech")
        }
        override fun onBeginningOfSpeech() {
            Log.d("RecordingVM", "Beginning of speech")
        }
        override fun onRmsChanged(rmsdB: Float) {
            val amp = (rmsdB.coerceAtLeast(0f) * 2000).toInt().coerceIn(0, 32767)
            _amplitude.value = amp
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d("RecordingVM", "End of speech -> restarting")
            scheduleRestart()
        }
        override fun onError(error: Int) {
            Log.w("RecordingVM", "Recognizer error: $error -> restart if recording")
            if (_isRecording.value) scheduleRestart() else _isTranscribing.value = false
        }
        override fun onResults(results: Bundle?) {
            val finalText = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?: return
            commitFinal(finalText)
            _isTranscribing.value = _isRecording.value
            if (_isRecording.value) scheduleRestart()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val newPartial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?: return
            appendPartial(newPartial)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /** Replace trailing partial and dedupe any overlap with the already-committed text. */
    private fun appendPartial(newPartial: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        // Remove the *previous* partial once
        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        // Now smart-merge to avoid cross-restart duplication
        val merged = smartConcat(base, newPartial)
        _recognizedText.value = normalize(merged)

        // Tail we just appended (for the next replacement)
        lastPartial = normalize(merged.removePrefix(base).trimStart())
    }

    /** Commit final by stripping the live partial and deduping overlap. */
    private fun commitFinal(finalText: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        val merged = smartConcat(base, finalText)
        _recognizedText.value = normalize(merged)
        lastPartial = "" // clear live tail
    }

    private fun scheduleRestart(delayMs: Long = 100L) {
        if (!_isRecording.value || restartPending) return
        restartPending = true
        _isTranscribing.value = true
        _amplitude.value = 0
        lastPartial = "" // prevent cross-session duplication
        mainHandler.postDelayed({
            try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
            mainHandler.postDelayed({
                restartPending = false
                if (_isRecording.value) startListeningInternal()
            }, 80L)
        }, delayMs)
    }

    // ---- Helpers ----

    /** Normalize whitespace to keep merges clean. */
    private fun normalize(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    /**
     * Concatenate `addition` to `base` while removing any word-level overlap
     * where the end of `base` matches the beginning of `addition`.
     * This protects against engines that resend the whole hypothesis.
     */
    private fun smartConcat(base: String, addition: String, maxOverlapWords: Int = 12): String {
        if (base.isEmpty()) return addition
        val baseWords = words(base)
        val addWords = words(addition)
        val maxK = min(maxOverlapWords, min(baseWords.size, addWords.size))
        var overlap = 0
        for (k in maxK downTo 1) {
            var match = true
            var i = 0
            while (i < k && match) {
                if (!tokensEqual(baseWords[baseWords.size - k + i], addWords[i])) match = false
                i++
            }
            if (match) { overlap = k; break }
        }
        val addTail = if (overlap == 0) addition else
            reconstruct(addWords.drop(overlap))
        return if (addTail.isEmpty()) base else
            (if (base.isEmpty()) addTail else "$base $addTail")
    }

    /** Split into tokens (words) in a locale-aware way; keep original substrings for reconstruction. */
    private fun words(s: String): List<String> {
        val it = BreakIterator.getWordInstance(Locale.getDefault())
        it.setText(s)
        val out = mutableListOf<String>()
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val token = s.substring(start, end)
            if (token.any { c -> c.isLetterOrDigit() }) {
                out += token.trim()
            }
            start = end
            end = it.next()
        }
        return out
    }

    private fun reconstruct(tokens: List<String>): String =
        tokens.joinToString(" ")

    private fun tokensEqual(a: String, b: String): Boolean {
        // Compare case-insensitively and ignore simple trailing punctuation
        fun norm(t: String) = t.trim().trimEnd('.', ',', ';', ':', '!', '?', '…').lowercase()
        return norm(a) == norm(b)
    }
}
