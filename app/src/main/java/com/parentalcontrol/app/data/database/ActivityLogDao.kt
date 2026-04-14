package com.parentalcontrol.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.parentalcontrol.app.data.model.ActivityLog

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insert(log: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<ActivityLog>>

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()
}
