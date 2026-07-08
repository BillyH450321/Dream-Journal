package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyStore
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.Dream
import com.example.data.DreamRepository
import com.example.data.UsageQuotaSnapshot
import com.example.data.UsageQuotaStore
import com.example.network.ApiKeyProvider
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.Part
import com.example.utils.AudioPlayer
import com.example.utils.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

sealed interface RecordingState {
    object Idle : RecordingState
    object Recording : RecordingState
    data class Processing(val stage: String) : RecordingState
    data class Success(val dreamId: Long) : RecordingState
    data class Error(val message: String) : RecordingState
}

sealed interface PatternAnalysisState {
    object Idle : PatternAnalysisState
    object Loading : PatternAnalysisState
    data class Success(val report: String, val dreamCountAnalyzed: Int) : PatternAnalysisState
    data class Error(val message: String) : PatternAnalysisState
}

sealed interface ApiTestState {
    object Idle : ApiTestState
    object Loading : ApiTestState
    data class Result(val message: String, val success: Boolean) : ApiTestState
}

data class AudioPlaybackState(
    val dreamId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
)

class DreamJournalViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DreamJournalViewModel"
    private val context = application.applicationContext

    companion object {
        const val VOICE_DEFERRED_PLACEHOLDER =
            "Voice recording saved — tap Analyze Dream to transcribe and interpret."
    }

    private val repository: DreamRepository
    private val apiKeyStore = ApiKeyStore(context)
    private val usageQuotaStore = UsageQuotaStore(context)
    val allDreams: StateFlow<List<Dream>>

    private val _storedApiKey = MutableStateFlow("")
    val storedApiKey: StateFlow<String> = _storedApiKey.asStateFlow()

    private val _apiTestState = MutableStateFlow<ApiTestState>(ApiTestState.Idle)
    val apiTestState: StateFlow<ApiTestState> = _apiTestState.asStateFlow()

    private val _analyzeWithAiDefault = MutableStateFlow(false)
    val analyzeWithAiDefault: StateFlow<Boolean> = _analyzeWithAiDefault.asStateFlow()

    private val _usageQuota = MutableStateFlow(
        UsageQuotaSnapshot(isPro = false, usedThisMonth = 0)
    )
    val usageQuota: StateFlow<UsageQuotaSnapshot> = _usageQuota.asStateFlow()

    private val _paywallRequested = MutableStateFlow(false)
    val paywallRequested: StateFlow<Boolean> = _paywallRequested.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(context)
        repository = DreamRepository(database.dreamDao())
        allDreams = repository.allDreams
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        viewModelScope.launch {
            apiKeyStore.apiKey.collect { key ->
                _storedApiKey.value = key
                ApiKeyProvider.runtimeKey = key
            }
        }

        viewModelScope.launch {
            apiKeyStore.analyzeWithAiDefault.collect { enabled ->
                _analyzeWithAiDefault.value = enabled
            }
        }

        viewModelScope.launch {
            usageQuotaStore.usageSnapshot.collect { snapshot ->
                _usageQuota.value = snapshot
            }
        }
    }

    // --- State Variables ---
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _patternAnalysisState = MutableStateFlow<PatternAnalysisState>(PatternAnalysisState.Idle)
    val patternAnalysisState: StateFlow<PatternAnalysisState> = _patternAnalysisState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _selectedDreamId = MutableStateFlow<Long?>(null)
    val selectedDreamId: StateFlow<Long?> = _selectedDreamId.asStateFlow()

    // Observe currently selected dream dynamically
    val selectedDream: StateFlow<Dream?> = _selectedDreamId
        .flatMapLatest { id ->
            if (id != null) repository.getDreamById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Observe active chat history for the selected dream
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedDreamId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat sending state
    private val _isSendingChatMessage = MutableStateFlow(false)
    val isSendingChatMessage: StateFlow<Boolean> = _isSendingChatMessage.asStateFlow()

    private val audioRecorder = AudioRecorder(context)
    private val audioPlayer = AudioPlayer()
    private var timerJob: Job? = null
    private var positionUpdateJob: Job? = null
    private var recordedFile: File? = null

    private val _audioPlaybackState = MutableStateFlow(AudioPlaybackState())
    val audioPlaybackState: StateFlow<AudioPlaybackState> = _audioPlaybackState.asStateFlow()

    private val _analyzingDreamId = MutableStateFlow<Long?>(null)
    val analyzingDreamId: StateFlow<Long?> = _analyzingDreamId.asStateFlow()

    // --- Actions ---

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.saveApiKey(key)
            ApiKeyProvider.runtimeKey = key.trim()
        }
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _apiTestState.value = ApiTestState.Loading
            val result = GeminiClient.testConnection()
            val success = result.startsWith("Connection successful")
            _apiTestState.value = ApiTestState.Result(result, success)
        }
    }

    fun resetApiTestState() {
        _apiTestState.value = ApiTestState.Idle
    }

    fun setAnalyzeWithAiDefault(enabled: Boolean) {
        viewModelScope.launch {
            apiKeyStore.saveAnalyzeWithAiDefault(enabled)
        }
    }

    fun clearPaywallRequest() {
        _paywallRequested.value = false
    }

    private fun requestPaywall() {
        _paywallRequested.value = true
    }

    fun setProUserForTesting(enabled: Boolean) {
        viewModelScope.launch {
            usageQuotaStore.setProUser(enabled)
        }
    }

    private suspend fun beginAnalysisOrShowPaywall(): Boolean {
        if (usageQuotaStore.reserveAnalysisSlot()) {
            return true
        }
        requestPaywall()
        return false
    }

    private suspend fun requireProOrShowPaywall(): Boolean {
        if (_usageQuota.value.isPro) {
            return true
        }
        requestPaywall()
        return false
    }

    fun selectDream(dreamId: Long?) {
        if (_selectedDreamId.value != dreamId) {
            stopAudioPlayback()
        }
        _selectedDreamId.value = dreamId
    }

    fun startVoiceRecording() {
        recordedFile = null
        _recordingDuration.value = 0
        audioRecorder.startRecording(
            onStarted = { file ->
                recordedFile = file
                _recordingState.value = RecordingState.Recording
                startTimer()
            },
            onError = { error ->
                _recordingState.value = RecordingState.Error("Could not access microphone: ${error.localizedMessage}")
            }
        )
    }

    fun stopVoiceRecording(analyzeNow: Boolean = false) {
        stopTimer()
        val file = audioRecorder.stopRecording()
        if (file != null && file.exists() && file.length() > 0) {
            _recordingState.value = RecordingState.Processing("Saving voice recording...")
            processRecordedAudio(file, analyzeNow = analyzeNow)
        } else {
            _recordingState.value = RecordingState.Error("No audio was recorded.")
        }
    }

    fun cancelVoiceRecording() {
        stopTimer()
        audioRecorder.cancelRecording()
        _recordingState.value = RecordingState.Idle
    }

    fun deleteDream(dreamId: Long) {
        viewModelScope.launch {
            try {
                // If deleting currently viewed dream, reset selection
                if (_selectedDreamId.value == dreamId) {
                    _selectedDreamId.value = null
                }
                
                // Get the dream to delete its local files
                val dream = allDreams.value.find { it.id == dreamId }
                dream?.surrealImagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                dream?.audioPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }

                if (_audioPlaybackState.value.dreamId == dreamId) {
                    stopAudioPlayback()
                }

                repository.deleteDream(dreamId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting dream", e)
            }
        }
    }

    fun updateDreamTitle(dreamId: Long, title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        viewModelScope.launch {
            try {
                val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
                repository.updateDream(dream.copy(title = trimmedTitle))
            } catch (e: Exception) {
                Log.e(TAG, "Error updating dream title", e)
            }
        }
    }

    fun toggleAudioPlayback(dreamId: Long, audioPath: String) {
        val current = _audioPlaybackState.value
        if (current.dreamId == dreamId && current.isPlaying) {
            audioPlayer.pause()
            stopPositionUpdates()
            _audioPlaybackState.value = current.copy(isPlaying = false)
            return
        }

        if (current.dreamId == dreamId && audioPlayer.isPreparedFor(audioPath)) {
            audioPlayer.play()
            startPositionUpdates(dreamId)
            _audioPlaybackState.value = current.copy(isPlaying = true, error = null)
            return
        }

        viewModelScope.launch {
            try {
                stopAudioPlayback()
                val duration = withContext(Dispatchers.IO) {
                    audioPlayer.prepare(audioPath)
                }
                audioPlayer.play()
                _audioPlaybackState.value = AudioPlaybackState(
                    dreamId = dreamId,
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = duration,
                    error = null
                )
                startPositionUpdates(dreamId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play dream audio", e)
                _audioPlaybackState.value = AudioPlaybackState(
                    dreamId = dreamId,
                    isPlaying = false,
                    error = "Could not play recording: ${e.localizedMessage}"
                )
            }
        }
    }

    fun seekAudioPlayback(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
        _audioPlaybackState.value = _audioPlaybackState.value.copy(currentPositionMs = positionMs)
    }

    fun stopAudioPlayback() {
        stopPositionUpdates()
        audioPlayer.release()
        _audioPlaybackState.value = AudioPlaybackState()
    }

    private fun startPositionUpdates(dreamId: Long) {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                delay(250)
                val state = _audioPlaybackState.value
                if (state.dreamId != dreamId) break

                val position = audioPlayer.currentPosition()
                val duration = audioPlayer.duration().takeIf { it > 0 } ?: state.durationMs
                val playing = audioPlayer.isPlaying()

                if (!playing && position >= duration - 500 && duration > 0) {
                    audioPlayer.stop()
                    _audioPlaybackState.value = state.copy(
                        isPlaying = false,
                        currentPositionMs = 0,
                        durationMs = duration
                    )
                    break
                }

                _audioPlaybackState.value = state.copy(
                    isPlaying = playing,
                    currentPositionMs = position,
                    durationMs = duration
                )
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    override fun onCleared() {
        stopAudioPlayback()
        super.onCleared()
    }

    /**
     * Manually creates a dream with user entered text, skipping audio transcription
     */
    fun processManualDreamText(text: String, analyzeNow: Boolean = false) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _recordingState.value = RecordingState.Processing("Writing dream entry...")
            try {
                val id = repository.insertDream(
                    Dream(
                        rawText = text.trim(),
                        analysisStatus = if (analyzeNow) "pending" else "deferred",
                        artworkStatus = if (analyzeNow) "pending" else "deferred"
                    )
                )

                if (analyzeNow) {
                    if (!beginAnalysisOrShowPaywall()) {
                        _recordingState.value = RecordingState.Success(id)
                        _selectedDreamId.value = id
                        return@launch
                    }
                    runImageAndInterpretationPipeline(id, text.trim(), updateRecordingState = true)
                } else {
                    _recordingState.value = RecordingState.Success(id)
                    _selectedDreamId.value = id
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing manual dream text", e)
                _recordingState.value = RecordingState.Error("Failed to save entry: ${e.localizedMessage}")
            }
        }
    }

    fun analyzeDream(dreamId: Long) {
        viewModelScope.launch {
            val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
            if (dream.analysisStatus == "pending" || dream.analysisStatus == "complete") return@launch

            if (!beginAnalysisOrShowPaywall()) {
                return@launch
            }

            _analyzingDreamId.value = dreamId
            repository.updateDream(
                dream.copy(analysisStatus = "pending", artworkStatus = "pending")
            )

            try {
                var dreamText = dream.rawText

                if (!dream.audioPath.isNullOrBlank() && dream.rawText == VOICE_DEFERRED_PLACEHOLDER) {
                    val audioFile = File(dream.audioPath)
                    if (!audioFile.exists()) {
                        repository.updateDream(
                            dream.copy(analysisStatus = "failed", artworkStatus = "failed")
                        )
                        _analyzingDreamId.value = null
                        return@launch
                    }

                    val base64Audio = withContext(Dispatchers.IO) {
                        Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
                    }
                    val transcription = GeminiClient.transcribeAudio(base64Audio, "audio/mp4")
                    if (transcription.contains("API Key Error") ||
                        transcription.contains("Transcription failed") ||
                        transcription.contains("rate limit", ignoreCase = true) ||
                        transcription.contains("HTTP 429")
                    ) {
                        repository.updateDream(
                            dream.copy(analysisStatus = "failed", artworkStatus = "failed")
                        )
                        _analyzingDreamId.value = null
                        return@launch
                    }
                    dreamText = transcription
                    repository.updateDream(
                        dream.copy(rawText = transcription, analysisStatus = "pending")
                    )
                }

                runImageAndInterpretationPipeline(
                    id = dreamId,
                    transcription = dreamText,
                    updateRecordingState = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze dream", e)
                val current = allDreams.value.find { it.id == dreamId }
                if (current != null) {
                    repository.updateDream(
                        current.copy(analysisStatus = "failed", artworkStatus = "failed")
                    )
                }
            } finally {
                _analyzingDreamId.value = null
            }
        }
    }

    /**
     * Sends follow-up message to chat
     */
    fun sendChatMessage(question: String) {
        val dreamId = _selectedDreamId.value ?: return
        val dream = selectedDream.value ?: return
        if (question.isBlank() || _isSendingChatMessage.value) return

        viewModelScope.launch {
            if (!requireProOrShowPaywall()) {
                return@launch
            }

            _isSendingChatMessage.value = true
            try {
                // 1. Save user's chat message to DB
                val userMsg = ChatMessage(dreamId = dreamId, isUser = true, text = question)
                repository.insertChatMessage(userMsg)

                // 2. Fetch all previous messages from current state to construct turn-by-turn history
                val rawMessages = chatMessages.value
                val formattedHistory = rawMessages.map { msg ->
                    Content(
                        role = if (msg.isUser) "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                // 3. Ask Gemini Client for answer
                val answer = GeminiClient.askFollowUpQuestion(dream.rawText, formattedHistory, question)

                // 4. Save AI's response to DB
                val assistantMsg = ChatMessage(dreamId = dreamId, isUser = false, text = answer)
                repository.insertChatMessage(assistantMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Chat request failed", e)
                val errorMsg = ChatMessage(dreamId = dreamId, isUser = false, text = "I encountered an error analyzing your question: ${e.localizedMessage}")
                repository.insertChatMessage(errorMsg)
            } finally {
                _isSendingChatMessage.value = false
            }
        }
    }

    // --- Helpers ---

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun processRecordedAudio(file: File, analyzeNow: Boolean) {
        viewModelScope.launch {
            try {
                val stableAudioFile = withContext(Dispatchers.IO) {
                    val dest = File(context.filesDir, "dream_audio_${System.currentTimeMillis()}.m4a")
                    file.copyTo(dest, overwrite = true)
                    file.delete()
                    dest
                }

                if (!analyzeNow) {
                    val id = repository.insertDream(
                        Dream(
                            rawText = VOICE_DEFERRED_PLACEHOLDER,
                            audioPath = stableAudioFile.absolutePath,
                            title = "Voice Dream",
                            analysisStatus = "deferred",
                            artworkStatus = "deferred"
                        )
                    )
                    _recordingState.value = RecordingState.Success(id)
                    _selectedDreamId.value = id
                    return@launch
                }

                if (!beginAnalysisOrShowPaywall()) {
                    val id = repository.insertDream(
                        Dream(
                            rawText = VOICE_DEFERRED_PLACEHOLDER,
                            audioPath = stableAudioFile.absolutePath,
                            title = "Voice Dream",
                            analysisStatus = "deferred",
                            artworkStatus = "deferred"
                        )
                    )
                    _recordingState.value = RecordingState.Success(id)
                    _selectedDreamId.value = id
                    return@launch
                }

                _recordingState.value = RecordingState.Processing("Transcribing dream recording...")

                val base64Audio = withContext(Dispatchers.IO) {
                    Base64.encodeToString(stableAudioFile.readBytes(), Base64.NO_WRAP)
                }

                val transcription = GeminiClient.transcribeAudio(base64Audio, "audio/mp4")

                if (transcription.contains("API Key Error") ||
                    transcription.contains("Transcription failed") ||
                    transcription.contains("rate limit", ignoreCase = true) ||
                    transcription.contains("HTTP 429")
                ) {
                    _recordingState.value = RecordingState.Error(transcription)
                    return@launch
                }

                val id = repository.insertDream(
                    Dream(
                        rawText = transcription,
                        audioPath = stableAudioFile.absolutePath,
                        analysisStatus = "pending",
                        artworkStatus = "pending"
                    )
                )

                runImageAndInterpretationPipeline(id, transcription, updateRecordingState = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process audio recording", e)
                _recordingState.value = RecordingState.Error("Failed to save dream: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runImageAndInterpretationPipeline(
        id: Long,
        transcription: String,
        updateRecordingState: Boolean = true
    ) {
        try {
            if (updateRecordingState) {
                _recordingState.value = RecordingState.Processing("Analyzing archetypes & symbols...")
            }

            var loadedDream = repository.allDreams.first().find { it.id == id }
            if (loadedDream != null) {
                repository.updateDream(
                    loadedDream.copy(analysisStatus = "pending", artworkStatus = "pending")
                )
            }

            val interpretation = GeminiClient.interpretDream(transcription)
            if (interpretation.contains("HTTP 429") || interpretation.contains("rate limit", ignoreCase = true)) {
                if (updateRecordingState) {
                    _recordingState.value = RecordingState.Error(interpretation)
                }
                loadedDream = repository.allDreams.first().find { it.id == id }
                if (loadedDream != null) {
                    repository.updateDream(
                        loadedDream.copy(analysisStatus = "failed", artworkStatus = "failed")
                    )
                }
                return
            }

            if (updateRecordingState) {
                _recordingState.value = RecordingState.Processing("Generating title & tags...")
            }
            val metadata = GeminiClient.suggestDreamMetadata(transcription)

            val firstLines = interpretation.lines().filter { it.isNotBlank() }
            val emotionalTheme = firstLines.firstOrNull { !it.startsWith("#") } ?: "Jungian Dream Analysis"

            loadedDream = repository.allDreams.first().find { it.id == id }
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

            if (updateRecordingState) {
                _recordingState.value = RecordingState.Success(id)
                _selectedDreamId.value = id
            }

            viewModelScope.launch {
                try {
                    delay(5_000)
                    val result = GeminiClient.generateSurrealImage(transcription)
                    var savedImagePath: String? = null
                    if (result.imageBytesBase64 != null) {
                        savedImagePath = withContext(Dispatchers.IO) {
                            saveBase64ToLocalFile(result.imageBytesBase64, id)
                        }
                    }

                    val currentDream = repository.allDreams.first().find { it.id == id }
                    if (currentDream != null) {
                        repository.updateDream(
                            currentDream.copy(
                                surrealImagePath = savedImagePath,
                                artworkStatus = if (savedImagePath != null) "complete" else "failed",
                                artworkFallbackUsed = result.fallbackUsed
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Image generation background task failed", e)
                    val currentDream = repository.allDreams.first().find { it.id == id }
                    if (currentDream != null) {
                        repository.updateDream(currentDream.copy(artworkStatus = "failed"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dream analysis pipeline failed", e)
            val loadedDream = repository.allDreams.first().find { it.id == id }
            if (loadedDream != null) {
                repository.updateDream(
                    loadedDream.copy(analysisStatus = "failed", artworkStatus = "failed")
                )
            }
            if (updateRecordingState) {
                _recordingState.value = RecordingState.Error(
                    "Dream analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private fun saveBase64ToLocalFile(base64Str: String, dreamId: Long): String? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val file = File(context.filesDir, "dream_img_${dreamId}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64 to file", e)
            null
        }
    }

    fun resetRecordingState() {
        _recordingState.value = RecordingState.Idle
        _recordingDuration.value = 0
    }

    /**
     * Adds a custom tag to a specified dream entry.
     */
    fun addTagToDream(dreamId: Long, tag: String) {
        val trimmedTag = tag.trim().lowercase()
        if (trimmedTag.isBlank()) return
        viewModelScope.launch {
            try {
                val dreams = allDreams.value
                val dream = dreams.find { it.id == dreamId }
                if (dream != null) {
                    val currentTagsList = if (dream.tags.isBlank()) emptyList() else dream.tags.split(",").map { it.trim() }
                    if (!currentTagsList.contains(trimmedTag)) {
                        val newTagsList = currentTagsList + trimmedTag
                        val updatedDream = dream.copy(tags = newTagsList.joinToString(","))
                        repository.updateDream(updatedDream)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding tag to dream", e)
            }
        }
    }

    /**
     * Removes a custom tag from a specified dream entry.
     */
    fun removeTagFromDream(dreamId: Long, tag: String) {
        val trimmedTag = tag.trim().lowercase()
        viewModelScope.launch {
            try {
                val dreams = allDreams.value
                val dream = dreams.find { it.id == dreamId }
                if (dream != null) {
                    val currentTagsList = if (dream.tags.isBlank()) emptyList() else dream.tags.split(",").map { it.trim() }
                    if (currentTagsList.contains(trimmedTag)) {
                        val newTagsList = currentTagsList - trimmedTag
                        val updatedDream = dream.copy(tags = newTagsList.joinToString(","))
                        repository.updateDream(updatedDream)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing tag from dream", e)
            }
        }
    }

    /**
     * Regenerates the dream artwork using the two-stage pipeline.
     */
    fun regenerateDreamArtwork(dreamId: Long) {
        viewModelScope.launch {
            if (!requireProOrShowPaywall()) {
                return@launch
            }

            val dream = allDreams.value.find { it.id == dreamId }
            if (dream != null) {
                try {
                    // Set status to pending
                    repository.updateDream(dream.copy(artworkStatus = "pending"))
                    
                    val result = GeminiClient.generateSurrealImage(dream.rawText)
                    var savedImagePath: String? = null
                    if (result.imageBytesBase64 != null) {
                        savedImagePath = withContext(Dispatchers.IO) {
                            saveBase64ToLocalFile(result.imageBytesBase64, dream.id)
                        }
                    }
                    
                    val currentDream = repository.allDreams.first().find { it.id == dreamId }
                    if (currentDream != null) {
                        repository.updateDream(
                            currentDream.copy(
                                surrealImagePath = savedImagePath,
                                artworkStatus = if (savedImagePath != null) "complete" else "failed",
                                artworkFallbackUsed = result.fallbackUsed
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Manual artwork regeneration failed", e)
                    val currentDream = repository.allDreams.first().find { it.id == dreamId }
                    if (currentDream != null) {
                        repository.updateDream(currentDream.copy(artworkStatus = "failed"))
                    }
                }
            }
        }
    }

    /**
     * Loads the last cached pattern analysis report if available, otherwise sets to Idle.
     */
    fun loadPatternAnalysis() {
        val prefs = context.getSharedPreferences("dream_analysis_prefs", Context.MODE_PRIVATE)
        val cachedReport = prefs.getString("cached_pattern_report", null)
        val cachedCount = prefs.getInt("cached_dream_count", 0)

        if (cachedReport != null) {
            _patternAnalysisState.value = PatternAnalysisState.Success(cachedReport, cachedCount)
        } else {
            _patternAnalysisState.value = PatternAnalysisState.Idle
        }
    }

    /**
     * Triggers a new subconscious pattern analysis using the Gemini API for all recorded dreams.
     */
    fun generatePatternAnalysis() {
        viewModelScope.launch {
            if (!requireProOrShowPaywall()) {
                return@launch
            }

            val dreams = allDreams.value
            if (dreams.isEmpty()) {
                _patternAnalysisState.value = PatternAnalysisState.Error("No dreams have been recorded yet. Please capture some dreams first!")
                return@launch
            }

            _patternAnalysisState.value = PatternAnalysisState.Loading

            try {
                // Compile raw texts of all recorded dreams
                val dreamTexts = dreams.map { dream ->
                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dream.timestamp))
                    "[$dateStr]\n${dream.rawText}"
                }

                // Call Gemini API through the GeminiClient
                val report = GeminiClient.analyzeDreamPatterns(dreamTexts)

                if (report.contains("API Key Error") || report.contains("Analysis failed")) {
                    _patternAnalysisState.value = PatternAnalysisState.Error(report)
                } else {
                    // Cache the successful report in SharedPreferences
                    val prefs = context.getSharedPreferences("dream_analysis_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("cached_pattern_report", report)
                        .putInt("cached_dream_count", dreams.size)
                        .apply()

                    _patternAnalysisState.value = PatternAnalysisState.Success(report, dreams.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating pattern analysis", e)
                _patternAnalysisState.value = PatternAnalysisState.Error("Failed to analyze patterns: ${e.localizedMessage}")
            }
        }
    }
}
