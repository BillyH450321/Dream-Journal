package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.Dream
import com.example.data.DreamRepository
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.Part
import com.example.utils.AudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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

class DreamJournalViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DreamJournalViewModel"
    private val context = application.applicationContext

    private val repository: DreamRepository
    val allDreams: StateFlow<List<Dream>>

    init {
        val database = AppDatabase.getDatabase(context)
        repository = DreamRepository(database.dreamDao())
        allDreams = repository.allDreams
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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
    private var timerJob: Job? = null
    private var recordedFile: File? = null

    // --- Actions ---

    fun selectDream(dreamId: Long?) {
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

    fun stopVoiceRecording() {
        stopTimer()
        val file = audioRecorder.stopRecording()
        if (file != null && file.exists() && file.length() > 0) {
            _recordingState.value = RecordingState.Processing("Saving voice recording...")
            processRecordedAudio(file)
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

                repository.deleteDream(dreamId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting dream", e)
            }
        }
    }

    /**
     * Manually creates a dream with user entered text, skipping audio transcription
     */
    fun processManualDreamText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _recordingState.value = RecordingState.Processing("Writing dream entry...")
            try {
                val newDream = Dream(rawText = text)
                val id = repository.insertDream(newDream)
                
                // Execute image generation & interpretation
                runImageAndInterpretationPipeline(id, text)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing manual dream text", e)
                _recordingState.value = RecordingState.Error("Failed to save entry: ${e.localizedMessage}")
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

    private fun processRecordedAudio(file: File) {
        viewModelScope.launch {
            try {
                _recordingState.value = RecordingState.Processing("Transcribing dream recording...")

                // 1. Load audio bytes and convert to Base64
                val base64Audio = withContext(Dispatchers.IO) {
                    val bytes = file.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }

                // 2. Transcribe using Gemini API
                val transcription = GeminiClient.transcribeAudio(base64Audio, "audio/mp4")

                if (transcription.contains("API Key Error") || transcription.contains("Transcription failed")) {
                    _recordingState.value = RecordingState.Error(transcription)
                    return@launch
                }

                _recordingState.value = RecordingState.Processing("Saving entry...")

                // 3. Copy recorded file from cache to a stable location inside app files directory
                val stableAudioFile = withContext(Dispatchers.IO) {
                    val dest = File(context.filesDir, "dream_audio_${System.currentTimeMillis()}.m4a")
                    file.copyTo(dest, overwrite = true)
                    file.delete() // clean up cache file
                    dest
                }

                // 4. Create initial Dream record in database
                val dream = Dream(
                    rawText = transcription,
                    audioPath = stableAudioFile.absolutePath
                )
                val id = repository.insertDream(dream)

                // 5. Trigger the surreal image + archetypal interpretation pipeline
                runImageAndInterpretationPipeline(id, transcription)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process audio recording", e)
                _recordingState.value = RecordingState.Error("Failed to transcribe dream: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runImageAndInterpretationPipeline(id: Long, transcription: String) {
        _recordingState.value = RecordingState.Processing("Analyzing archetypes & symbols...")
        
        // Mark artwork as pending
        var loadedDream = repository.allDreams.first().find { it.id == id }
        if (loadedDream != null) {
            repository.updateDream(loadedDream.copy(artworkStatus = "pending"))
        }

        // Launch image generation in parallel
        viewModelScope.launch {
            try {
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

        // 2. Structured Jungian interpretation
        val interpretation = GeminiClient.interpretDream(transcription)

        // Extract a simple emotional theme or summary
        val firstLines = interpretation.lines().filter { it.isNotBlank() }
        val emotionalTheme = firstLines.firstOrNull { !it.startsWith("#") } ?: "Jungian Dream Analysis"

        // Suggest dream tags from Gemini
        val autoTags = GeminiClient.suggestDreamTags(transcription)

        // 3. Update database record with structured analysis
        loadedDream = repository.allDreams.first().find { it.id == id }
        if (loadedDream != null) {
            val updatedDream = loadedDream.copy(
                emotionalTheme = emotionalTheme,
                structuredInterpretation = interpretation,
                tags = autoTags
            )
            repository.updateDream(updatedDream)
        }

        // Set state to Success, select the dream to view it immediately
        _recordingState.value = RecordingState.Success(id)
        _selectedDreamId.value = id
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
