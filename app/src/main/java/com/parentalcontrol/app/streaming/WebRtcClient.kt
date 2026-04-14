package com.parentalcontrol.app.streaming

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebRTC client skeleton for secure video/audio streaming.
 * Uses OkHttp WebSocket for signaling channel (HTTPS/WSS).
 * Full WebRTC peer connection can be integrated with google-webrtc library.
 */
class WebRtcClient(private val context: Context) {

    companion object {
        private const val TAG = "WebRtcClient"
    }

    private var webSocket: WebSocket? = null
    private var isConnected = false

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    interface SignalingListener {
        fun onConnected()
        fun onDisconnected()
        fun onMessage(message: String)
        fun onError(error: String)
    }

    private var signalingListener: SignalingListener? = null

    fun setSignalingListener(listener: SignalingListener) {
        signalingListener = listener
    }

    fun connect(serverUrl: String) {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                isConnected = true
                Log.d(TAG, "Signaling connected")
                signalingListener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message: $text")
                signalingListener?.onMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Binary message received")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                signalingListener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                isConnected = false
                signalingListener?.onError(t.message ?: "Unknown error")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }

    fun isConnected() = isConnected
}
