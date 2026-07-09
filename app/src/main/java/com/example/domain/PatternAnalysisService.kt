package com.example.domain

import android.content.Context
import com.example.data.Dream
import com.example.ui.PatternAnalysisState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatternAnalysisService(
    context: Context,
    private val aiClient: DreamAiClient
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCached(): PatternAnalysisState {
        val cachedReport = prefs.getString(KEY_REPORT, null)
        val cachedCount = prefs.getInt(KEY_DREAM_COUNT, 0)
        return if (cachedReport != null) {
            PatternAnalysisState.Success(cachedReport, cachedCount)
        } else {
            PatternAnalysisState.Idle
        }
    }

    suspend fun generate(dreams: List<Dream>): PatternAnalysisState {
        if (dreams.isEmpty()) {
            return PatternAnalysisState.Error(
                "No dreams have been recorded yet. Please capture some dreams first!"
            )
        }

        return try {
            val dreamTexts = dreams.map { dream ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(dream.timestamp))
                "[$dateStr]\n${dream.rawText}"
            }
            val report = aiClient.analyzeDreamPatterns(dreamTexts)
            if (GeminiResponseValidator.isPatternAnalysisFailure(report)) {
                PatternAnalysisState.Error(report)
            } else {
                prefs.edit()
                    .putString(KEY_REPORT, report)
                    .putInt(KEY_DREAM_COUNT, dreams.size)
                    .apply()
                PatternAnalysisState.Success(report, dreams.size)
            }
        } catch (e: Exception) {
            PatternAnalysisState.Error("Failed to analyze patterns: ${e.localizedMessage}")
        }
    }

    companion object {
        private const val PREFS_NAME = "dream_analysis_prefs"
        private const val KEY_REPORT = "cached_pattern_report"
        private const val KEY_DREAM_COUNT = "cached_dream_count"
    }
}