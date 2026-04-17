package com.parentalcontrol.app.ui.widget

/**
 * Callback interface for MJPEG stream events.
 * Implement this interface and pass it to [MjpegViewCustom.streamListener]
 * to receive notifications about connection state and frame delivery.
 */
interface MjpegStreamListener {
    /** Called on the main thread when the HTTP connection is established. */
    fun onConnected()

    /** Called on the main thread after each JPEG frame is rendered. */
    fun onFrameReceived()

    /** Called on the main thread when the stream ends cleanly (no error). */
    fun onDisconnected()

    /**
     * Called on the main thread when the stream ends due to a network error.
     * @param message Human-readable description of the error.
     */
    fun onError(message: String)
}
