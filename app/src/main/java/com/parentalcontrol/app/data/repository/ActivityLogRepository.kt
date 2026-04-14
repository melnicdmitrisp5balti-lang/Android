package com.parentalcontrol.app.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.parentalcontrol.app.data.database.AppDatabase
import com.parentalcontrol.app.data.model.ActivityLog

class ActivityLogRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).activityLogDao()

    val allLogs: LiveData<List<ActivityLog>> = dao.getAllLogs()

    suspend fun addLog(action: String, description: String) {
        dao.insert(ActivityLog(action = action, description = description))
    }

    suspend fun clearLogs() {
        dao.clearAll()
    }
}
