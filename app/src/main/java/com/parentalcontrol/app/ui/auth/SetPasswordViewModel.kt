package com.parentalcontrol.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.parentalcontrol.app.R
import com.parentalcontrol.app.data.repository.AuthRepository
import com.parentalcontrol.app.utils.Constants

class SetPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(appContext)

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    fun setPassword(password: String, confirm: String) {
        when {
            password.length < Constants.MIN_PASSWORD_LENGTH ->
                _result.value = appContext.getString(R.string.password_too_short)
            password != confirm -> _result.value = appContext.getString(R.string.passwords_dont_match)
            else -> {
                authRepository.setPassword(password)
                _result.value = appContext.getString(R.string.password_saved)
            }
        }
    }
}
