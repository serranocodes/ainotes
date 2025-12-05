package com.example.ainotes.viewmodel

data class RecordingUiState(
    val rawText: String = "",
    val cleanText: String = "",
    val isAiCleaning: Boolean = false,
)
