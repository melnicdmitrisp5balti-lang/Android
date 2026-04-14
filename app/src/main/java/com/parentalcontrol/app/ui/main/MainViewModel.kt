package com.parentalcontrol.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parentalcontrol.app.R
import com.parentalcontrol.app.utils.PermissionUtils

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

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
        val cameraOk = PermissionUtils.hasCameraPermission(appContext)
        val audioOk = PermissionUtils.hasAudioPermission(appContext)
        _cameraPermissionGranted.value = cameraOk
        _audioPermissionGranted.value = audioOk

        _statusText.value = when {
            cameraOk && audioOk -> appContext.getString(R.string.status_ready)
            !cameraOk && !audioOk -> appContext.getString(R.string.status_no_permissions)
            !cameraOk -> appContext.getString(R.string.status_no_camera_permission)
            else -> appContext.getString(R.string.status_no_audio_permission)
        }
    }

    fun onPermissionsResult() {
        refreshStatus()
    }
}
