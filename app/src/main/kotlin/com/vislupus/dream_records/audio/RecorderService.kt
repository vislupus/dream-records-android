package com.vislupus.dream_records.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vislupus.dream_records.DreamRecordsApp
import com.vislupus.dream_records.MainActivity
import com.vislupus.dream_records.R
import com.vislupus.dream_records.config.AppConfig
import com.vislupus.dream_records.config.Strings
import com.vislupus.dream_records.data.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Observable recorder state shared with the UI (no binding needed). */
data class RecorderState(
    val isRecording: Boolean = false,
    val startedAt: Long = 0L,
    val elapsedMs: Long = 0L,
    /** 0..1 microphone level for the pulsing ring. */
    val level: Float = 0f,
    /** Seconds of continuous silence so far. */
    val silenceSec: Int = 0,
    /** Set briefly after a recording finished: "saved", "auto", "short". */
    val lastEvent: String? = null,
    val lastRecordingId: String? = null,
)

object RecorderBus {
    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()
    internal fun update(block: (RecorderState) -> RecorderState) { _state.value = block(_state.value) }
    fun clearEvent() = update { it.copy(lastEvent = null) }
}

/**
 * Foreground service that records audio with MediaRecorder (AAC in .m4a) and stops
 * itself after the configured period of silence, or at the hard max length.
 * The screen may turn off — a partial wake lock keeps the CPU alive.
 */
class RecorderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var silenceTimeoutMs = AppConfig.Recording.DEFAULT_SILENCE_TIMEOUT_MIN * 60_000L
    private var pollJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stopping = false
    private var maxDurationReached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                silenceTimeoutMs = intent.getIntExtra(EXTRA_SILENCE_MIN, AppConfig.Recording.DEFAULT_SILENCE_TIMEOUT_MIN) * 60_000L
                val lang = intent.getStringExtra(EXTRA_LANGUAGE) ?: AppConfig.App.DEFAULT_LANGUAGE
                startForegroundCompat(buildNotification(Strings.forCode(lang)))
                startRecording()
            }
            ACTION_STOP -> stopRecording(autoStopped = false)
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(AppConfig.Recording.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(AppConfig.Recording.NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(s: Strings): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, AppConfig.Recording.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.lucide_mic)
            .setContentTitle(AppConfig.App.NAME)
            .setContentText(s.recording + "…")
            .setContentIntent(open)
            .addAction(R.drawable.lucide_square, s.tapToStop, stop)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startRecording() {
        if (recorder != null) return
        val app = DreamRecordsApp.from(this)
        startedAt = TimeUtils.now()
        val out = app.repository.newAudioFile(startedAt)
        file = out

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(AppConfig.Recording.AUDIO_SOURCE)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(AppConfig.Recording.SAMPLE_RATE_HZ)
            r.setAudioEncodingBitRate(AppConfig.Recording.BIT_RATE_BPS)
            r.setAudioChannels(AppConfig.Recording.CHANNELS)
            r.setMaxDuration(AppConfig.Recording.MAX_RECORDING_MINUTES * 60_000)
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    maxDurationReached = true // the recorder has already stopped itself at this point
                    stopRecording(autoStopped = true)
                }
            }
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recorder", e)
            r.release()
            out.delete()
            stopSelf()
            return
        }
        recorder = r
        acquireWakeLock()
        RecorderBus.update { RecorderState(isRecording = true, startedAt = startedAt) }
        startLevelPolling()
    }

    /** Polls the mic amplitude; drives the UI level meter and the silence auto-stop. */
    private fun startLevelPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var lastLoud = TimeUtils.now()
            // MediaRecorder.getMaxAmplitude() returns the max since the previous call.
            while (isActive) {
                delay(AppConfig.Recording.LEVEL_POLL_INTERVAL_MS)
                val amp = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                val now = TimeUtils.now()
                if (amp >= AppConfig.Recording.SILENCE_AMPLITUDE_THRESHOLD) lastLoud = now
                val silence = now - lastLoud
                RecorderBus.update {
                    it.copy(
                        elapsedMs = now - startedAt,
                        level = (amp / 32767f).coerceIn(0f, 1f),
                        silenceSec = (silence / 1000).toInt(),
                    )
                }
                if (silence >= silenceTimeoutMs) {
                    stopRecording(autoStopped = true)
                    return@launch
                }
            }
        }
    }

    private fun stopRecording(autoStopped: Boolean) {
        if (stopping) return
        stopping = true
        pollJob?.cancel()
        val r = recorder ?: run { stopSelf(); return }
        recorder = null
        val out = file
        val endedAt = TimeUtils.now()
        var ok = true
        if (!maxDurationReached) {
            try { r.stop() } catch (e: Exception) { ok = false; Log.w(TAG, "stop() failed (too short?)", e) }
        }
        try { r.release() } catch (_: Exception) {}
        releaseWakeLock()

        scope.launch {
            val app = DreamRecordsApp.from(this@RecorderService)
            var durationMs = endedAt - startedAt
            // When auto-stopped, trim the trailing silence from the stored duration (file keeps it; harmless).
            if (autoStopped) durationMs -= silenceTimeoutMs.coerceAtMost(durationMs)
            val tooShort = !ok || out == null || !out.exists() || durationMs < AppConfig.Recording.MIN_RECORDING_SECONDS * 1000L
            if (tooShort) {
                out?.delete()
                RecorderBus.update { RecorderState(lastEvent = "short") }
            } else {
                val rec = withContext(Dispatchers.IO) { app.repository.createRecording(out!!, startedAt, durationMs, autoStopped) }
                RecorderBus.update { RecorderState(lastEvent = if (autoStopped) "auto" else "saved", lastRecordingId = rec.id) }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:recorder").apply {
            acquire((AppConfig.Recording.MAX_RECORDING_MINUTES + 1) * 60_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        if (recorder != null) stopRecording(autoStopped = false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RecorderService"
        const val ACTION_START = "com.vislupus.dream_records.action.START"
        const val ACTION_STOP = "com.vislupus.dream_records.action.STOP"
        const val EXTRA_SILENCE_MIN = "silence_min"
        const val EXTRA_LANGUAGE = "language"

        fun start(context: Context, silenceMinutes: Int, languageCode: String) {
            val i = Intent(context, RecorderService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SILENCE_MIN, silenceMinutes)
                .putExtra(EXTRA_LANGUAGE, languageCode)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RecorderService::class.java).setAction(ACTION_STOP))
        }
    }
}
