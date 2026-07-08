package com.example.utils

import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var preparedPath: String? = null

    fun prepare(path: String): Int {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("Audio file not found")
        }

        if (preparedPath == path && mediaPlayer != null) {
            return mediaPlayer?.duration ?: 0
        }

        release()
        preparedPath = path

        return MediaPlayer().apply {
            setDataSource(path)
            prepare()
            mediaPlayer = this
        }.duration
    }

    fun play() {
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            seekTo(0)
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun currentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun duration(): Int = mediaPlayer?.duration ?: 0

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun isPreparedFor(path: String): Boolean = preparedPath == path && mediaPlayer != null

    fun release() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to release media player", e)
        }
        mediaPlayer = null
        preparedPath = null
    }
}