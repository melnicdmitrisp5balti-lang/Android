package com.parentalcontrol.app.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityParentMainBinding
import com.parentalcontrol.app.databinding.DialogEnterCodeBinding
import com.parentalcontrol.app.databinding.DialogQrScannerBinding
import com.parentalcontrol.app.ui.logs.ActivityLogActivity
import com.parentalcontrol.app.viewmodel.ParentViewModel

class ParentMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentMainBinding
    private val viewModel: ParentViewModel by viewModels()
    private var lastConnectionEvent = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupQuickActionDropdown()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupQuickActionDropdown() {
        val options = listOf(
            getString(R.string.quick_action_camera_audio),
            getString(R.string.quick_action_camera),
            getString(R.string.quick_action_audio),
            getString(R.string.quick_action_logs),
            getString(R.string.quick_action_location)
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            options
        )
        binding.dropdownQuickAction.setAdapter(adapter)
        binding.dropdownQuickAction.setText(options.first(), false)
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener { showCodeDialog() }
        binding.btnScanQr.setOnClickListener { showQrDialog() }

        binding.btnCameraStream.setOnClickListener { openCamera() }
        binding.btnAudioStream.setOnClickListener { openAudio() }
        binding.btnLogs.setOnClickListener { openLogs() }
        binding.btnLocation.setOnClickListener { openLocationHint() }
    }

    private fun observeViewModel() {
        viewModel.status.observe(this) { status ->
            binding.tvParentStatus.text = status
        }
        viewModel.connected.observe(this) { connected ->
            binding.connectionControls.alpha = if (connected) 1f else 0.5f
            binding.btnCameraStream.isEnabled = connected
            binding.btnAudioStream.isEnabled = connected
            binding.btnLogs.isEnabled = connected
            binding.btnLocation.isEnabled = connected
        }
        viewModel.connectionEvent.observe(this) { event ->
            if (event <= 0L || event == lastConnectionEvent) return@observe
            lastConnectionEvent = event
            openSelectedQuickAction()
        }
    }

    private fun showCodeDialog() {
        val dialogBinding = DialogEnterCodeBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Подключиться") { _, _ ->
                val code = dialogBinding.etConnectionCode.text?.toString().orEmpty()
                viewModel.connect(code)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showQrDialog() {
        val dialogBinding = DialogQrScannerBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Ок", null)
            .show()
    }

    private fun openSelectedQuickAction() {
        when (binding.dropdownQuickAction.text?.toString().orEmpty()) {
            getString(R.string.quick_action_camera) -> openCamera()
            getString(R.string.quick_action_audio) -> openAudio()
            getString(R.string.quick_action_logs) -> openLogs()
            getString(R.string.quick_action_location) -> openLocationHint()
            else -> {
                openCamera()
                openAudio()
            }
        }
    }

    private fun openCamera() {
        startActivity(Intent(this, CameraStreamActivity::class.java))
    }

    private fun openAudio() {
        startActivity(Intent(this, AudioStreamActivity::class.java))
    }

    private fun openLogs() {
        startActivity(Intent(this, ActivityLogActivity::class.java))
    }

    private fun openLocationHint() {
        Toast.makeText(this, "Местоположение доступно при наличии разрешения", Toast.LENGTH_SHORT).show()
    }
}
