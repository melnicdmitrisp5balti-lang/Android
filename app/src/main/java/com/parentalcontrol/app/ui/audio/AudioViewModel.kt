package com.parentalcontrol.app.ui.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.parentalcontrol.app.data.repository.ActivityLogRepository
import com.parentalcontrol.app.utils.Constants
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = ActivityLogRepository(application.applicationContext)

    private val _isRecording = MutableLiveData<Boolean>(false)
    val isRecording: LiveData<Boolean> = _isRecording

    fun startAudio() {
        _isRecording.value = true
        viewModelScope.launch {
            logRepository.addLog(Constants.LOG_AUDIO_START, "Audio monitoring started")
        }
    }

    fun stopAudio() {
        if (_isRecording.value == true) {
            _isRecording.value = false
            viewModelScope.launch {
                logRepository.addLog(Constants.LOG_AUDIO_STOP, "Audio monitoring stopped")
            }
        }
    }
}
