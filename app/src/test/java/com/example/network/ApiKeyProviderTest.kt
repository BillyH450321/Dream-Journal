package com.example.network

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyProviderTest {

    @After
    fun tearDown() {
        ApiKeyProvider.runtimeKey = ""
    }

    @Test
    fun `resolve prefers runtime key over build config`() {
        ApiKeyProvider.runtimeKey = "runtime-test-key"
        assertTrue(ApiKeyProvider.resolve().contains("runtime-test-key"))
    }

    @Test
    fun `resolve strips surrounding quotes from runtime key`() {
        ApiKeyProvider.runtimeKey = "\"quoted-key\""
        assertTrue(ApiKeyProvider.resolve() == "quoted-key")
    }

    @Test
    fun `isConfigured returns false for placeholder key`() {
        ApiKeyProvider.runtimeKey = "MY_GEMINI_API_KEY"
        assertFalse(ApiKeyProvider.isConfigured())
    }

    @Test
    fun `isConfigured returns true for non-empty runtime key`() {
        ApiKeyProvider.runtimeKey = "sk-test-key"
        assertTrue(ApiKeyProvider.isConfigured())
    }
}