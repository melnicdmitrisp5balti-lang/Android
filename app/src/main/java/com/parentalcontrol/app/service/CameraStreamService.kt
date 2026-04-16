package com.parentalcontrol.app.service

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class CameraStreamService : LifecycleService() {

    companion object {
        private const val TAG = "CameraStreamService"

        @Volatile
        private var streamReady = false

        fun isStreamReady(): Boolean = streamReady
    }

    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val latestJpegFrame = AtomicReference<ByteArray?>(null)
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var serverSocket: ServerSocket? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        startForeground(
            NotificationHelper.MONITORING_NOTIFICATION_ID + 1,
            NotificationHelper.buildMonitoringNotification(this, isCamera = true, isAudio = false)
        )
        startCameraAnalyzer()
        startMjpegServer()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startCameraAnalyzer() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val jpeg = imageProxy.toJpeg()
                        if (jpeg != null) {
                            latestJpegFrame.set(jpeg)
                            streamReady = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame encode error: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis
                )
            }.onFailure {
                Log.e(TAG, "Camera analyzer init error: ${it.message}")
                streamReady = false
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startMjpegServer() {
        serviceScope.launch {
            try {
                serverSocket = ServerSocket(Constants.DEFAULT_MJPEG_PORT)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleClient(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MJPEG server error: ${e.message}")
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.soTimeout = 10_000
        val output = BufferedOutputStream(socket.getOutputStream())
        try {
            output.write(
                (
                    "HTTP/1.1 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n"
                    ).toByteArray()
            )
            output.flush()

            while (serviceScope.isActive && !socket.isClosed) {
                val frame = latestJpegFrame.get()
                if (frame == null) {
                    delay(80)
                    continue
                }
                output.write("--frame\r\n".toByteArray())
                output.write("Content-Type: image/jpeg\r\n".toByteArray())
                output.write("Content-Length: ${frame.size}\r\n\r\n".toByteArray())
                output.write(frame)
                output.write("\r\n".toByteArray())
                output.flush()
                delay(120)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { output.close() }
            runCatching { socket.close() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamReady = false
        latestJpegFrame.set(null)
        runCatching { serverSocket?.close() }
        serverSocket = null
        serviceScope.cancel()
        cameraExecutor.shutdown()
    }
}

private fun ImageProxy.toJpeg(quality: Int = 60): ByteArray? {
    if (format != ImageFormat.YUV_420_888) return null
    val nv21 = yuv420888ToNv21(this) ?: return null
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val stream = ByteArrayOutputStream()
    return if (yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, stream)) {
        stream.toByteArray()
    } else {
        null
    }
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
    val planes = image.planes
    if (planes.size < 3) return null

    val width = image.width
    val height = image.height
    val ySize = width * height
    val uvSize = width * height / 4
    val out = ByteArray(ySize + uvSize * 2)

    var outOffset = 0
    val yPlane = planes[0]
    val yBuffer = yPlane.buffer
    val yRowStride = yPlane.rowStride
    for (row in 0 until height) {
        val rowStart = row * yRowStride
        yBuffer.position(rowStart)
        yBuffer.get(out, outOffset, width)
        outOffset += width
    }

    val uPlane = planes[1]
    val vPlane = planes[2]
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    val chromaHeight = height / 2
    val chromaWidth = width / 2

    val uRowStride = uPlane.rowStride
    val vRowStride = vPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    val vPixelStride = vPlane.pixelStride

    for (row in 0 until chromaHeight) {
        var uPos = row * uRowStride
        var vPos = row * vRowStride
        for (col in 0 until chromaWidth) {
            out[outOffset++] = vBuffer.get(vPos)
            out[outOffset++] = uBuffer.get(uPos)
            uPos += uPixelStride
            vPos += vPixelStride
        }
    }

    return out
}
