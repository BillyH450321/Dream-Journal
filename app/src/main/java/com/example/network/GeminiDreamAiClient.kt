package com.example.network

import com.example.domain.DreamAiClient

class GeminiDreamAiClient : DreamAiClient {
    override suspend fun testConnection(): String = GeminiClient.testConnection()

    override suspend fun transcribeAudio(audioBytesBase64: String, mimeType: String): String =
        GeminiClient.transcribeAudio(audioBytesBase64, mimeType)

    override suspend fun interpretDream(dreamText: String): String =
        GeminiClient.interpretDream(dreamText)

    override suspend fun suggestDreamMetadata(dreamText: String): DreamMetadata =
        GeminiClient.suggestDreamMetadata(dreamText)

    override suspend fun generateSurrealImage(dreamText: String): ImageGenerationResult =
        GeminiClient.generateSurrealImage(dreamText)

    override suspend fun askFollowUpQuestion(
        dreamText: String,
        history: List<Content>,
        question: String
    ): String = GeminiClient.askFollowUpQuestion(dreamText, history, question)

    override suspend fun analyzeDreamPatterns(dreamTexts: List<String>): String =
        GeminiClient.analyzeDreamPatterns(dreamTexts)
}