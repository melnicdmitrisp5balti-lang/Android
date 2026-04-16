package com.parentalcontrol.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.parentalcontrol.app.data.model.ActivityLog

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activityLog: ActivityLog): Long

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    @SuppressWarnings(RoomWarnings.QUERY_RUNTIME_UNEXPLAINED_QUERY)
    fun getAllLogs(): LiveData<List<ActivityLog>>

    @Query("DELETE FROM activity_logs")
    fun clearAll()
}