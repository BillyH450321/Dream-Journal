package com.example.ui

data class AudioPlaybackState(
    val dreamId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
)

sealed interface RecordingState {
    object Idle : RecordingState
    object Recording : RecordingState
    data class Processing(val stage: String) : RecordingState
    data class Success(val dreamId: Long) : RecordingState
    data class Error(val message: String) : RecordingState
}

sealed interface PatternAnalysisState {
    object Idle : PatternAnalysisState
    object Loading : PatternAnalysisState
    data class Success(val report: String, val dreamCountAnalyzed: Int) : PatternAnalysisState
    data class Error(val message: String) : PatternAnalysisState
}

sealed interface ApiTestState {
    object Idle : ApiTestState
    object Loading : ApiTestState
    data class Result(val message: String, val success: Boolean) : ApiTestState
}