package com.example.ui.util

import com.example.data.Dream

fun Dream.displayTitle(): String =
    title?.takeIf { it.isNotBlank() && it != "Voice Dream" }
        ?: rawText.lineSequence().firstOrNull()?.take(60)?.trim().orEmpty().ifBlank { "Untitled Dream" }

fun Dream.needsAnalysis(): Boolean =
    analysisStatus == "deferred" || analysisStatus == "failed"

fun Dream.isAnalysisInProgress(analyzingDreamId: Long?): Boolean =
    analysisStatus == "pending" || analyzingDreamId == id

fun formatAudioTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun filterDreams(dreams: List<Dream>, searchQuery: String): List<Dream> {
    if (searchQuery.isBlank()) return dreams
    val query = searchQuery.lowercase().trim()
    return dreams.filter { dream ->
        dream.rawText.lowercase().contains(query) ||
            dream.title?.lowercase()?.contains(query) == true ||
            dream.tags.lowercase().split(",").any { tag -> tag.trim().contains(query) }
    }
}