package com.parentalcontrol.app.ui.child

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityChildMainBinding
import com.parentalcontrol.app.service.ChildCameraService
import com.parentalcontrol.app.service.ChildSocketServer
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PermissionUtils
import com.parentalcontrol.app.viewmodel.ChildViewModel
import java.net.Inet4Address
import java.net.NetworkInterface

class ChildMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildMainBinding
    private val viewModel: ChildViewModel by viewModels()
    private var monitoringStarted = false
    private var childCameraServiceStartRequested = false

    private val connectionStatusReceiver = ChildConnectionStatusReceiver { status, parentConnected ->
        viewModel.updateConnectionStatus(status, parentConnected)
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startMonitoring()
        } else {
            Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
        updateStreamUrl()
        ensurePermissionsAndStartMonitoring()
    }

    private fun observeViewModel() {
        viewModel.code.observe(this) { code ->
            binding.tvConnectionCode.text = code
            updateStreamUrl()
            // If monitoring was started but service code was not yet available, start it now.
            if (monitoringStarted && code.isNotBlank() && !childCameraServiceStartRequested) {
                startChildCameraService(code)
            }
        }
        viewModel.connectionStatus.observe(this) { status ->
            binding.tvConnectionStatus.text = status
        }
        viewModel.parentConnected.observe(this) { connected ->
            binding.btnRegenerateCode.isEnabled = !connected
            if (connected) {
                binding.btnRegenerateCode.text = getString(R.string.generate_code_locked)
            } else {
                binding.btnRegenerateCode.text = getString(R.string.generate_new_code)
            }
        }
    }

    private fun updateStreamUrl() {
        val ip = getLocalIpAddress()
        binding.tvStreamUrl.text = getString(
            R.string.stream_url_format,
            ip,
            Constants.DEFAULT_MJPEG_PORT,
            Constants.MJPEG_STREAM_PATH
        )
    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.filterIsInstance<Inet4Address>()
                ?.filter { !it.isLoopbackAddress }
                ?.firstOrNull()?.hostAddress ?: "..."
        } catch (e: Exception) {
            "..."
        }
    }

    private fun setupClickListeners() {
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvConnectionCode.text.toString()
            if (code.isBlank()) return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("connection_code", code))
            Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show()
        }

        binding.btnRegenerateCode.setOnClickListener {
            viewModel.regenerateCode()
            startChildServer()
        }
    }

    private fun startMonitoring() {
        monitoringStarted = true
        val code = viewModel.code.value.orEmpty()
        if (code.isNotBlank()) {
            startChildCameraService(code)
        }
        startChildServer()
    }

    private fun startChildCameraService(code: String) {
        if (childCameraServiceStartRequested) return
        childCameraServiceStartRequested = true
        startForegroundService(
            Intent(this, ChildCameraService::class.java).apply {
                action = ChildCameraService.ACTION_START
                putExtra(ChildCameraService.EXTRA_CODE, code)
            }
        )
    }

    private fun ensurePermissionsAndStartMonitoring() {
        val missingPermissions = PermissionUtils.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            startMonitoring()
            return
        }
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    private fun startChildServer() {
        val code = viewModel.code.value.orEmpty()
        if (code.isBlank()) return
        startService(Intent(this, ChildSocketServer::class.java).apply {
            putExtra(ChildSocketServer.EXTRA_CODE, code)
        })
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Constants.ACTION_CHILD_CONNECTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectionStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(connectionStatusReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(connectionStatusReceiver) }
    }
}
