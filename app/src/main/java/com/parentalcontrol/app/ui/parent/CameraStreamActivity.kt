package com.parentalcontrol.app.ui.parent

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding
import com.parentalcontrol.app.service.ParentSocketClient
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.launch

class CameraStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraStreamBinding
    private val socketClient = ParentSocketClient()
    private lateinit var prefs: PreferenceManager
    private var childHost: String = ""
    private var connectionCode: String = ""

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
        if (childHost.isBlank() || connectionCode.isBlank()) {
            binding.tvCameraStatus.text = getString(R.string.camera_stream_no_active_session)
            binding.btnRefreshStream.isEnabled = false
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
                val childName = result.getOrNull().orEmpty()
                binding.tvCameraStatus.text =
                    getString(R.string.camera_stream_child_ready, childName)
            } else {
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
