package com.parentalcontrol.app.ui.child

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.cloud.FirebaseConnectionManager
import com.parentalcontrol.app.databinding.ActivityChildAutoRegisterBinding
import com.parentalcontrol.app.service.ChildAudioService
import com.parentalcontrol.app.service.ChildCameraService
import com.parentalcontrol.app.utils.CodeGenerator
import com.parentalcontrol.app.utils.PermissionUtils
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.launch

/**
 * Displays the child's 6-digit connection code and automatically registers the
 * device with Firebase so the parent can find it from anywhere on the internet.
 *
 * On launch it:
 * 1. Generates (or restores) the 6-digit code.
 * 2. Registers the device in Firebase.
 * 3. Requests camera + microphone permissions.
 * 4. Starts [ChildCameraService] to begin WebRTC streaming.
 */
class ChildAutoRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildAutoRegisterBinding
    private lateinit var prefs: PreferenceManager
    private val firebaseManager = FirebaseConnectionManager()
    private var connectionCode: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startChildServices()
        } else {
            Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildAutoRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        setupClickListeners()
        loadOrGenerateCode()
    }

    private fun loadOrGenerateCode() {
        val stored = prefs.getChildConnectionCode()
        connectionCode = if (!stored.isNullOrBlank()) stored else generateNewCode()
        displayCode(connectionCode)
        registerWithFirebase(connectionCode)
        ensurePermissionsAndStart()
    }

    private fun generateNewCode(): String {
        val code = CodeGenerator.generateSixDigitCode()
        prefs.saveChildConnectionCode(code)
        return code
    }

    private fun displayCode(code: String) {
        binding.tvConnectionCode.text = code
        binding.tvCloudStatus.text = getString(R.string.child_registering)
    }

    private fun registerWithFirebase(code: String) {
        val deviceName = Build.MODEL
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()

        lifecycleScope.launch {
            val result = firebaseManager.registerDevice(code, deviceName, deviceId)
            if (result != null) {
                binding.tvCloudStatus.text = getString(R.string.child_registered_cloud)
                binding.tvCloudStatus.setTextColor(
                    ContextCompat.getColor(this@ChildAutoRegisterActivity, R.color.neon_green)
                )
            } else {
                binding.tvCloudStatus.text = getString(R.string.child_register_failed)
                binding.tvCloudStatus.setTextColor(
                    ContextCompat.getColor(this@ChildAutoRegisterActivity, R.color.neon_magenta)
                )
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCopyCode.setOnClickListener {
            if (connectionCode.isBlank()) return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("code", connectionCode))
            Toast.makeText(this, getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
        }

        binding.btnRegenerateCode.setOnClickListener {
            // Stop the current WebRTC service before changing the code
            stopChildServices()
            lifecycleScope.launch {
                firebaseManager.deactivateDevice(connectionCode)
                connectionCode = generateNewCode()
                displayCode(connectionCode)
                registerWithFirebase(connectionCode)
                ensurePermissionsAndStart()
            }
        }
    }

    private fun ensurePermissionsAndStart() {
        val missing = PermissionUtils.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startChildServices()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startChildServices() {
        // Full camera + audio WebRTC service
        val intent = Intent(this, ChildCameraService::class.java).apply {
            action = ChildCameraService.ACTION_START
            putExtra(ChildCameraService.EXTRA_CODE, connectionCode)
        }
        startForegroundService(intent)
        binding.tvStreamStatus.text = getString(R.string.child_stream_active)
        binding.tvStreamStatus.setTextColor(
            ContextCompat.getColor(this, R.color.neon_green)
        )
    }

    private fun stopChildServices() {
        startService(
            Intent(this, ChildCameraService::class.java).apply {
                action = ChildCameraService.ACTION_STOP
            }
        )
        startService(
            Intent(this, ChildAudioService::class.java).apply {
                action = ChildAudioService.ACTION_STOP
            }
        )
    }

    override fun onDestroy() {
        // Keep the service running in the background; only stop it explicitly.
        super.onDestroy()
    }
}
