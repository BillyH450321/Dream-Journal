package com.example.domain

import android.util.Base64
import android.util.Log
import com.example.data.Dream
import java.io.File

class DreamFileStorage(private val filesDir: File) {
    private val tag = "DreamFileStorage"

    fun persistRecording(source: File): File {
        val dest = File(filesDir, "dream_audio_${System.currentTimeMillis()}.m4a")
        source.copyTo(dest, overwrite = true)
        source.delete()
        return dest
    }

    fun saveDreamImage(base64: String, dreamId: Long): String? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP or Base64.URL_SAFE)
            val file = File(filesDir, "dream_img_${dreamId}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(tag, "Error saving dream image", e)
            null
        }
    }

    fun deleteDreamAssets(dream: Dream) {
        dream.surrealImagePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        dream.audioPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }
}