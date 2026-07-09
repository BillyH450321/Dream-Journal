package com.example.domain

import android.util.Base64
import android.util.Log
import com.example.data.Dream
import com.example.data.DreamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class AnalysisStage {
    data class Completed(val dreamId: Long) : AnalysisStage()
    data class Failed(val message: String?, val refundQuota: Boolean) : AnalysisStage()
}

class DreamAnalysisPipeline(
    private val repository: DreamRepository,
    private val fileStorage: DreamFileStorage,
    private val backgroundScope: CoroutineScope,
    private val aiClient: DreamAiClient
) {
    private val tag = "DreamAnalysisPipeline"

    suspend fun run(
        dreamId: Long,
        transcription: String,
        onProgress: ((String) -> Unit)? = null
    ): AnalysisStage {
        try {
            onProgress?.invoke("Analyzing archetypes & symbols...")

            var loadedDream = repository.allDreams.first().find { it.id == dreamId }
            if (loadedDream != null) {
                repository.updateDream(
                    loadedDream.copy(analysisStatus = "pending", artworkStatus = "pending")
                )
            }

            val interpretation = aiClient.interpretDream(transcription)
            if (GeminiResponseValidator.isRateLimited(interpretation)) {
                markAnalysisFailed(dreamId)
                return AnalysisStage.Failed(interpretation, refundQuota = true)
            }

            onProgress?.invoke("Generating title & tags...")
            val metadata = aiClient.suggestDreamMetadata(transcription)

            val firstLines = interpretation.lines().filter { it.isNotBlank() }
            val emotionalTheme = firstLines.firstOrNull { !it.startsWith("#") }
                ?: "Jungian Dream Analysis"

            loadedDream = repository.allDreams.first().find { it.id == dreamId }
            if (loadedDream != null) {
                repository.updateDream(
                    loadedDream.copy(
                        title = metadata.title,
                        emotionalTheme = emotionalTheme,
                        structuredInterpretation = interpretation,
                        tags = metadata.tags,
                        analysisStatus = "complete"
                    )
                )
            }

            scheduleArtworkGeneration(dreamId, transcription)
            return AnalysisStage.Completed(dreamId)
        } catch (e: Exception) {
            Log.e(tag, "Dream analysis pipeline failed", e)
            markAnalysisFailed(dreamId)
            return AnalysisStage.Failed(
                message = "Dream analysis failed: ${e.localizedMessage ?: "Unknown error"}",
                refundQuota = true
            )
        }
    }

    suspend fun transcribeDeferredVoiceDream(dream: Dream): String? {
        val audioPath = dream.audioPath ?: return null
        val audioFile = File(audioPath)
        if (!audioFile.exists()) return null

        val base64Audio = withContext(Dispatchers.IO) {
            Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
        }
        val transcription = aiClient.transcribeAudio(base64Audio, "audio/mp4")
        if (GeminiResponseValidator.isTranscriptionFailure(transcription)) {
            return null
        }
        return transcription
    }

    suspend fun transcribeRecordingFile(audioFile: File): String {
        val base64Audio = withContext(Dispatchers.IO) {
            Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
        }
        return aiClient.transcribeAudio(base64Audio, "audio/mp4")
    }

    suspend fun regenerateArtwork(dreamId: Long, rawText: String) {
        val dream = repository.allDreams.first().find { it.id == dreamId } ?: return
        try {
            repository.updateDream(dream.copy(artworkStatus = "pending"))
            val result = aiClient.generateSurrealImage(rawText)
            applyArtworkResult(dreamId, result.imageBytesBase64, result.fallbackUsed)
        } catch (e: Exception) {
            Log.e(tag, "Manual artwork regeneration failed", e)
            val currentDream = repository.allDreams.first().find { it.id == dreamId }
            if (currentDream != null) {
                repository.updateDream(currentDream.copy(artworkStatus = "failed"))
            }
        }
    }

    private fun scheduleArtworkGeneration(dreamId: Long, transcription: String) {
        backgroundScope.launch {
            try {
                delay(5_000)
                val result = aiClient.generateSurrealImage(transcription)
                applyArtworkResult(dreamId, result.imageBytesBase64, result.fallbackUsed)
            } catch (e: Exception) {
                Log.e(tag, "Image generation background task failed", e)
                val currentDream = repository.allDreams.first().find { it.id == dreamId }
                if (currentDream != null) {
                    repository.updateDream(currentDream.copy(artworkStatus = "failed"))
                }
            }
        }
    }

    private suspend fun applyArtworkResult(
        dreamId: Long,
        imageBytesBase64: String?,
        fallbackUsed: Boolean
    ) {
        var savedImagePath: String? = null
        if (imageBytesBase64 != null) {
            savedImagePath = withContext(Dispatchers.IO) {
                fileStorage.saveDreamImage(imageBytesBase64, dreamId)
            }
        }

        val currentDream = repository.allDreams.first().find { it.id == dreamId }
        if (currentDream != null) {
            repository.updateDream(
                currentDream.copy(
                    surrealImagePath = savedImagePath,
                    artworkStatus = if (savedImagePath != null) "complete" else "failed",
                    artworkFallbackUsed = fallbackUsed
                )
            )
        }
    }

    private suspend fun markAnalysisFailed(dreamId: Long) {
        val loadedDream = repository.allDreams.first().find { it.id == dreamId }
        if (loadedDream != null) {
            repository.updateDream(
                loadedDream.copy(analysisStatus = "failed", artworkStatus = "failed")
            )
        }
    }
}