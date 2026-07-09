package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyStore
import com.example.data.AppDatabase
import com.example.data.Dream
import com.example.data.DreamRepository
import com.example.data.UsageQuotaSnapshot
import com.example.data.UsageQuotaStore
import com.example.domain.AnalysisQuotaGate
import com.example.domain.AnalysisStage
import com.example.domain.DreamAnalysisPipeline
import com.example.domain.DreamChatService
import com.example.domain.DreamFileStorage
import com.example.domain.DreamPlaybackController
import com.example.domain.DreamTagEditor
import com.example.domain.DreamVoiceConstants
import com.example.domain.DreamVoiceProcessor
import com.example.domain.GeminiResponseValidator
import com.example.domain.PatternAnalysisService
import com.example.domain.DreamAiClient
import com.example.network.ApiKeyProvider
import com.example.network.GeminiDreamAiClient
import com.example.utils.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DreamJournalViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "DreamJournalViewModel"
    private val context = application.applicationContext

    private val repository = DreamRepository(
        AppDatabase.getDatabase(context).dreamDao()
    )
    private val aiClient: DreamAiClient = GeminiDreamAiClient()
    private val apiKeyStore = ApiKeyStore(context)
    private val usageQuotaStore = UsageQuotaStore(context)
    private val fileStorage = DreamFileStorage(context.filesDir)
    private val analysisPipeline = DreamAnalysisPipeline(
        repository, fileStorage, viewModelScope, aiClient
    )
    private val voiceProcessor = DreamVoiceProcessor(repository, fileStorage, analysisPipeline)

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

    private val quotaGate = AnalysisQuotaGate(usageQuotaStore) { _paywallRequested.value = true }
    private val chatService = DreamChatService(repository, aiClient)
    private val tagEditor = DreamTagEditor(repository)
    private val patternAnalysisService = PatternAnalysisService(context, aiClient)
    private val playbackController = DreamPlaybackController(viewModelScope)

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _patternAnalysisState = MutableStateFlow<PatternAnalysisState>(PatternAnalysisState.Idle)
    val patternAnalysisState: StateFlow<PatternAnalysisState> = _patternAnalysisState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _selectedDreamId = MutableStateFlow<Long?>(null)
    val selectedDreamId: StateFlow<Long?> = _selectedDreamId.asStateFlow()

    val selectedDream: StateFlow<Dream?> = _selectedDreamId
        .flatMapLatest { id ->
            if (id != null) repository.getDreamById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatMessages: StateFlow<List<com.example.data.ChatMessage>> = _selectedDreamId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSendingChatMessage = MutableStateFlow(false)
    val isSendingChatMessage: StateFlow<Boolean> = _isSendingChatMessage.asStateFlow()

    val audioPlaybackState: StateFlow<AudioPlaybackState> = playbackController.state

    private val _analyzingDreamId = MutableStateFlow<Long?>(null)
    val analyzingDreamId: StateFlow<Long?> = _analyzingDreamId.asStateFlow()

    private val audioRecorder = AudioRecorder(context)
    private var timerJob: Job? = null

    init {
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

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.saveApiKey(key)
            ApiKeyProvider.runtimeKey = key.trim()
        }
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _apiTestState.value = ApiTestState.Loading
            val result = aiClient.testConnection()
            _apiTestState.value = ApiTestState.Result(
                message = result,
                success = result.startsWith("Connection successful")
            )
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

    fun setProUserForTesting(enabled: Boolean) {
        viewModelScope.launch {
            usageQuotaStore.setProUser(enabled)
        }
    }

    fun selectDream(dreamId: Long?) {
        if (_selectedDreamId.value != dreamId) {
            playbackController.stop()
        }
        _selectedDreamId.value = dreamId
    }

    fun startVoiceRecording() {
        _recordingDuration.value = 0
        audioRecorder.startRecording(
            onStarted = {
                _recordingState.value = RecordingState.Recording
                startTimer()
            },
            onError = { error ->
                _recordingState.value = RecordingState.Error(
                    "Could not access microphone: ${error.localizedMessage}"
                )
            }
        )
    }

    fun stopVoiceRecording(analyzeNow: Boolean = false) {
        stopTimer()
        val file = audioRecorder.stopRecording()
        if (file != null && file.exists() && file.length() > 0) {
            _recordingState.value = RecordingState.Processing("Saving voice recording...")
            processRecordedAudio(file, analyzeNow)
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
                if (_selectedDreamId.value == dreamId) {
                    _selectedDreamId.value = null
                }
                allDreams.value.find { it.id == dreamId }?.let { dream ->
                    fileStorage.deleteDreamAssets(dream)
                }
                if (playbackController.isPlayingDream(dreamId)) {
                    playbackController.stop()
                }
                repository.deleteDream(dreamId)
            } catch (e: Exception) {
                Log.e(tag, "Error deleting dream", e)
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
                Log.e(tag, "Error updating dream title", e)
            }
        }
    }

    fun toggleAudioPlayback(dreamId: Long, audioPath: String) {
        playbackController.toggle(dreamId, audioPath)
    }

    fun seekAudioPlayback(positionMs: Int) {
        playbackController.seek(positionMs)
    }

    fun stopAudioPlayback() {
        playbackController.stop()
    }

    override fun onCleared() {
        playbackController.stop()
        super.onCleared()
    }

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
                    if (!quotaGate.reserveAnalysisSlot()) {
                        finishRecordingSuccess(id)
                        return@launch
                    }
                    runAnalysis(id, text.trim(), updateRecordingState = true)
                } else {
                    finishRecordingSuccess(id)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing manual dream text", e)
                _recordingState.value = RecordingState.Error("Failed to save entry: ${e.localizedMessage}")
            }
        }
    }

    fun analyzeDream(dreamId: Long) {
        viewModelScope.launch {
            val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
            if (dream.analysisStatus == "pending" || dream.analysisStatus == "complete") return@launch
            if (!quotaGate.reserveAnalysisSlot()) return@launch

            _analyzingDreamId.value = dreamId
            repository.updateDream(
                dream.copy(analysisStatus = "pending", artworkStatus = "pending")
            )

            try {
                var dreamText = dream.rawText
                if (!dream.audioPath.isNullOrBlank() &&
                    dream.rawText == DreamVoiceConstants.DEFERRED_PLACEHOLDER
                ) {
                    val transcription = analysisPipeline.transcribeDeferredVoiceDream(dream)
                    if (transcription == null) {
                        repository.updateDream(
                            dream.copy(analysisStatus = "failed", artworkStatus = "failed")
                        )
                        quotaGate.releaseAnalysisSlot()
                        return@launch
                    }
                    dreamText = transcription
                    repository.updateDream(
                        dream.copy(rawText = transcription, analysisStatus = "pending")
                    )
                }
                runAnalysis(dreamId, dreamText, updateRecordingState = false)
            } catch (e: Exception) {
                Log.e(tag, "Failed to analyze dream", e)
                allDreams.value.find { it.id == dreamId }?.let { current ->
                    repository.updateDream(
                        current.copy(analysisStatus = "failed", artworkStatus = "failed")
                    )
                }
                quotaGate.releaseAnalysisSlot()
            } finally {
                _analyzingDreamId.value = null
            }
        }
    }

    fun sendChatMessage(question: String) {
        val dreamId = _selectedDreamId.value ?: return
        val dream = selectedDream.value ?: return
        if (question.isBlank() || _isSendingChatMessage.value) return

        viewModelScope.launch {
            if (!quotaGate.requirePro()) return@launch
            _isSendingChatMessage.value = true
            try {
                chatService.sendMessage(dreamId, dream.rawText, chatMessages.value, question)
            } finally {
                _isSendingChatMessage.value = false
            }
        }
    }

    fun resetRecordingState() {
        _recordingState.value = RecordingState.Idle
        _recordingDuration.value = 0
    }

    fun addTagToDream(dreamId: Long, tag: String) {
        viewModelScope.launch {
            try {
                val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
                tagEditor.addTag(dream, tag)
            } catch (e: Exception) {
                Log.e(tag, "Error adding tag to dream", e)
            }
        }
    }

    fun removeTagFromDream(dreamId: Long, tag: String) {
        viewModelScope.launch {
            try {
                val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
                tagEditor.removeTag(dream, tag)
            } catch (e: Exception) {
                Log.e(tag, "Error removing tag from dream", e)
            }
        }
    }

    fun regenerateDreamArtwork(dreamId: Long) {
        viewModelScope.launch {
            if (!quotaGate.requirePro()) return@launch
            val dream = allDreams.value.find { it.id == dreamId } ?: return@launch
            analysisPipeline.regenerateArtwork(dreamId, dream.rawText)
        }
    }

    fun loadPatternAnalysis() {
        _patternAnalysisState.value = patternAnalysisService.loadCached()
    }

    fun generatePatternAnalysis() {
        viewModelScope.launch {
            if (!quotaGate.requirePro()) return@launch
            _patternAnalysisState.value = PatternAnalysisState.Loading
            _patternAnalysisState.value = patternAnalysisService.generate(allDreams.value)
        }
    }

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
                if (!analyzeNow) {
                    val id = voiceProcessor.saveDeferredRecording(file)
                    finishRecordingSuccess(id)
                    return@launch
                }

                if (!quotaGate.reserveAnalysisSlot()) {
                    val id = voiceProcessor.saveDeferredRecording(file)
                    finishRecordingSuccess(id)
                    return@launch
                }

                _recordingState.value = RecordingState.Processing("Transcribing dream recording...")
                val result = voiceProcessor.transcribeAndPersist(file)
                if (GeminiResponseValidator.isTranscriptionFailure(result.transcription)) {
                    quotaGate.releaseAnalysisSlot()
                    _recordingState.value = RecordingState.Error(result.transcription)
                    return@launch
                }

                val id = voiceProcessor.saveTranscribedRecording(
                    result.audioFile,
                    result.transcription
                )
                runAnalysis(id, result.transcription, updateRecordingState = true)
            } catch (e: Exception) {
                Log.e(tag, "Failed to process audio recording", e)
                _recordingState.value = RecordingState.Error("Failed to save dream: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runAnalysis(
        dreamId: Long,
        transcription: String,
        updateRecordingState: Boolean
    ) {
        val result = analysisPipeline.run(dreamId, transcription) { message ->
            if (updateRecordingState) {
                _recordingState.value = RecordingState.Processing(message)
            }
        }

        when (result) {
            is AnalysisStage.Completed -> {
                if (updateRecordingState) {
                    finishRecordingSuccess(result.dreamId)
                }
            }
            is AnalysisStage.Failed -> {
                if (result.refundQuota) {
                    quotaGate.releaseAnalysisSlot()
                }
                if (updateRecordingState) {
                    _recordingState.value = RecordingState.Error(
                        result.message ?: "Dream analysis failed"
                    )
                }
            }
        }
    }

    private fun finishRecordingSuccess(dreamId: Long) {
        _recordingState.value = RecordingState.Success(dreamId)
        _selectedDreamId.value = dreamId
    }
}