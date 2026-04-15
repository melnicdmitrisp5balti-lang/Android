package com.parentalcontrol.app.ui.parent

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding
import com.parentalcontrol.app.service.ParentSocketClient
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.MjpegFrameReader
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class CameraStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraStreamBinding
    private val socketClient = ParentSocketClient()
    private lateinit var prefs: PreferenceManager
    private var childHost: String = ""
    private var connectionCode: String = ""
    private var streamJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        childHost = prefs.getLastChildHost().orEmpty()
        connectionCode = prefs.getLastConnectionCode().orEmpty()

        supportActionBar?.title = getString(R.string.camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnRefreshStream.setOnClickListener {
            requestChildCameraStatus()
        }

        requestChildCameraStatus()
    }

    private fun requestChildCameraStatus() {
        streamJob?.cancel()
        binding.progressStream.visibility = View.VISIBLE
        binding.ivCameraStream.setImageDrawable(null)
        updateQualityUi(getString(R.string.camera_stream_quality_connecting), R.color.neon_magenta)

        if (connectionCode.isBlank()) {
            binding.tvCameraStatus.text = getString(R.string.camera_stream_no_active_session)
            binding.btnRefreshStream.isEnabled = false
            binding.progressStream.visibility = View.GONE
            return
        }

        // Cloud connection — no direct LAN host available.
        // The WebRTC P2P channel (signaled via Firebase) carries the media.
        if (childHost.isBlank()) {
            binding.progressStream.visibility = View.GONE
            updateQualityUi(getString(R.string.camera_stream_quality_cloud), R.color.neon_green)
            binding.tvCameraStatus.text = getString(R.string.camera_stream_cloud_connected)
            binding.btnRefreshStream.isEnabled = true
            return
        }

        binding.btnRefreshStream.isEnabled = false
        binding.tvCameraStatus.text = getString(R.string.camera_stream_connecting_child)

        lifecycleScope.launch {
            val result = socketClient.requestChildCameraStatus(
                host = childHost,
                port = Constants.DEFAULT_SOCKET_PORT,
                code = connectionCode
            )
            if (result.isSuccess) {
                val info = result.getOrNull() ?: return@launch
                binding.tvCameraStatus.text =
                    getString(R.string.camera_stream_child_ready, info.childName)
                startMjpegPlayback(info.streamUrl)
            } else {
                binding.progressStream.visibility = View.GONE
                updateQualityUi(getString(R.string.camera_stream_quality_lost), R.color.neon_magenta)
                binding.tvCameraStatus.text = getString(R.string.camera_stream_child_unavailable)
                Toast.makeText(
                    this@CameraStreamActivity,
                    result.exceptionOrNull()?.message ?: getString(R.string.camera_stream_child_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.btnRefreshStream.isEnabled = true
        }
    }

    private fun startMjpegPlayback(url: String) {
        streamJob?.cancel()
        streamJob = lifecycleScope.launch(Dispatchers.IO) {
            var reconnectAttempt = 0
            while (isActive) {
                val connected = readMjpegStream(url)
                if (!connected) {
                    reconnectAttempt++
                    runOnUiThread {
                        binding.progressStream.visibility = View.VISIBLE
                        updateQualityUi(getString(R.string.camera_stream_quality_reconnecting), R.color.neon_magenta)
                        binding.tvCameraStatus.text = getString(
                            R.string.camera_stream_reconnect_attempt,
                            reconnectAttempt
                        )
                    }
                    delay(Constants.STREAM_RECONNECT_DELAY_MS)
                } else {
                    reconnectAttempt = 0
                }
            }
        }
    }

    private suspend fun readMjpegStream(url: String): Boolean {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = Constants.SOCKET_CONNECT_TIMEOUT_MS
            readTimeout = 12_000
            doInput = true
        }
        return try {
            connection.connect()
            runOnUiThread {
                updateQualityUi(getString(R.string.camera_stream_quality_good), R.color.neon_green)
                binding.tvCameraStatus.text = getString(R.string.camera_stream_live)
                binding.progressStream.visibility = View.GONE
            }

            val input = BufferedInputStream(connection.inputStream)
            while (streamJob?.isActive == true) {
                val frame = MjpegFrameReader.readJpegFrame(input) ?: return false
                val bitmap = BitmapFactory.decodeByteArray(frame, 0, frame.size) ?: continue
                runOnUiThread {
                    binding.ivCameraStream.setImageBitmap(bitmap)
                }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun updateQualityUi(text: String, colorRes: Int) {
        binding.tvConnectionQuality.text = text
        binding.tvConnectionQuality.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    override fun onDestroy() {
        streamJob?.cancel()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
