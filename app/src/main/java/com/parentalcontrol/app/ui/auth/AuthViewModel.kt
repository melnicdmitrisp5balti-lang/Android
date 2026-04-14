package com.parentalcontrol.app.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parentalcontrol.app.data.repository.AuthRepository
import com.parentalcontrol.app.utils.PreferenceManager

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val prefs: PreferenceManager
) : ViewModel() {

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

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(
            AuthRepository(context),
            PreferenceManager(context)
        ) as T
    }
}
