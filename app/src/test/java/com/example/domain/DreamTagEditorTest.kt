package com.example.domain

import com.example.data.Dream
import com.example.data.DreamDao
import com.example.data.DreamRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDreamDao : DreamDao {
    var lastUpdated: Dream? = null

    override fun getAllDreams() = emptyFlow<List<Dream>>()
    override fun getDreamById(id: Long) = emptyFlow<Dream?>()
    override suspend fun insertDream(dream: Dream) = throw UnsupportedOperationException()
    override suspend fun updateDream(dream: Dream) {
        lastUpdated = dream
    }
    override suspend fun deleteDream(id: Long) = throw UnsupportedOperationException()
    override fun getChatMessagesForDream(dreamId: Long) = emptyFlow<List<com.example.data.ChatMessage>>()
    override suspend fun insertChatMessage(message: com.example.data.ChatMessage) =
        throw UnsupportedOperationException()
}

class DreamTagEditorTest {

    private val dao = FakeDreamDao()
    private val editor = DreamTagEditor(DreamRepository(dao))

    @Test
    fun `addTag appends normalized tag`() = runBlocking {
        val dream = Dream(id = 1, rawText = "test", tags = "flying")
        assertTrue(editor.addTag(dream, " Water "))
        assertEquals("flying,water", dao.lastUpdated?.tags)
    }

    @Test
    fun `addTag ignores duplicates`() = runBlocking {
        val dream = Dream(id = 1, rawText = "test", tags = "flying")
        assertFalse(editor.addTag(dream, "flying"))
        assertEquals(null, dao.lastUpdated)
    }

    @Test
    fun `removeTag deletes existing tag`() = runBlocking {
        val dream = Dream(id = 1, rawText = "test", tags = "flying,water")
        assertTrue(editor.removeTag(dream, "water"))
        assertEquals("flying", dao.lastUpdated?.tags)
    }
}