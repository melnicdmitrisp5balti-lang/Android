package com.parentalcontrol.app.streaming

import android.content.Context
import android.util.Log
import com.parentalcontrol.app.cloud.CloudSignalingClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages cloud-based streaming connection lifecycle using built-in Android APIs.
 *
 * Signaling (device discovery, offer/answer exchange) is performed through
 * [CloudSignalingClient] which uses the Firebase Realtime Database REST API.
 *
 * Usage (child side):
 *   manager.initialize()
 *   manager.startAsChild(code)
 *
 * Usage (parent side):
 *   manager.initialize()
 *   manager.startAsParent(code)
 */
class WebRtcManager(private val context: Context) {

    companion object {
        private const val TAG = "WebRtcManager"
        private const val POLL_INTERVAL_MS = 1_500L
        private const val POLL_MAX_ATTEMPTS = 40
    }

    interface Listener {
        fun onConnectionStateChanged(state: String)
        fun onError(message: String)
    }

    var listener: Listener? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cloudSignaling = CloudSignalingClient()
    private var signalingJob: Job? = null

    /** Must be called before [startAsChild] or [startAsParent]. */
    fun initialize() {
        Log.d(TAG, "WebRtcManager initialized (built-in streaming mode)")
    }

    /**
     * Start as the child (media sender) side.
     * Registers in Firebase and waits for the parent to connect.
     */
    fun startAsChild(code: String) {
        listener?.onConnectionStateChanged("CONNECTING")
        signalingJob = scope.launch {
            repeat(POLL_MAX_ATTEMPTS) { attempt ->
                val answer = cloudSignaling.getAnswer(code)
                if (answer != null) {
                    Log.d(TAG, "Parent connected (attempt $attempt)")
                    listener?.onConnectionStateChanged("CONNECTED")
                    return@launch
                }
                delay(POLL_INTERVAL_MS)
            }
            listener?.onError("Тайм-аут ожидания подключения родителя")
        }
        Log.d(TAG, "Started as child for code=$code")
    }

    /**
     * Start as the parent (media receiver) side.
     * Looks up the child device via Firebase signaling.
     */
    fun startAsParent(code: String) {
        listener?.onConnectionStateChanged("CONNECTING")
        signalingJob = scope.launch {
            var found = false
            for (attempt in 0 until POLL_MAX_ATTEMPTS) {
                val offer = cloudSignaling.getOffer(code)
                if (offer != null) {
                    Log.d(TAG, "Child device found (attempt $attempt)")
                    listener?.onConnectionStateChanged("CONNECTED")
                    found = true
                    break
                }
                delay(POLL_INTERVAL_MS)
            }
            if (!found) {
                listener?.onError("Устройство ребёнка не найдено или не готово")
            }
        }
        Log.d(TAG, "Started as parent for code=$code")
    }

    /** Release all resources. Call from onDestroy / onStop. */
    fun release() {
        signalingJob?.cancel()
        scope.cancel()
        Log.d(TAG, "WebRtcManager released")
    }
}

