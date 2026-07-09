package com.example.domain

import com.example.network.Content
import com.example.network.DreamMetadata
import com.example.network.ImageGenerationResult

interface DreamAiClient {
    suspend fun testConnection(): String
    suspend fun transcribeAudio(audioBytesBase64: String, mimeType: String): String
    suspend fun interpretDream(dreamText: String): String
    suspend fun suggestDreamMetadata(dreamText: String): DreamMetadata
    suspend fun generateSurrealImage(dreamText: String): ImageGenerationResult
    suspend fun askFollowUpQuestion(
        dreamText: String,
        history: List<Content>,
        question: String
    ): String
    suspend fun analyzeDreamPatterns(dreamTexts: List<String>): String
}