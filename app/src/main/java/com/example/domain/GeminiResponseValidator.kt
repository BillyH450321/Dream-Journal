package com.example.domain

object GeminiResponseValidator {
    fun isTranscriptionFailure(text: String): Boolean =
        text.contains("API Key Error") ||
            text.contains("Transcription failed") ||
            isRateLimited(text)

    fun isRateLimited(text: String): Boolean =
        text.contains("rate limit", ignoreCase = true) ||
            text.contains("HTTP 429")

    fun isPatternAnalysisFailure(text: String): Boolean =
        text.contains("API Key Error") || text.contains("Analysis failed")
}