package com.parentalcontrol.app.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = ActivityLogRepository(application.applicationContext)

    private val _isStreaming = MutableLiveData<Boolean>(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    fun onCameraStarted() {
        _isStreaming.value = true
        viewModelScope.launch {
            logRepository.addLog("CAMERA_START", "Camera monitoring started")
        }
    }

    fun onCameraStopped() {
        if (_isStreaming.value == true) {
            _isStreaming.value = false
            viewModelScope.launch {
                logRepository.addLog("CAMERA_STOP", "Camera monitoring stopped")
            }
        }
    }
}
