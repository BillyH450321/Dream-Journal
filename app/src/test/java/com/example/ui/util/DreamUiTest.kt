package com.example.ui.util

import com.example.data.Dream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamUiTest {

    @Test
    fun `displayTitle uses explicit title when present`() {
        val dream = Dream(id = 1, rawText = "long dream body", title = "Moonlit Chase")
        assertEquals("Moonlit Chase", dream.displayTitle())
    }

    @Test
    fun `displayTitle falls back to raw text preview`() {
        val dream = Dream(id = 1, rawText = "I was flying over the ocean")
        assertEquals("I was flying over the ocean", dream.displayTitle())
    }

    @Test
    fun `needsAnalysis is true for deferred and failed statuses`() {
        assertTrue(Dream(rawText = "a", analysisStatus = "deferred").needsAnalysis())
        assertTrue(Dream(rawText = "a", analysisStatus = "failed").needsAnalysis())
        assertFalse(Dream(rawText = "a", analysisStatus = "complete").needsAnalysis())
    }

    @Test
    fun `filterDreams matches title tags and body`() {
        val dreams = listOf(
            Dream(id = 1, rawText = "flying over water", title = "Ocean Flight", tags = "water,flight"),
            Dream(id = 2, rawText = "running in forest", title = "Forest Run", tags = "nature")
        )

        assertEquals(1, filterDreams(dreams, "ocean").size)
        assertEquals(1, filterDreams(dreams, "forest").size)
        assertEquals(1, filterDreams(dreams, "water").size)
        assertEquals(2, filterDreams(dreams, "").size)
    }

    @Test
    fun `formatAudioTime renders mm ss`() {
        assertEquals("1:05", formatAudioTime(65_000))
        assertEquals("0:00", formatAudioTime(0))
    }
}