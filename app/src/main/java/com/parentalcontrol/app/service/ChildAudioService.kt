package com.parentalcontrol.app.service

import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import com.parentalcontrol.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service for audio-only monitoring on the child device.
 *
 * Use this when the parent requests audio but not video. If both camera and
 * audio are needed start [ChildCameraService] instead (it captures both).
 *
 * Start with [ACTION_START]; stop with [ACTION_STOP].
 */
class ChildAudioService : Service() {

    companion object {
        private const val TAG = "ChildAudioService"
        const val ACTION_START = "com.parentalcontrol.app.CHILD_AUDIO_START"
        const val ACTION_STOP = "com.parentalcontrol.app.CHILD_AUDIO_STOP"

        private const val SAMPLE_RATE = 44_100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        @Volatile
        private var isRunning = false

        fun isRunning() = isRunning
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildMonitoringNotification(
            this, isCamera = false, isAudio = true
        )
        startForeground(NotificationHelper.MONITORING_NOTIFICATION_ID + 3, notification)
        isRunning = true
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission not granted: ${e.message}")
            stopSelf()
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized")
            stopSelf()
            return
        }

        audioRecord?.startRecording()
        Log.d(TAG, "Audio capture started (sample rate=$SAMPLE_RATE)")

        captureJob = serviceScope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read <= 0) continue
                // Audio data is available here; in a full WebRTC implementation
                // this would feed into a custom AudioSource. When ChildCameraService
                // is running, WebRTC handles audio capture internally — this service
                // can be used as a standalone audio-only monitor.
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        captureJob?.cancel()
        serviceScope.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
