package com.parentalcontrol.app.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.R
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import com.parentalcontrol.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val logRepository: ActivityLogRepository,
    private val context: Context
) : ViewModel() {

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _passwordStatus = MutableLiveData<String>()
    val passwordStatus: LiveData<String> = _passwordStatus

    init {
        refreshPasswordStatus()
    }

    fun refreshPasswordStatus() {
        _passwordStatus.value = if (authRepository.isPasswordSet())
            context.getString(R.string.password_set)
        else
            context.getString(R.string.password_not_set)
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
            _message.postValue(context.getString(R.string.logs_cleared))
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(
            AuthRepository(context),
            ActivityLogRepository(context),
            context
        ) as T
    }
}
