package com.parentalcontrol.app.ui.parent

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.databinding.ActivityCameraStreamBinding
import com.parentalcontrol.app.utils.PermissionUtils

class CameraStreamActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CameraStreamActivity"
    }

    private lateinit var binding: ActivityCameraStreamBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }

        if (PermissionUtils.hasCameraPermission(this)) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            try {
                bindCamera(lensFacing)
                binding.tvCameraStatus.text = getString(R.string.camera_stream_live)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Requested lens is not available", e)
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && tryFallbackToBackCamera()) {
                    return@addListener
                }
                showBackCameraError()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Camera lifecycle error", e)
                binding.tvCameraStatus.text = getString(R.string.camera_lifecycle_error)
                Toast.makeText(this, getString(R.string.camera_lifecycle_error), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera stream", e)
                binding.tvCameraStatus.text = getString(R.string.camera_error)
                Toast.makeText(this, getString(R.string.camera_error), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(targetLensFacing: Int) {
        val provider = cameraProvider
            ?: throw IllegalStateException("Camera provider is unavailable")
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            this,
            CameraSelector.Builder().requireLensFacing(targetLensFacing).build(),
            preview
        )
    }

    private fun tryFallbackToBackCamera(): Boolean {
        lensFacing = CameraSelector.LENS_FACING_BACK
        return try {
            bindCamera(lensFacing)
            binding.tvCameraStatus.text = getString(R.string.camera_stream_live)
            Toast.makeText(this, getString(R.string.front_camera_unavailable), Toast.LENGTH_SHORT).show()
            true
        } catch (fallbackException: Exception) {
            Log.e(TAG, "Fallback to back camera failed", fallbackException)
            showBackCameraError()
            false
        }
    }

    private fun showBackCameraError() {
        binding.tvCameraStatus.text = getString(R.string.back_camera_error)
        Toast.makeText(this, getString(R.string.back_camera_error), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        super.onDestroy()
    }
}
