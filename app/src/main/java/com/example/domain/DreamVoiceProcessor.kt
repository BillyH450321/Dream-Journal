package com.example.domain

import com.example.data.Dream
import com.example.data.DreamRepository
import java.io.File

object DreamVoiceConstants {
    const val DEFERRED_PLACEHOLDER =
        "Voice recording saved — tap Analyze Dream to transcribe and interpret."
    const val DEFAULT_TITLE = "Voice Dream"
}

class DreamVoiceProcessor(
    private val repository: DreamRepository,
    private val fileStorage: DreamFileStorage,
    private val analysisPipeline: DreamAnalysisPipeline
) {
    suspend fun saveDeferredRecording(sourceFile: File): Long {
        val stableAudioFile = fileStorage.persistRecording(sourceFile)
        return repository.insertDream(
            Dream(
                rawText = DreamVoiceConstants.DEFERRED_PLACEHOLDER,
                audioPath = stableAudioFile.absolutePath,
                title = DreamVoiceConstants.DEFAULT_TITLE,
                analysisStatus = "deferred",
                artworkStatus = "deferred"
            )
        )
    }

    suspend fun saveTranscribedRecording(sourceFile: File, transcription: String): Long {
        return repository.insertDream(
            Dream(
                rawText = transcription,
                audioPath = sourceFile.absolutePath,
                analysisStatus = "pending",
                artworkStatus = "pending"
            )
        )
    }

    data class TranscriptionResult(
        val transcription: String,
        val audioFile: File
    )

    suspend fun transcribeAndPersist(sourceFile: File): TranscriptionResult {
        val stableAudioFile = fileStorage.persistRecording(sourceFile)
        val transcription = analysisPipeline.transcribeRecordingFile(stableAudioFile)
        return TranscriptionResult(transcription, stableAudioFile)
    }
}