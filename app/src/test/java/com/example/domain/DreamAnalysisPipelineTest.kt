package com.example.domain

import com.example.data.Dream
import com.example.data.DreamDao
import com.example.data.DreamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


private class InMemoryDreamDao : DreamDao {
    private val dreams = MutableStateFlow<List<Dream>>(emptyList())
    private var nextId = 1L

    override fun getAllDreams(): Flow<List<Dream>> = dreams

    override fun getDreamById(id: Long): Flow<Dream?> =
        kotlinx.coroutines.flow.flow {
            emit(dreams.value.find { it.id == id })
        }

    override suspend fun insertDream(dream: Dream): Long {
        val id = nextId++
        dreams.value = dreams.value + dream.copy(id = id)
        return id
    }

    override suspend fun updateDream(dream: Dream) {
        dreams.value = dreams.value.map { if (it.id == dream.id) dream else it }
    }

    override suspend fun deleteDream(id: Long) {
        dreams.value = dreams.value.filterNot { it.id == id }
    }

    override fun getChatMessagesForDream(dreamId: Long) =
        kotlinx.coroutines.flow.emptyFlow<List<com.example.data.ChatMessage>>()

    override suspend fun insertChatMessage(message: com.example.data.ChatMessage): Long = 0L
}

class DreamAnalysisPipelineTest {

    @Test
    fun `run completes dream analysis using ai client`() = runTest {
        val dao = InMemoryDreamDao()
        val repository = DreamRepository(dao)
        val dreamId = repository.insertDream(
            Dream(rawText = "I flew over the sea", analysisStatus = "pending")
        )
        val aiClient = FakeDreamAiClient()
        val pipeline = DreamAnalysisPipeline(
            repository = repository,
            fileStorage = DreamFileStorage(Files.createTempDirectory("dream-test").toFile()),
            backgroundScope = TestScope(testScheduler),
            aiClient = aiClient
        )

        val result = pipeline.run(dreamId, "I flew over the sea")

        assertTrue(result is AnalysisStage.Completed)
        val updated = repository.allDreams.first().first { it.id == dreamId }
        assertEquals("complete", updated.analysisStatus)
        assertEquals("Ocean Flight", updated.title)
        assertEquals("flying,water", updated.tags)
        assertEquals(listOf("I flew over the sea"), aiClient.interpretCalls)
    }

    @Test
    fun `run refunds quota path on rate limit response`() = runTest {
        val dao = InMemoryDreamDao()
        val repository = DreamRepository(dao)
        val dreamId = repository.insertDream(Dream(rawText = "test dream"))
        val aiClient = FakeDreamAiClient().apply {
            interpretationResult = "HTTP 429 rate limit exceeded"
        }
        val pipeline = DreamAnalysisPipeline(
            repository = repository,
            fileStorage = DreamFileStorage(Files.createTempDirectory("dream-test-rate").toFile()),
            backgroundScope = TestScope(testScheduler),
            aiClient = aiClient
        )

        val result = pipeline.run(dreamId, "test dream")

        assertTrue(result is AnalysisStage.Failed)
        val failed = result as AnalysisStage.Failed
        assertTrue(failed.refundQuota)
    }
}