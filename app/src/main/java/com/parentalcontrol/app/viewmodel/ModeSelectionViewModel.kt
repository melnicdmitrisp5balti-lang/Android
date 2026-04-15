package com.parentalcontrol.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ModeSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedMode = MutableLiveData<String>()
    val selectedMode: LiveData<String> = _selectedMode

    fun selectChildMode() {
        _selectedMode.value = "child"
    }

    fun selectParentMode() {
        _selectedMode.value = "parent"
    }
}
