package com.parentalcontrol.app.ui.camera

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraBinding
import com.parentalcontrol.app.service.MonitoringService
import com.parentalcontrol.app.utils.PermissionUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraViewModel by viewModels {
        CameraViewModelFactory(this)
    }
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (!PermissionUtils.hasCameraPermission(this)) {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startCamera()
        startMonitoringService()
        observeViewModel()

        binding.btnStopCamera.setOnClickListener {
            viewModel.onCameraStopped()
            stopMonitoringService()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                viewModel.onCameraStarted()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java).apply {
            action = MonitoringService.ACTION_START_CAMERA
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
        viewModel.isStreaming.observe(this) { streaming ->
            binding.tvStreamStatus.text = if (streaming)
                getString(R.string.camera_active)
            else
                getString(R.string.camera_inactive)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
