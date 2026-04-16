<<<<<<< HEAD
package com.parentalcontrol.app.data.database

import androidx.room.*
import com.parentalcontrol.app.data.model.ActivityLog

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activityLog: ActivityLog): Long

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    @SuppressWarnings(RoomWarnings.QUERY_RUNTIME_UNEXPLAINED_QUERY)
    fun getAllLogs(): List<ActivityLog>

    @Query("DELETE FROM activity_logs")
    fun clearAll()
=======
package com.parentalcontrol.app.data.database

import androidx.room.*
import com.parentalcontrol.app.data.model.ActivityLog

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activityLog: ActivityLog): Long

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    @SuppressWarnings(RoomWarnings.QUERY_RUNTIME_UNEXPLAINED_QUERY)
    fun getAllLogs(): List<ActivityLog>

    @Query("DELETE FROM activity_logs")
    fun clearAll()
>>>>>>> a1f791063e2890681a681f6a89b0ee2ae0427c6b
}