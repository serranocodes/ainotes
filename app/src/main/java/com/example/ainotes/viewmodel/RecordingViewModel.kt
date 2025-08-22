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
    private var controllerJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    //Languages
    private val _languageTag = MutableStateFlow(Locale.getDefault().toLanguageTag())
    fun setLanguageTag(tag: String) { _languageTag.value = tag }

    // Restart throttling
    private var lastRestartMs = 0L
    private val MIN_RESTART_INTERVAL_MS = 1200L
    private val RESTART_DELAY_MS = 420L

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
                lastPartial = ""
                _isRecording.value = true
                _isTranscribing.value = true
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
            lastPartial = ""
            try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
        }
    }

    fun cancelRecording() {
        stopRecording()
        resetTranscription(keepText = false)
    }

    fun saveTranscription(text: String, onResult: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid
        if (uid == null) { onResult(false); return }
        val normalized = normalize(text)
        val note = if (currentTranscriptionId == null) Note(content = normalized)
        else Note(id = currentTranscriptionId!!, content = normalized)
        viewModelScope.launch {
            try {
                if (currentTranscriptionId == null) notesRepository.addNote(note)
                else notesRepository.updateNote(note)
                currentTranscriptionId = note.id
                _recognizedText.value = normalized
                resetTranscription(keepText = false)
                onResult(true)
            } catch (e: Exception) {
                Log.e("RecordingVM", "Error saving transcription", e)
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

    // RecordingViewModel.kt
    private fun startListeningInternal() {
        val tag = _languageTag.value  // e.g., "es-ES", "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

            // Use cloud when needed → many locales aren’t available offline
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)

            // Endpointer tuning (optional)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000)

            // Optional extras some engines honor
            putExtra("android.speech.extra.ENABLE_FORMATTING", "quality")
            putExtra("android.speech.extra.SEGMENTED_SESSION", true)
        }

        Log.d("RecordingVM", "Recognizer language = $tag")
        try {
            speechRecognizer?.startListening(intent)
            lastRestartMs = System.currentTimeMillis()
        } catch (t: Throwable) {
            Log.e("RecordingVM", "startListening failed", t)
            restartAfterSegment()
        }
    }


    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val amp = (rmsdB.coerceAtLeast(0f) * 2000).toInt().coerceIn(0, 32767)
            _amplitude.value = amp
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        // IMPORTANT: do not force a restart here; wait for results/error
        override fun onEndOfSpeech() {
            // no-op: the service will deliver onResults or an error next
        }

        override fun onError(error: Int) {
            // Recoverable errors -> start a new segment calmly
            // 5: SPEECH_TIMEOUT, 6: NO_MATCH, 7: BUSY, 8: INSUFFICIENT_PERMISSIONS, 9: NETWORK
            if (_isRecording.value) restartAfterSegment()
            else _isTranscribing.value = false
        }

        override fun onResults(results: Bundle?) {
            val finalText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!finalText.isNullOrBlank()) commitFinal(finalText)
            _isTranscribing.value = _isRecording.value
            if (_isRecording.value) restartAfterSegment()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val newPartial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            appendPartial(newPartial)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /** Schedule the next start WITHOUT calling stopListening(), with debounce/throttle. */
    private fun restartAfterSegment() {
        if (!_isRecording.value) { _isTranscribing.value = false; return }

        val now = System.currentTimeMillis()
        val elapsed = now - lastRestartMs
        val delay = max(RESTART_DELAY_MS, MIN_RESTART_INTERVAL_MS - elapsed)

        _isTranscribing.value = true
        lastPartial = "" // don’t carry a stale tail
        mainHandler.postDelayed({
            if (_isRecording.value) startListeningInternal()
        }, delay)
    }

    /** Replace trailing partial and dedupe aggressively. */
    private fun appendPartial(newPartial: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        var merged = mergeAppend(base, newPartial)
        merged = dedupeTailBlock(merged)

        _recognizedText.value = normalize(merged)
        lastPartial = normalize(merged.removePrefix(base).trimStart())
    }

    /** Commit final by stripping live partial and deduping. */
    private fun commitFinal(finalText: String) {
        val current = _recognizedText.value
        val prevPartial = lastPartial

        val base = if (prevPartial.isNotEmpty() && current.endsWith(prevPartial)) {
            current.removeSuffix(prevPartial).trimEnd()
        } else current

        var merged = mergeAppend(base, finalText)
        merged = dedupeTailBlock(merged)

        _recognizedText.value = normalize(merged)
        lastPartial = ""
    }

    // ---- Merge & dedupe helpers ----

    private fun normalize(s: String): String = s.replace(Regex("\\s+"), " ").trim()

    private fun mergeAppend(base: String, addition: String, windowWords: Int = 120): String {
        if (addition.isBlank()) return base
        if (base.isBlank()) return addition

        val baseWords = words(base)
        val addWords = words(addition)

        // 1) end/start overlap
        val maxK = min(20, min(baseWords.size, addWords.size))
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

        // 2) substring search in tail window (handles sentence repeats)
        if (remainder.isNotEmpty()) {
            val tailStart = max(0, baseWords.size - windowWords)
            val tail = baseWords.subList(tailStart, baseWords.size)
            val addAll = words(remainder)

            var cut = 0
            val maxPrefix = min(addAll.size, windowWords)
            outer@ for (len in maxPrefix downTo 1) {
                val prefix = addAll.subList(0, len)
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

    /** If the transcript ends with “… X X” (two equal blocks), drop the last X. */
    private fun dedupeTailBlock(text: String, maxBlockTokens: Int = 50): String {
        val toks = words(text)
        val n = toks.size
        if (n < 8) return text

        val maxH = min(maxBlockTokens, n / 2)
        for (h in maxH downTo 4) {
            var same = true
            var i = 0
            while (i < h && same) {
                if (!tokensEqual(toks[n - 2*h + i], toks[n - h + i])) same = false
                i++
            }
            if (same) {
                val kept = toks.dropLast(h)
                return reconstruct(kept)
            }
        }
        return text
    }

    // ---- Tokenization ----

    private fun words(s: String): List<String> {
        val it = BreakIterator.getWordInstance(Locale.getDefault())
        it.setText(s)
        val out = mutableListOf<String>()
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val token = s.substring(start, end)
            if (token.any { c -> c.isLetterOrDigit() }) out += token.trim()
            start = end
            end = it.next()
        }
        return out
    }

    private fun reconstruct(tokens: List<String>): String = tokens.joinToString(" ")

    private fun tokensEqual(a: String, b: String): Boolean {
        fun norm(t: String): String =
            t.trim()
                .lowercase()
                .replace("’", "'")
                .replace("`", "'")
                .replace("´", "'")
                .replace("'", "") // I'm == Im
                .trimEnd('.', ',', ';', ':', '!', '?', '…', ')', ']', '"')
        return norm(a) == norm(b)
    }
}
