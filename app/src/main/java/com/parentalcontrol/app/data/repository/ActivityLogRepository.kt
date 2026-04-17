package com.parentalcontrol.app.data.repository

import androidx.lifecycle.LiveData
import com.parentalcontrol.app.data.database.ActivityLogDao
import com.parentalcontrol.app.data.model.ActivityLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityLogRepository(private val dao: ActivityLogDao) {

    val allLogs: LiveData<List<ActivityLog>> = dao.getAllLogs()

    suspend fun addLog(action: String, description: String) {
        withContext(Dispatchers.IO) {
            dao.insert(ActivityLog(
                action = action,
                description = description,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }
    }
}