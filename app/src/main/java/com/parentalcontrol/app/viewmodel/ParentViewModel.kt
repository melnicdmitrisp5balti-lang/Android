package com.parentalcontrol.app.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.SessionEntity
import com.parentalcontrol.app.service.ParentSocketClient
import kotlinx.coroutines.launch

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val client = ParentSocketClient()
    private val sessionDao = AppDatabase.getInstance(appContext).sessionDao()

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    fun validateCode(code: String): Boolean = Regex("^\\d{6}$").matches(code)

    fun connect(host: String, port: Int, code: String) {
        viewModelScope.launch {
            if (!validateCode(code)) {
                _status.value = "Код должен состоять из 6 цифр"
                _connected.value = false
                return@launch
            }

            val result = client.connect(host, port, code)
            if (result.isSuccess) {
                val childName = result.getOrNull().orEmpty()
                _status.value = "Подключено к: $childName"
                _connected.value = true
                sessionDao.insert(
                    SessionEntity(
                        childDeviceId = childName,
                        parentDeviceId = Settings.Secure.getString(
                            appContext.contentResolver,
                            Settings.Secure.ANDROID_ID
                        ).orEmpty(),
                        connectionCode = code
                    )
                )
            } else {
                _status.value = result.exceptionOrNull()?.message ?: "Не удалось подключиться"
                _connected.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
    }
}
