package com.parentalcontrol.app.ui.camera

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import kotlinx.coroutines.launch

class CameraViewModel(private val logRepository: ActivityLogRepository) : ViewModel() {

    private val _isStreaming = MutableLiveData<Boolean>(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    fun onCameraStarted() {
        _isStreaming.value = true
        viewModelScope.launch {
            logRepository.addLog("CAMERA_START", "Camera monitoring started")
        }
    }

    fun onCameraStopped() {
        _isStreaming.value = false
        viewModelScope.launch {
            logRepository.addLog("CAMERA_STOP", "Camera monitoring stopped")
        }
    }
}

class CameraViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CameraViewModel(ActivityLogRepository(context)) as T
    }
}
