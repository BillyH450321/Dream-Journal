package com.example.network

import com.example.BuildConfig

object ApiKeyProvider {
    @Volatile
    var runtimeKey: String = ""

    fun resolve(): String {
        val fromRuntime = runtimeKey.trim().removeSurrounding("\"").removeSurrounding("'")
        if (fromRuntime.isNotEmpty()) return fromRuntime

        return BuildConfig.GEMINI_API_KEY
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
    }

    fun isConfigured(): Boolean {
        val key = resolve()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }
}