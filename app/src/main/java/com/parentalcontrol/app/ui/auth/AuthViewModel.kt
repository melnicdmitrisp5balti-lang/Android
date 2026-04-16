package com.parentalcontrol.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parentalcontrol.app.data.repository.AuthRepository
import com.parentalcontrol.app.utils.PreferenceManager

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(appContext)
    private val prefs = PreferenceManager(appContext)

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> = _authResult

    private val _noPasswordSet = MutableLiveData<Boolean>()
    val noPasswordSet: LiveData<Boolean> = _noPasswordSet

    init {
        _noPasswordSet.value = !authRepository.isPasswordSet()
    }

    fun authenticate(password: String) {
        if (!authRepository.isPasswordSet()) {
            _noPasswordSet.value = true
            return
        }
        val result = authRepository.verifyPassword(password)
        if (result) {
            prefs.setAuthenticated(true)
        }
        _authResult.value = result
    }
}
