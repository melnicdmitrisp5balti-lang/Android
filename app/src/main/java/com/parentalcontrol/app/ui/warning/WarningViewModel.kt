package com.parentalcontrol.app.ui.warning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parentalcontrol.app.utils.PreferenceManager

class WarningViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application.applicationContext)

    private val _isWarningAccepted = MutableLiveData<Boolean>()
    val isWarningAccepted: LiveData<Boolean> = _isWarningAccepted

    init {
        _isWarningAccepted.value = prefs.isWarningAccepted()
    }

    fun acceptWarning() {
        prefs.setWarningAccepted(true)
        _isWarningAccepted.value = true
    }

    fun declineWarning() {
        _isWarningAccepted.value = false
    }
}
