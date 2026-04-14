package com.parentalcontrol.app.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parentalcontrol.app.R
import com.parentalcontrol.app.utils.PermissionUtils

class MainViewModel(private val context: Context) : ViewModel() {

    private val _statusText = MutableLiveData<String>()
    val statusText: LiveData<String> = _statusText

    private val _cameraPermissionGranted = MutableLiveData<Boolean>()
    val cameraPermissionGranted: LiveData<Boolean> = _cameraPermissionGranted

    private val _audioPermissionGranted = MutableLiveData<Boolean>()
    val audioPermissionGranted: LiveData<Boolean> = _audioPermissionGranted

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val cameraOk = PermissionUtils.hasCameraPermission(context)
        val audioOk = PermissionUtils.hasAudioPermission(context)
        _cameraPermissionGranted.value = cameraOk
        _audioPermissionGranted.value = audioOk

        _statusText.value = when {
            cameraOk && audioOk -> context.getString(R.string.status_ready)
            !cameraOk && !audioOk -> context.getString(R.string.status_no_permissions)
            !cameraOk -> context.getString(R.string.status_no_camera_permission)
            else -> context.getString(R.string.status_no_audio_permission)
        }
    }

    fun onPermissionsResult() {
        refreshStatus()
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(context) as T
    }
}
