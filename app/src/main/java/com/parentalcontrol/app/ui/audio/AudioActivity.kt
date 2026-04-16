package com.parentalcontrol.app.ui.audio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityAudioBinding
import com.parentalcontrol.app.service.MonitoringService
import com.parentalcontrol.app.utils.PermissionUtils

class AudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioBinding
    private val viewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.microphone)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (!PermissionUtils.hasAudioPermission(this)) {
            Toast.makeText(this, getString(R.string.audio_permission_required), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStartAudio.setOnClickListener {
            viewModel.startAudio()
            startMonitoringService()
        }

        binding.btnStopAudio.setOnClickListener {
            stopAudioMonitoring()
        }
    }

    private fun stopAudioMonitoring() {
        viewModel.stopAudio()
        stopMonitoringService()
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_AUDIO
        }
        startForegroundService(intent)
    }

    private fun stopMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_STOP
        }
        startService(intent)
    }

    private fun observeViewModel() {
        viewModel.isRecording.observe(this) { recording ->
            binding.tvAudioStatus.text = if (recording)
                getString(R.string.audio_active)
            else
                getString(R.string.audio_inactive)
            binding.btnStartAudio.isEnabled = !recording
            binding.btnStopAudio.isEnabled = recording
            binding.ivMicIcon.alpha = if (recording) 1.0f else 0.3f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioMonitoring()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
