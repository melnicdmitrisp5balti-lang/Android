package com.parentalcontrol.app.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.parentalcontrol.app.databinding.ActivityParentMainBinding
import com.parentalcontrol.app.databinding.DialogEnterCodeBinding
import com.parentalcontrol.app.databinding.DialogQrScannerBinding
import com.parentalcontrol.app.ui.logs.ActivityLogActivity
import com.parentalcontrol.app.viewmodel.ParentViewModel

class ParentMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentMainBinding
    private val viewModel: ParentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener { showCodeDialog() }
        binding.btnScanQr.setOnClickListener { showQrDialog() }

        binding.btnCameraStream.setOnClickListener {
            startActivity(Intent(this, CameraStreamActivity::class.java))
        }
        binding.btnAudioStream.setOnClickListener {
            startActivity(Intent(this, AudioStreamActivity::class.java))
        }
        binding.btnLogs.setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }
        binding.btnLocation.setOnClickListener {
            Toast.makeText(this, "Местоположение доступно при наличии разрешения", Toast.LENGTH_SHORT).show()
        }
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
    }

    private fun showCodeDialog() {
        val dialogBinding = DialogEnterCodeBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Подключиться") { _, _ ->
                val code = dialogBinding.etConnectionCode.text?.toString().orEmpty()
                val host = dialogBinding.etHost.text?.toString().orEmpty().ifBlank { "10.0.2.2" }
                val port = dialogBinding.etPort.text?.toString()?.toIntOrNull() ?: 5050
                viewModel.connect(host, port, code)
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
}
