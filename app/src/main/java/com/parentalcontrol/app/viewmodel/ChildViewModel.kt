package com.parentalcontrol.app.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.ConnectionCode
import com.parentalcontrol.app.utils.CodeGenerator
import com.parentalcontrol.app.utils.PreferenceManager
import kotlinx.coroutines.launch

class ChildViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = PreferenceManager(appContext)
    private val connectionCodeDao = AppDatabase.getInstance(appContext).connectionCodeDao()

    private val _code = MutableLiveData<String>()
    val code: LiveData<String> = _code

    private val _connectionStatus = MutableLiveData("Ожидание подключения...")
    val connectionStatus: LiveData<String> = _connectionStatus

    init {
        loadOrGenerateCode()
    }

    fun loadOrGenerateCode() {
        val stored = prefs.getChildConnectionCode()
        if (!stored.isNullOrBlank()) {
            _code.value = stored
            return
        }
        regenerateCode()
    }

    fun regenerateCode() {
        viewModelScope.launch {
            val newCode = CodeGenerator.generateSixDigitCode()
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
        }
    }

    fun updateConnectionStatus(status: String) {
        _connectionStatus.value = status
    }
}
