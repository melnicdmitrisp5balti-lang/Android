package com.parentalcontrol.app.ui.audio

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import kotlinx.coroutines.launch

class AudioViewModel(private val logRepository: ActivityLogRepository) : ViewModel() {

    private val _isRecording = MutableLiveData<Boolean>(false)
    val isRecording: LiveData<Boolean> = _isRecording

    fun startAudio() {
        _isRecording.value = true
        viewModelScope.launch {
            logRepository.addLog("AUDIO_START", "Audio monitoring started")
        }
    }

    fun stopAudio() {
        if (_isRecording.value == true) {
            _isRecording.value = false
            viewModelScope.launch {
                logRepository.addLog("AUDIO_STOP", "Audio monitoring stopped")
            }
        }
    }
}

class AudioViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AudioViewModel(ActivityLogRepository(context)) as T
    }
}
