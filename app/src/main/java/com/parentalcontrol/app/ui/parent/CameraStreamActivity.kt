package com.parentalcontrol.app.ui.parent

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding
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
    private lateinit var prefs: PreferenceManager
    private var childHost: String = ""
    private var streamJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        childHost = prefs.getLastChildHost().orEmpty()

        supportActionBar?.title = getString(R.string.camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupIpInput()

        binding.btnRefreshStream.setOnClickListener {
            connectToStream()
        }

        if (childHost.isNotBlank()) {
            // Host already known — connect immediately
            connectToStream()
        } else {
            // No host stored — prompt user to enter the child's IP address
            showIpInputPanel()
        }
    }

    private fun setupIpInput() {
        binding.btnConnectIp.setOnClickListener {
            val ip = binding.etChildIp.text?.toString()?.trim().orEmpty()
            if (ip.isBlank()) {
                binding.tilChildIp.error = getString(R.string.hint_child_ip)
                return@setOnClickListener
            }
            binding.tilChildIp.error = null
            childHost = ip
            prefs.saveLastChildHost(ip)
            hideIpInputPanel()
            // Dismiss keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etChildIp.windowToken, 0)
            connectToStream()
        }

        binding.etChildIp.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnConnectIp.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun showIpInputPanel() {
        binding.layoutIpInput.visibility = View.VISIBLE
        binding.progressStream.visibility = View.GONE
        updateQualityUi(getString(R.string.camera_stream_quality_connecting), R.color.neon_magenta)
        binding.tvCameraStatus.text = getString(R.string.camera_stream_waiting_child)
    }

    private fun hideIpInputPanel() {
        binding.layoutIpInput.visibility = View.GONE
    }

    private fun connectToStream() {
        if (childHost.isBlank()) {
            showIpInputPanel()
            return
        }
        streamJob?.cancel()
        binding.progressStream.visibility = View.VISIBLE
        binding.ivCameraStream.setImageDrawable(null)
        updateQualityUi(getString(R.string.camera_stream_quality_connecting), R.color.neon_magenta)
        binding.tvCameraStatus.text = getString(R.string.camera_stream_connecting_child)
        binding.btnRefreshStream.isEnabled = false

        val url = "http://$childHost:${Constants.DEFAULT_MJPEG_PORT}${Constants.MJPEG_STREAM_PATH}"
        startMjpegPlayback(url)
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
                        if (reconnectAttempt > 3) {
                            // After several failed attempts, let the user change the IP
                            showIpInputPanel()
                            binding.btnRefreshStream.isEnabled = true
                        }
                    }
                    if (reconnectAttempt > 3) break
                    delay(Constants.STREAM_RECONNECT_DELAY_MS)
                } else {
                    reconnectAttempt = 0
                    runOnUiThread { binding.btnRefreshStream.isEnabled = true }
                }
            }
        }
    }

    private suspend fun readMjpegStream(url: String): Boolean {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 15_000
            doInput = true
        }
        return try {
            connection.connect()
            runOnUiThread {
                updateQualityUi(getString(R.string.camera_stream_quality_good), R.color.neon_green)
                binding.tvCameraStatus.text = getString(R.string.camera_stream_live)
                binding.progressStream.visibility = View.GONE
                binding.btnRefreshStream.isEnabled = true
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
