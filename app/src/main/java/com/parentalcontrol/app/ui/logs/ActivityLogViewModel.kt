package com.parentalcontrol.app.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.ActivityLog
import com.parentalcontrol.app.data.repository.ActivityLogRepository

class ActivityLogViewModel(application: Application) : AndroidViewModel(application) {
    private val activityLogDao = AppDatabase.getInstance(application).activityLogDao()
    private val repository = ActivityLogRepository(activityLogDao)
    val logs: LiveData<List<ActivityLog>> = repository.allLogs
}
