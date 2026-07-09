package com.example.domain

import com.example.data.ChatMessage
import com.example.data.DreamRepository
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.Part

class DreamChatService(private val repository: DreamRepository) {
    suspend fun sendMessage(
        dreamId: Long,
        dreamText: String,
        history: List<ChatMessage>,
        question: String
    ) {
        val userMsg = ChatMessage(dreamId = dreamId, isUser = true, text = question)
        repository.insertChatMessage(userMsg)

        try {
            val formattedHistory = history.map { msg ->
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            }
            val answer = GeminiClient.askFollowUpQuestion(dreamText, formattedHistory, question)
            repository.insertChatMessage(
                ChatMessage(dreamId = dreamId, isUser = false, text = answer)
            )
        } catch (e: Exception) {
            repository.insertChatMessage(
                ChatMessage(
                    dreamId = dreamId,
                    isUser = false,
                    text = "I encountered an error analyzing your question: ${e.localizedMessage}"
                )
            )
        }
    }
}