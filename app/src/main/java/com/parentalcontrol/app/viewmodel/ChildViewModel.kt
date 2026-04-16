package com.parentalcontrol.app.viewmodel

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.cloud.CloudSignalingClient
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.ConnectionCode
import com.parentalcontrol.app.utils.CodeGenerator
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.launch

class ChildViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = PreferenceManager(appContext)
    private val connectionCodeDao = AppDatabase.getInstance(appContext).connectionCodeDao()
    private val cloudSignaling = CloudSignalingClient()

    private val _code = MutableLiveData<String>()
    val code: LiveData<String> = _code

    private val _connectionStatus = MutableLiveData("Ожидание подключения...")
    val connectionStatus: LiveData<String> = _connectionStatus
    private val _parentConnected = MutableLiveData(false)
    val parentConnected: LiveData<Boolean> = _parentConnected

    init {
        loadOrGenerateCode()
    }

    fun loadOrGenerateCode() {
        val stored = prefs.getChildConnectionCode()
        if (!stored.isNullOrBlank()) {
            _code.value = stored
            // Re-register with the cloud in case the app was restarted.
            viewModelScope.launch { registerWithCloud(stored) }
            return
        }
        regenerateCode()
    }

    fun regenerateCode() {
        if (_parentConnected.value == true) return
        viewModelScope.launch {
            val oldCode = prefs.getChildConnectionCode()
            val newCode = CodeGenerator.generateSixDigitCode()

            // Deactivate the old code in the cloud before registering the new one.
            if (!oldCode.isNullOrBlank()) {
                cloudSignaling.deactivateDevice(oldCode)
            }

            prefs.saveChildConnectionCode(newCode)
            connectionCodeDao.deactivateAll()
            connectionCodeDao.insert(
                ConnectionCode(
                    code = newCode,
                    deviceId = Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ).orEmpty()
                )
            )
            _code.postValue(newCode)
            _connectionStatus.postValue("Ожидание подключения...")
            _parentConnected.postValue(false)

            registerWithCloud(newCode)
        }
    }

    private suspend fun registerWithCloud(code: String) {
        val deviceId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        cloudSignaling.registerChildDevice(
            code = code,
            deviceName = Build.MODEL,
            deviceId = deviceId
        )
    }

    fun updateConnectionStatus(status: String, parentConnected: Boolean) {
        _connectionStatus.value = status
        _parentConnected.value = parentConnected
    }
}
