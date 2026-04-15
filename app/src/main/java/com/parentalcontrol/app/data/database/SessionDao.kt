package com.parentalcontrol.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.parentalcontrol.app.data.model.SessionEntity

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query(
        "SELECT * FROM Sessions WHERE connection_code = :code AND disconnected_at IS NULL ORDER BY connected_at DESC LIMIT 1"
    )
    suspend fun getActiveByCode(code: String): SessionEntity?

    @Query("UPDATE Sessions SET disconnected_at = :disconnectedAt WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, disconnectedAt: Long)
}
