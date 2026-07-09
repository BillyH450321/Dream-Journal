package com.example.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiResponseValidatorTest {

    @Test
    fun `detects transcription failures`() {
        assertTrue(GeminiResponseValidator.isTranscriptionFailure("API Key Error: missing"))
        assertTrue(GeminiResponseValidator.isTranscriptionFailure("Transcription failed"))
        assertTrue(GeminiResponseValidator.isTranscriptionFailure("HTTP 429 Too Many Requests"))
        assertFalse(GeminiResponseValidator.isTranscriptionFailure("I dreamed of flying"))
    }

    @Test
    fun `detects rate limits`() {
        assertTrue(GeminiResponseValidator.isRateLimited("rate limit exceeded"))
        assertTrue(GeminiResponseValidator.isRateLimited("HTTP 429"))
        assertFalse(GeminiResponseValidator.isRateLimited("success"))
    }

    @Test
    fun `detects pattern analysis failures`() {
        assertTrue(GeminiResponseValidator.isPatternAnalysisFailure("API Key Error"))
        assertTrue(GeminiResponseValidator.isPatternAnalysisFailure("Analysis failed"))
        assertFalse(GeminiResponseValidator.isPatternAnalysisFailure("Your recurring water motif suggests..."))
    }
}