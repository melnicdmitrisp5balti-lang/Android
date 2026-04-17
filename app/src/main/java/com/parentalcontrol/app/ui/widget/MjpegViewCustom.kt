package com.parentalcontrol.app.ui.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.parentalcontrol.app.utils.MjpegFrameReader
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Custom ImageView that connects to an MJPEG HTTP stream and renders
 * decoded JPEG frames in real-time (~15 FPS).
 *
 * Usage:
 *   1. Call [startStream] with the stream URL to begin playback.
 *   2. Call [stopStream] to stop (automatically called on detach).
 */
class MjpegViewCustom @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var streamListener: MjpegStreamListener? = null

    @Volatile private var active = false
    private var streamThread: Thread? = null

    /**
     * Start receiving and displaying the MJPEG stream at [url].
     * Any previously running stream is stopped first.
     */
    fun startStream(url: String) {
        stopStream()
        active = true
        streamThread = thread(isDaemon = true, name = "MjpegStreamThread") {
            streamLoop(url)
        }
    }

    /** Stop the stream and clear the displayed image. */
    fun stopStream() {
        active = false
        streamThread?.interrupt()
        streamThread = null
        post { setImageDrawable(null) }
    }

    private fun streamLoop(url: String) {
        while (active && !Thread.currentThread().isInterrupted) {
            var connection: HttpURLConnection? = null
            var errorOccurred = false
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 15_000
                    doInput = true
                }
                connection.connect()
                post { streamListener?.onConnected() }

                val input = BufferedInputStream(connection.inputStream, 8192)
                while (active && !Thread.currentThread().isInterrupted) {
                    val jpegBytes = MjpegFrameReader.readJpegFrame(input)
                    if (jpegBytes == null) {
                        // null means EOF or broken stream — treat as an error disconnect
                        errorOccurred = true
                        break
                    }
                    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                        ?: continue
                    post {
                        setImageBitmap(bitmap)
                        streamListener?.onFrameReceived()
                    }
                }
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                errorOccurred = true
            } finally {
                runCatching { connection?.disconnect() }
            }

            val capturedError = errorOccurred
            post {
                if (capturedError) {
                    streamListener?.onError("Stream error or EOF")
                } else {
                    streamListener?.onDisconnected()
                }
            }

            if (active) {
                try {
                    Thread.sleep(1_500)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        stopStream()
        super.onDetachedFromWindow()
    }
}
