package com.parentalcontrol.app.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parentalcontrol.app.R
import com.parentalcontrol.app.data.repository.AuthRepository

class SetPasswordViewModel(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    fun setPassword(password: String, confirm: String) {
        when {
            password.length < 8 -> _result.value = context.getString(R.string.password_too_short)
            password != confirm -> _result.value = context.getString(R.string.passwords_dont_match)
            else -> {
                authRepository.setPassword(password)
                _result.value = context.getString(R.string.password_saved)
            }
        }
    }
}

class SetPasswordViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SetPasswordViewModel(AuthRepository(context), context) as T
    }
}
