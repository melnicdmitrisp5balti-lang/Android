package com.parentalcontrol.app.ui.logs

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parentalcontrol.app.data.model.ActivityLog
import com.parentalcontrol.app.data.repository.ActivityLogRepository

class ActivityLogViewModel(private val repository: ActivityLogRepository) : ViewModel() {
    val logs: LiveData<List<ActivityLog>> = repository.allLogs
}

class ActivityLogViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ActivityLogViewModel(ActivityLogRepository(context)) as T
    }
}
