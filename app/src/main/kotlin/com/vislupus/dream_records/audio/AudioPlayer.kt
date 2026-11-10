package com.vislupus.dream_records.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.vislupus.dream_records.config.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

/** Thin MediaPlayer wrapper used by the recording detail screen. */
class AudioPlayer(private val scope: CoroutineScope) {
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    fun load(file: File) {
        release()
        val p = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
                stopTicker()
            }
            prepare()
        }
        player = p
        _state.value = PlayerState(isReady = true, durationMs = p.duration)
    }

    fun toggle() { if (_state.value.isPlaying) pause() else play() }

    fun play() {
        val p = player ?: return
        p.start()
        _state.value = _state.value.copy(isPlaying = true)
        startTicker()
    }

    fun pause() {
        val p = player ?: return
        if (p.isPlaying) p.pause()
        _state.value = _state.value.copy(isPlaying = false, positionMs = p.currentPosition)
        stopTicker()
    }

    fun seekTo(ms: Int) {
        val p = player ?: return
        val clamped = ms.coerceIn(0, _state.value.durationMs)
        p.seekTo(clamped)
        _state.value = _state.value.copy(positionMs = clamped)
    }

    fun skip(seconds: Int) = seekTo(_state.value.positionMs + seconds * 1000)

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                player?.let { if (it.isPlaying) _state.value = _state.value.copy(positionMs = it.currentPosition) }
                delay(AppConfig.Player.POSITION_POLL_MS)
            }
        }
    }

    private fun stopTicker() { ticker?.cancel(); ticker = null }

    fun release() {
        stopTicker()
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        _state.value = PlayerState()
    }
}
