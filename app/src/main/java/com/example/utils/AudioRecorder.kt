package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(onStarted: (File) -> Unit, onError: (Exception) -> Unit) {
        try {
            // Save recording to application cache directory
            val file = File(context.cacheDir, "dream_rec_${System.currentTimeMillis()}.m4a")
            currentFile = file

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)

                prepare()
                start()
            }
            mediaRecorder = recorder
            onStarted(file)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            onError(e)
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = currentFile
            currentFile = null
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            mediaRecorder = null
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        currentFile?.delete()
        currentFile = null
    }
}
