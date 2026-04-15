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
import com.parentalcontrol.app.service.ChildSocketServer
import com.parentalcontrol.app.service.MonitoringService
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PermissionUtils
import com.parentalcontrol.app.viewmodel.ChildViewModel

class ChildMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildMainBinding
    private val viewModel: ChildViewModel by viewModels()

    private val connectionStatusReceiver = ChildConnectionStatusReceiver { status ->
        viewModel.updateConnectionStatus(status)
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
        ensurePermissionsAndStartMonitoring()
    }

    private fun observeViewModel() {
        viewModel.code.observe(this) { code ->
            binding.tvConnectionCode.text = code
        }
        viewModel.connectionStatus.observe(this) { status ->
            binding.tvConnectionStatus.text = status
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
        startForegroundService(
            Intent(this, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_START_CAMERA
            }
        )
        startForegroundService(
            Intent(this, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_START_AUDIO
            }
        )
        startChildServer()
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
