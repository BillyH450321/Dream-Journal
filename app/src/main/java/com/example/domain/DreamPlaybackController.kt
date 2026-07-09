package com.example.domain

import android.util.Log
import com.example.ui.AudioPlaybackState
import com.example.utils.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DreamPlaybackController(
    private val scope: CoroutineScope,
    private val audioPlayer: AudioPlayer = AudioPlayer()
) {
    private val tag = "DreamPlaybackController"
    private var positionUpdateJob: Job? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun toggle(dreamId: Long, audioPath: String) {
        val current = _state.value
        if (current.dreamId == dreamId && current.isPlaying) {
            audioPlayer.pause()
            stopPositionUpdates()
            _state.value = current.copy(isPlaying = false)
            return
        }

        if (current.dreamId == dreamId && audioPlayer.isPreparedFor(audioPath)) {
            audioPlayer.play()
            startPositionUpdates(dreamId)
            _state.value = current.copy(isPlaying = true, error = null)
            return
        }

        scope.launch {
            try {
                stop()
                val duration = withContext(Dispatchers.IO) {
                    audioPlayer.prepare(audioPath)
                }
                audioPlayer.play()
                _state.value = AudioPlaybackState(
                    dreamId = dreamId,
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = duration,
                    error = null
                )
                startPositionUpdates(dreamId)
            } catch (e: Exception) {
                Log.e(tag, "Failed to play dream audio", e)
                _state.value = AudioPlaybackState(
                    dreamId = dreamId,
                    isPlaying = false,
                    error = "Could not play recording: ${e.localizedMessage}"
                )
            }
        }
    }

    fun seek(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
        _state.value = _state.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        stopPositionUpdates()
        audioPlayer.release()
        _state.value = AudioPlaybackState()
    }

    fun isPlayingDream(dreamId: Long): Boolean = _state.value.dreamId == dreamId

    private fun startPositionUpdates(dreamId: Long) {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (true) {
                delay(250)
                val current = _state.value
                if (current.dreamId != dreamId) break

                val position = audioPlayer.currentPosition()
                val duration = audioPlayer.duration().takeIf { it > 0 } ?: current.durationMs
                val playing = audioPlayer.isPlaying()

                if (!playing && position >= duration - 500 && duration > 0) {
                    audioPlayer.stop()
                    _state.value = current.copy(
                        isPlaying = false,
                        currentPositionMs = 0,
                        durationMs = duration
                    )
                    break
                }

                _state.value = current.copy(
                    isPlaying = playing,
                    currentPositionMs = position,
                    durationMs = duration
                )
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
}