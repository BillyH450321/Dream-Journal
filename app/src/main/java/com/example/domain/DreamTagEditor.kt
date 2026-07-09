package com.example.domain

import com.example.data.Dream
import com.example.data.DreamRepository

class DreamTagEditor(private val repository: DreamRepository) {
    suspend fun addTag(dream: Dream, tag: String): Boolean {
        val trimmedTag = tag.trim().lowercase()
        if (trimmedTag.isBlank()) return false

        val currentTags = dream.tags
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()

        if (currentTags.contains(trimmedTag)) return false

        repository.updateDream(dream.copy(tags = (currentTags + trimmedTag).joinToString(",")))
        return true
    }

    suspend fun removeTag(dream: Dream, tag: String): Boolean {
        val trimmedTag = tag.trim().lowercase()
        val currentTags = dream.tags
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()

        if (!currentTags.contains(trimmedTag)) return false

        repository.updateDream(dream.copy(tags = (currentTags - trimmedTag).joinToString(",")))
        return true
    }
}