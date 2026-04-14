package com.parentalcontrol.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityMainBinding
import com.parentalcontrol.app.ui.audio.AudioActivity
import com.parentalcontrol.app.ui.camera.CameraActivity
import com.parentalcontrol.app.ui.logs.ActivityLogActivity
import com.parentalcontrol.app.ui.settings.SettingsActivity
import com.parentalcontrol.app.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
        }
        viewModel.onPermissionsResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
        requestPermissionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener {
            if (PermissionUtils.hasCameraPermission(this)) {
                startActivity(Intent(this, CameraActivity::class.java))
            } else {
                showPermissionRationale(Manifest.permission.CAMERA)
            }
        }

        binding.btnMicrophone.setOnClickListener {
            if (PermissionUtils.hasAudioPermission(this)) {
                startActivity(Intent(this, AudioActivity::class.java))
            } else {
                showPermissionRationale(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnActivityLog.setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.statusText.observe(this) { status ->
            binding.tvStatus.text = status
        }

        viewModel.cameraPermissionGranted.observe(this) { granted ->
            binding.ivCameraIndicator.alpha = if (granted) 1.0f else 0.3f
        }

        viewModel.audioPermissionGranted.observe(this) { granted ->
            binding.ivMicIndicator.alpha = if (granted) 1.0f else 0.3f
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = PermissionUtils.REQUIRED_PERMISSIONS
        val notGranted = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun showPermissionRationale(permission: String) {
        val message = when (permission) {
            Manifest.permission.CAMERA -> getString(R.string.camera_permission_rationale)
            Manifest.permission.RECORD_AUDIO -> getString(R.string.audio_permission_rationale)
            else -> getString(R.string.permission_required_generic)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        permissionLauncher.launch(arrayOf(permission))
    }
}
