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
import kotlinx.coroutines.launch

class MonitoringService : Service() {

    companion object {
        const val ACTION_START_CAMERA = "action_start_camera"
        const val ACTION_START_AUDIO = "action_start_audio"
        const val ACTION_STOP = "action_stop"
        private const val TAG = "MonitoringService"
    }

    private var isCameraActive = false
    private var isAudioActive = false
    private var audioRecord: AudioRecord? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAMERA -> {
                isCameraActive = true
                startForegroundWithNotification()
            }
            ACTION_START_AUDIO -> {
                isAudioActive = true
                startForegroundWithNotification()
                startAudioCapture()
            }
            ACTION_STOP -> {
                stopMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationHelper.buildMonitoringNotification(
            this, isCameraActive, isAudioActive
        )
        startForeground(NotificationHelper.MONITORING_NOTIFICATION_ID, notification)
    }

    private fun startAudioCapture() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()

            serviceScope.launch {
                val buffer = ShortArray(bufferSize)
                while (isAudioActive) {
                    audioRecord?.read(buffer, 0, bufferSize)
                    // Buffer is available for streaming
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No audio permission: ${e.message}")
            isAudioActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopMonitoring() {
        isCameraActive = false
        isAudioActive = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
