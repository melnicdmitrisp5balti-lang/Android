package com.parentalcontrol.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import com.parentalcontrol.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(appContext)
    private val logRepository = ActivityLogRepository(appContext)

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _passwordStatus = MutableLiveData<String>()
    val passwordStatus: LiveData<String> = _passwordStatus

    init {
        refreshPasswordStatus()
    }

    fun refreshPasswordStatus() {
        _passwordStatus.value = if (authRepository.isPasswordSet())
            appContext.getString(R.string.password_set)
        else
            appContext.getString(R.string.password_not_set)
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
            _message.postValue(appContext.getString(R.string.logs_cleared))
        }
    }
}
