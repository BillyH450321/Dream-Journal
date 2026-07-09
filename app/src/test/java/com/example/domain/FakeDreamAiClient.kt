package com.example.domain

import com.example.network.Content
import com.example.network.DreamMetadata
import com.example.network.ImageGenerationResult

class FakeDreamAiClient : DreamAiClient {
    var testConnectionResult: String = "Connection successful. Gemini replied: OK"
    var transcriptionResult: String = "I dreamed of flying over the ocean"
    var interpretationResult: String = "### Symbols\nWater represents the unconscious."
    var metadataResult: DreamMetadata = DreamMetadata(tags = "flying,water", title = "Ocean Flight")
    var imageResult: ImageGenerationResult = ImageGenerationResult(imageBytesBase64 = null)
    var chatAnswer: String = "The water symbolizes emotional depth."
    var patternReport: String = "Recurring water imagery suggests emotional processing."

    val interpretCalls = mutableListOf<String>()
    val transcribeCalls = mutableListOf<Pair<String, String>>()

    override suspend fun testConnection(): String = testConnectionResult

    override suspend fun transcribeAudio(audioBytesBase64: String, mimeType: String): String {
        transcribeCalls += audioBytesBase64 to mimeType
        return transcriptionResult
    }

    override suspend fun interpretDream(dreamText: String): String {
        interpretCalls += dreamText
        return interpretationResult
    }

    override suspend fun suggestDreamMetadata(dreamText: String): DreamMetadata = metadataResult

    override suspend fun generateSurrealImage(dreamText: String): ImageGenerationResult = imageResult

    override suspend fun askFollowUpQuestion(
        dreamText: String,
        history: List<Content>,
        question: String
    ): String = chatAnswer

    override suspend fun analyzeDreamPatterns(dreamTexts: List<String>): String = patternReport
}