package com.example.network

data class DreamMetadata(
    val tags: String,
    val title: String
)

data class ImageGenerationResult(
    val imageBytesBase64: String?,
    val fallbackUsed: Boolean = false
)