package com.parentalcontrol.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.parentalcontrol.app.streaming.WebRtcManager
import com.parentalcontrol.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.parentalcontrol.app.cloud.FirebaseConnectionManager
import android.os.Build
import android.provider.Settings

/**
 * Foreground service for the child device that:
 * 1. Registers / refreshes the device in Firebase (heartbeat loop).
 * 2. Initialises WebRTC and starts broadcasting camera + audio to the parent.
 *
 * Start with [ACTION_START] and the child's 6-digit code in [EXTRA_CODE].
 * Stop with [ACTION_STOP].
 */
class ChildCameraService : Service() {

    companion object {
        private const val TAG = "ChildCameraService"
        const val ACTION_START = "com.parentalcontrol.app.CHILD_CAMERA_START"
        const val ACTION_STOP = "com.parentalcontrol.app.CHILD_CAMERA_STOP"
        const val EXTRA_CODE = "extra_child_code"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L

        @Volatile
        private var isRunning = false

        fun isRunning() = isRunning
    }

    private lateinit var webRtcManager: WebRtcManager
    private val firebaseManager = FirebaseConnectionManager()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var connectionCode: String = ""

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        connectionCode = intent?.getStringExtra(EXTRA_CODE).orEmpty()
        if (connectionCode.isBlank()) {
            Log.e(TAG, "No connection code provided — stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildMonitoringNotification(
            this, isCamera = true, isAudio = true
        )
        startForeground(NotificationHelper.MONITORING_NOTIFICATION_ID + 2, notification)

        if (isRunning) {
            // Already running — ignore duplicate start
            return START_STICKY
        }
        isRunning = true

        startWebRtc()
        startHeartbeat()

        return START_STICKY
    }

    private fun startWebRtc() {
        // Start the MJPEG HTTP server so LAN streaming is available via ChildSocketServer.
        startService(Intent(this, CameraStreamService::class.java))

        webRtcManager = WebRtcManager(this).also { mgr ->
            mgr.listener = object : WebRtcManager.Listener {
                override fun onConnectionStateChanged(state: String) {
                    Log.d(TAG, "Connection state: $state")
                }
                override fun onError(message: String) {
                    Log.e(TAG, "Connection error: $message")
                }
            }
            mgr.initialize()
            mgr.startAsChild(connectionCode)
        }
        Log.d(TAG, "Streaming started as child for code=$connectionCode")
    }

    private fun startHeartbeat() {
        val deviceName = Build.MODEL
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()

        heartbeatJob = serviceScope.launch {
            // Initial registration
            firebaseManager.registerDevice(connectionCode, deviceName, deviceId)

            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                firebaseManager.postHeartbeat(connectionCode)
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        heartbeatJob?.cancel()
        serviceScope.cancel()
        if (::webRtcManager.isInitialized) {
            webRtcManager.release()
        }
        // Stop the MJPEG stream server that was started alongside this service.
        stopService(Intent(this, CameraStreamService::class.java))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
