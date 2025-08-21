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
import kotlin.math.max
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
            lastPartial = "" // clear tail between sessions
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

    /** Replace trailing partial and dedupe any overlap or repetition. */
    private fun appendPartial(newPartial: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        // Remove the previous live tail from the end (once)
        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        // Append with robust dedupe
        val merged = mergeAppend(base, newPartial)
        _recognizedText.value = normalize(merged)

        // Save the newly appended tail (whatever got added beyond `base`)
        lastPartial = normalize(merged.removePrefix(base).trimStart())
    }

    /** Commit final by stripping live partial and deduping overlap & repetition. */
    private fun commitFinal(finalText: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        val merged = mergeAppend(base, finalText)
        _recognizedText.value = normalize(merged)
        lastPartial = "" // clear live tail
    }

    private fun scheduleRestart(delayMs: Long = 100L) {
        if (!_isRecording.value || restartPending) return
        restartPending = true
        _isTranscribing.value = true
        _amplitude.value = 0
        lastPartial = "" // avoid carrying a stale tail across sessions
        mainHandler.postDelayed({
            try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
            mainHandler.postDelayed({
                restartPending = false
                if (_isRecording.value) startListeningInternal()
            }, 80L)
        }, delayMs)
    }

    // ---- Helpers ----

    /** Normalize whitespace. */
    private fun normalize(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    /**
     * Append `addition` to `base` while removing:
     *  1) any overlap between the end of `base` and the start of `addition`
     *  2) any prefix of `addition` that already appears ANYWHERE within the last window of `base`
     *
     * This prevents the recognizer from duplicating a whole sentence on restarts.
     */
    private fun mergeAppend(base: String, addition: String, windowWords: Int = 40): String {
        if (addition.isBlank()) return base
        if (base.isBlank()) return addition

        val baseWords = words(base)
        val addWords = words(addition)

        // 1) classic suffix/prefix overlap (fast path)
        val maxK = min(12, min(baseWords.size, addWords.size)) // up to 12-word exact overlap
        var overlap = 0
        for (k in maxK downTo 1) {
            var ok = true
            var i = 0
            while (i < k && ok) {
                if (!tokensEqual(baseWords[baseWords.size - k + i], addWords[i])) ok = false
                i++
            }
            if (ok) { overlap = k; break }
        }
        var remainder = if (overlap > 0) reconstruct(addWords.drop(overlap)) else reconstruct(addWords)

        // 2) substring dedupe within a tail window of base (handles full-sentence repeats)
        if (remainder.isNotEmpty()) {
            val tailStart = max(0, baseWords.size - windowWords)
            val tail = baseWords.subList(tailStart, baseWords.size)
            val addAll = words(remainder) // words still to append

            // find largest prefix of addAll that appears anywhere inside `tail`
            var cut = 0
            val maxPrefix = min(addAll.size, windowWords)
            outer@ for (len in maxPrefix downTo 1) {
                val prefix = addAll.subList(0, len)
                // sliding window search in the tail
                val maxI = tail.size - len
                for (i in 0..maxI) {
                    var match = true
                    var j = 0
                    while (j < len && match) {
                        if (!tokensEqual(tail[i + j], prefix[j])) match = false
                        j++
                    }
                    if (match) { cut = len; break@outer }
                }
            }
            if (cut > 0) {
                val remaining = addAll.drop(cut)
                remainder = if (remaining.isEmpty()) "" else reconstruct(remaining)
            }
        }

        return if (remainder.isEmpty()) base else "$base $remainder"
    }

    /** Tokenize to words (locale-aware), keeping original substrings for reconstruction. */
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
        // Case-insensitive, ignore simple punctuation and apostrophes
        fun norm(t: String): String =
            t.trim()
                .lowercase()
                .replace("’", "'")
                .replace("`", "'")
                .replace("´", "'")
                .replace("'", "") // ignore apostrophes (I'm == Im)
                .trimEnd('.', ',', ';', ':', '!', '?', '…', ')', ']', '"', '\'')

        return norm(a) == norm(b)
    }
}
