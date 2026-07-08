package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val safetySettings: List<SafetySetting>? = null
)

@JsonClass(generateAdapter = true)
data class SafetySetting(
    val category: String,
    val threshold: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 representation
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    val aspectRatio: String? = null, // e.g. "1:1", "3:4", "4:3"
    val imageSize: String? = null // e.g. "1K", "2K"
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String, // e.g. "OBJECT", "ARRAY", "STRING"
    val description: String? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Transcribes audio using gemini-3.5-flash
     */
    suspend fun transcribeAudio(audioBytesBase64: String, mimeType: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not set or is placeholder")
            return "API Key Error: Please set your GEMINI_API_KEY in the Secrets panel."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(inlineData = InlineData(mimeType = mimeType, data = audioBytesBase64)),
                        Part(text = "Please transcribe the speech in this audio of a dream recording verbatim. Return ONLY the transcribed text, with no introductory or concluding remarks. If there is no speech or it cannot be transcribed, return '[Transcription unavailable or audio was silent]'.")
                    )
                )
            )
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "No transcription received."
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            "Transcription failed: ${e.localizedMessage}"
        }
    }

    /**
     * Data class to hold Stage A extraction results.
     */
    private data class DreamThemeExtraction(
        val coreEmotion: String,
        val keySymbols: String,
        val colorMood: String
    )

    /**
     * Stage A: Extracts core elements from the dream for the image prompt.
     */
    private suspend fun extractDreamTheme(dreamText: String, soften: Boolean = false): DreamThemeExtraction {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return DreamThemeExtraction("mystery", "abstract shapes", "dark and moody")
        }

        val prompt = if (soften) {
            """
            Analyze the following dream narrative and extract its core elements, but SOFTEN and ABSTRACT any explicit violence, gore, or highly disturbing content into general emotional tones (e.g. 'a menacing shadow' instead of a literal violent noun).
            Format exactly like this, on 3 separate lines:
            Emotion: [core emotion]
            Symbols: [comma-separated key symbols]
            Color: [color mood]
            
            Dream text: $dreamText
            """.trimIndent()
        } else {
            """
            Analyze the following dream narrative and extract its core elements for an art piece.
            Format exactly like this, on 3 separate lines:
            Emotion: [core emotion]
            Symbols: [comma-separated key symbols]
            Color: [color mood]
            
            Dream text: $dreamText
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            var emotion = "mystery"
            var symbols = "abstract shapes"
            var color = "dark and moody"

            text.lines().forEach { line ->
                when {
                    line.startsWith("Emotion:", ignoreCase = true) -> emotion = line.substringAfter(":").trim()
                    line.startsWith("Symbols:", ignoreCase = true) -> symbols = line.substringAfter(":").trim()
                    line.startsWith("Color:", ignoreCase = true) -> color = line.substringAfter(":").trim()
                }
            }
            DreamThemeExtraction(emotion, symbols, color)
        } catch (e: Exception) {
            Log.e(TAG, "Theme extraction failed", e)
            DreamThemeExtraction("mystery", "abstract shapes", "dark and moody")
        }
    }

    /**
     * Result of image generation
     */
    data class ImageGenerationResult(
        val imageBytesBase64: String?,
        val fallbackUsed: Boolean = false
    )

    /**
     * Generates a surrealist image representing the dream's emotional theme using a two-stage pipeline.
     */
    suspend fun generateSurrealImage(dreamText: String): ImageGenerationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not set or is placeholder")
            return ImageGenerationResult(null)
        }

        val imageModel = "gemini-3.1-flash-image"

        suspend fun attemptGeneration(extraction: DreamThemeExtraction): String? {
            val prompt = "A surrealist painting in the style of dreamlike symbolism, depicting ${extraction.coreEmotion}. Central imagery: ${extraction.keySymbols}. Color palette: ${extraction.colorMood}. Style: reminiscent of Dalí and Magritte, soft brushwork, impossible perspective, symbolic and metaphorical rather than literal, painterly texture, dramatic lighting, no text, no logos, no legible writing."
            
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                    responseModalities = listOf("TEXT", "IMAGE")
                ),
                safetySettings = listOf(
                    SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE"),
                    SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
                    SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
                    SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE")
                )
            )

            val response = service.generateContent(imageModel, apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            if (candidate?.finishReason == "IMAGE_SAFETY" || candidate?.finishReason == "SAFETY") {
                return null // Blocked
            }
            return candidate?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
        }

        try {
            // Stage 1: Standard Extraction
            val extraction = extractDreamTheme(dreamText, soften = false)
            var imageResult = attemptGeneration(extraction)

            if (imageResult != null) {
                return ImageGenerationResult(imageResult, fallbackUsed = false)
            }

            // Stage 2: Fallback to softened extraction
            Log.w(TAG, "Image generation blocked by safety filters. Attempting softened fallback.")
            val softenedExtraction = extractDreamTheme(dreamText, soften = true)
            imageResult = attemptGeneration(softenedExtraction)

            if (imageResult != null) {
                return ImageGenerationResult(imageResult, fallbackUsed = true)
            }

            // Stage 3: Ultimate abstract fallback
            Log.w(TAG, "Softened image generation also blocked. Attempting abstract fallback.")
            val abstractExtraction = DreamThemeExtraction(
                coreEmotion = "an undefined profound feeling",
                keySymbols = "soft, flowing shapes and atmospheric mist",
                colorMood = softenedExtraction.colorMood
            )
            imageResult = attemptGeneration(abstractExtraction)
            return ImageGenerationResult(imageResult, fallbackUsed = true)

        } catch (e: Exception) {
            Log.e(TAG, "Surreal image generation pipeline failed", e)
            return ImageGenerationResult(null)
        }
    }

    /**
     * Generates a structured psychological and Jungian interpretation of the dream.
     */
    suspend fun interpretDream(dreamText: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "API Key Error: Please set your GEMINI_API_KEY in the Secrets panel."
        }

        val systemPrompt = "You are a distinguished Jungian psychoanalyst and expert on dream archetypes, shadow work, and mythology. You provide highly intellectual, compassionate, and structured dream interpretations."
        val prompt = """
            Analyze this dream: '$dreamText'
            
            Provide a structured, beautifully formatted markdown analysis with the following distinct sections:
            
            ### Core Emotional Theme
            Describe the dominant emotional resonance or psychological conflict of the dream in 1-2 paragraphs.
            
            ### Archetypal Analysis
            Analyze key archetypes present (e.g., Anima/Animus, Shadow, Wise Old Man/Woman, Persona, Self, Trickster, etc.) and what they symbolize in the context of the dreamer's life.
            
            ### Major Symbols & Metaphors
            Identify 2-3 specific objects, actions, or environments and decipher their symbolic psychological meaning.
            
            ### Inner Growth Guidance
            Provide 2 actionable reflections or journaling prompts for shadow work or psychological integration.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "No interpretation could be generated."
        } catch (e: Exception) {
            Log.e(TAG, "Dream interpretation failed", e)
            "Interpretation failed: ${e.localizedMessage}"
        }
    }

    /**
     * Chat interface to ask questions about specific dream symbols.
     */
    suspend fun askFollowUpQuestion(
        dreamText: String,
        history: List<Content>,
        newQuestion: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "API Key Error: Please set your GEMINI_API_KEY in the Secrets panel."
        }

        val systemPrompt = """
            You are a Jungian dream analyst. The user has recorded a dream: '$dreamText'.
            They are asking follow-up questions about specific symbols, feelings, or details from this dream.
            Answer compassionately, depth-psychologically, and keep your responses deeply reflective, engaging, and focused on helping the user explore their inner psyche. Keep answers concise but intellectually rich.
        """.trimIndent()

        // Combine history + the new question
        val contentsList = history.toMutableList().apply {
            add(Content(role = "user", parts = listOf(Part(text = newQuestion))))
        }

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "I could not analyze that symbol."
        } catch (e: Exception) {
            Log.e(TAG, "Follow up question failed", e)
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Analyzes multiple dreams to identify recurring themes, symbols, emotional patterns, and archetypal presences.
     */
    suspend fun analyzeDreamPatterns(dreamsList: List<String>): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "API Key Error: Please set your GEMINI_API_KEY in the Secrets panel."
        }
        if (dreamsList.isEmpty()) {
            return "No dreams recorded yet to analyze. Please log some dreams first!"
        }

        val systemPrompt = "You are an elite Jungian psychoanalyst and dream research scientist. You are analyzing a collection of dreams from a single dreamer to find deeper subconscious trends, recurring symbols, underlying psychological struggles, and archetypal manifestations across time."

        val dreamsPromptText = dreamsList.mapIndexed { index, dream ->
            "Dream #${index + 1}:\n$dream"
        }.joinToString("\n\n---\n\n")

        val prompt = """
            You are given a collection of ${dreamsList.size} dreams recorded by the same individual.
            Analyze these dreams collectively to identify overarching subconscious patterns, recurring structures, and developments.
            
            Here are the dreams:
            $dreamsPromptText
            
            Provide a beautifully formatted, structured markdown report with the following sections:
            
            ### Subconscious Landscape Overview
            Summarize the general state of the dreamer's subconscious based on these dreams. What is the overarching atmosphere, narrative direction, or conflict? (1-2 paragraphs)
            
            ### Recurring Themes & Emotional Threads
            Identify 2-3 persistent themes or feelings that appear across multiple dreams (e.g. anxiety, seeking, flying, confronting shadows). Explain what these suggest about the dreamer's current waking or inner life.
            
            ### Symbolic Constellations
            Discuss any recurring symbols (e.g. water, animals, clocks, strangers, houses) and how their meaning evolves or connects between the dreams.
            
            ### Archetypal Presences
            Analyze which Jungian archetypes (e.g., the Shadow, the Anima/Animus, the Wise Guide, the Trickster, or the Devouring Mother/Father) are prominent across these dreams, and what their dialogue or presence suggests.
            
            ### Integration & Path Forward
            Offer 2-3 deep, actionable psychological recommendations or creative exercises to help the dreamer integrate these findings into their waking consciousness.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Could not generate dream pattern analysis."
        } catch (e: Exception) {
            Log.e(TAG, "Dream pattern analysis failed", e)
            "Analysis failed: ${e.localizedMessage}"
        }
    }

    /**
     * Generates a short, evocative title for a dream entry.
     */
    suspend fun generateDreamTitle(dreamText: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return fallbackDreamTitle(dreamText)
        }

        val prompt = """
            Create a short, evocative title (3-7 words) for this dream journal entry.
            The title should feel poetic and dreamlike, like a chapter name — not a summary sentence.
            Return ONLY the title text. No quotes, punctuation at the end, or explanation.
            
            Dream text:
            $dreamText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            val title = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?.trim()
                ?.removeSurrounding("\"")
                ?.removeSurrounding("'")
                ?.take(80)
            if (title.isNullOrBlank()) fallbackDreamTitle(dreamText) else title
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate dream title", e)
            fallbackDreamTitle(dreamText)
        }
    }

    private fun fallbackDreamTitle(dreamText: String): String {
        val words = dreamText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "Untitled Dream"
            words.size <= 5 -> words.joinToString(" ")
            else -> words.take(5).joinToString(" ") + "…"
        }
    }

    /**
     * Suggests a list of 3-5 lowercase, comma-separated tags representing core themes, symbols, or emotions in a dream.
     */
    suspend fun suggestDreamTags(dreamText: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not set or is placeholder")
            return "dream"
        }

        val prompt = """
            Given the following dream narrative, identify 3-5 single-word, concise categories or tags (e.g. flying, anxiety, family, water, chase, fear, forest, stranger) that best summarize its core symbols, emotions, or characters.
            Return ONLY a comma-separated list of these lowercase tags (e.g., flying,water,family). Do NOT include any introductory or concluding text, markdown formatting, spaces around commas, or explanations.
            
            Dream text:
            $dreamText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val response = service.generateContent("gemini-3.5-flash", apiKey, request)
            val tags = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.lowercase() ?: "dream"
            // Clean up common AI markdown artifacts like backticks
            tags.replace("`", "").replace(" ", "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to suggest dream tags", e)
            "dream"
        }
    }
}
