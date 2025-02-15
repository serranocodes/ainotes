package com.example.ainotes.viewmodel

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor() : ViewModel() {
    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: File

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private var amplitudeJob: Job? = null

    fun getOutputFile(): File? = if (::outputFile.isInitialized) outputFile else null

    fun startRecording(context: Context) {
        stopRecordingNonSuspend()

        try {
            val timeStamp = System.currentTimeMillis()
            val fileName = "recorded_audio_$timeStamp.m4a"
            outputFile = File(context.filesDir, fileName)

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(outputFile.absolutePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(96000)
                    setAudioSamplingRate(44100)
                    prepare()
                    start()
                }
            } else {
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(outputFile.absolutePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(96000)
                    setAudioSamplingRate(44100)
                    prepare()
                    start()
                }
            }

            _isRecording.value = true
            Log.d("RecordingViewModel", "Recording started successfully.")
            startAmplitudeUpdates()
        } catch (e: Exception) {
            Log.e("RecordingViewModel", "Error starting recording: ${e.message}")
        }
    }

    private fun stopRecordingNonSuspend() {
        if (!_isRecording.value) return
        viewModelScope.launch {
            try {
                _isRecording.value = false
                amplitudeJob?.cancelAndJoin()
                amplitudeJob = null
                _amplitude.value = 0
                recorder?.apply {
                    stop()
                    release()
                }
                recorder = null
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Error in stopRecordingNonSuspend: ${e.message}")
            }
        }
    }

    suspend fun stopRecording() {
        if (!_isRecording.value) {
            Log.d("RecordingViewModel", "Already stopped, no action taken.")
            return
        }
        try {
            Log.d("RecordingViewModel", "Stopping recording...")
            _isRecording.value = false
            amplitudeJob?.cancelAndJoin()
            amplitudeJob = null
            _amplitude.value = 0
            withContext(Dispatchers.IO) {
                recorder?.apply {
                    stop()
                    release()
                }
            }
            recorder = null
            Log.d("RecordingViewModel", "Recording stopped. File saved at: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("RecordingViewModel", "Error stopping recording: ${e.message}")
        }
    }

    private fun startAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch(Dispatchers.IO) {
            while (_isRecording.value) {
                val amp = recorder?.maxAmplitude ?: 0
                _amplitude.update { amp.coerceIn(0, 32767) }
                Log.d("RecordingViewModel", "Amplitude: $amp")
                delay(50)
                if (!_isRecording.value) {
                    _amplitude.value = 0
                    Log.d("RecordingViewModel", "Amplitude updates stopped.")
                    break
                }
            }
        }
    }

    fun cancelRecording() {
        viewModelScope.launch {
            stopRecording()
            outputFile.delete()
            Log.d("RecordingViewModel", "Recording deleted.")
        }
    }
}
