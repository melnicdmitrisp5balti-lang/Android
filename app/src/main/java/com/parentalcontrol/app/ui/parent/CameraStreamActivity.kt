package com.parentalcontrol.app.ui.parent

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding
import com.parentalcontrol.app.ui.widget.MjpegStreamListener
import com.parentalcontrol.app.ui.widget.MjpegViewCustom
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PreferenceManager

class CameraStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraStreamBinding
    private lateinit var prefs: PreferenceManager
    private var childHost: String = ""
    private var reconnectAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        childHost = prefs.getLastChildHost().orEmpty()

        supportActionBar?.title = getString(R.string.camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupMjpegView()
        setupIpInput()

        binding.btnRefreshStream.setOnClickListener {
            reconnectAttempt = 0
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

    private fun setupMjpegView() {
        binding.ivCameraStream.streamListener = object : MjpegStreamListener {
            override fun onConnected() {
                updateQualityUi(getString(R.string.camera_stream_quality_good), R.color.neon_green)
                binding.tvCameraStatus.text = getString(R.string.camera_stream_live)
                binding.progressStream.visibility = View.GONE
                binding.btnRefreshStream.isEnabled = true
            }

            override fun onFrameReceived() {
                // No extra action needed per frame
            }

            override fun onDisconnected() {
                // Clean disconnect — nothing to do
            }

            override fun onError(message: String) {
                reconnectAttempt++
                binding.progressStream.visibility = View.VISIBLE
                updateQualityUi(
                    getString(R.string.camera_stream_quality_reconnecting),
                    R.color.neon_magenta
                )
                binding.tvCameraStatus.text = getString(
                    R.string.camera_stream_reconnect_attempt,
                    reconnectAttempt
                )
                if (reconnectAttempt > Constants.MAX_MJPEG_RECONNECT_ATTEMPTS) {
                    binding.ivCameraStream.stopStream()
                    showIpInputPanel()
                    binding.btnRefreshStream.isEnabled = true
                }
            }
        }
    }

    private fun setupIpInput() {
        binding.btnConnectIp.setOnClickListener {
            val ip = binding.etChildIp.text?.toString()?.trim().orEmpty()
            if (ip.isBlank()) {
                binding.tilChildIp.error = getString(R.string.hint_child_ip)
                return@setOnClickListener
            }
            if (!isValidIpAddress(ip)) {
                binding.tilChildIp.error = getString(R.string.error_invalid_ip)
                return@setOnClickListener
            }
            binding.tilChildIp.error = null
            childHost = ip
            prefs.saveLastChildHost(ip)
            hideIpInputPanel()
            // Dismiss keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etChildIp.windowToken, 0)
            reconnectAttempt = 0
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

    private fun isValidIpAddress(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return false
            num in 0..255
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
        binding.progressStream.visibility = View.VISIBLE
        updateQualityUi(getString(R.string.camera_stream_quality_connecting), R.color.neon_magenta)
        binding.tvCameraStatus.text = getString(R.string.camera_stream_connecting_child)
        binding.btnRefreshStream.isEnabled = false

        val url = "http://$childHost:${Constants.DEFAULT_MJPEG_PORT}${Constants.MJPEG_STREAM_PATH}"
        binding.ivCameraStream.startStream(url)
    }

    private fun updateQualityUi(text: String, colorRes: Int) {
        binding.tvConnectionQuality.text = text
        binding.tvConnectionQuality.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    override fun onDestroy() {
        binding.ivCameraStream.stopStream()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

