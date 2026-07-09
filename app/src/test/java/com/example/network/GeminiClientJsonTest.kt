package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeminiClientJsonTest {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `parses snake_case inline image data from Gemini response`() {
        val json = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "inline_data": {
                      "mime_type": "image/png",
                      "data": "aGVsbG8="
                    }
                  }]
                },
                "finish_reason": "STOP"
              }]
            }
        """.trimIndent()

        val response = moshi.adapter(GenerateContentResponse::class.java).fromJson(json)
        val inlineData = response
            ?.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.inlineData

        assertNotNull(inlineData)
        assertEquals("image/png", inlineData?.mimeType)
        assertEquals("aGVsbG8=", inlineData?.data)
        assertEquals("STOP", response?.candidates?.firstOrNull()?.finishReason)
    }
}