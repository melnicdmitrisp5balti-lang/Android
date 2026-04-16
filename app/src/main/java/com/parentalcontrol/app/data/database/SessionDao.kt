package com.parentalcontrol.app.data.database

import androidx.room.*
import com.parentalcontrol.app.data.model.SessionEntity

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE connection_code = :connectionCode AND disconnected_at IS NULL")
    @SuppressWarnings(RoomWarnings.QUERY_RUNTIME_UNEXPLAINED_QUERY)
    fun getActiveByCode(connectionCode: String): SessionEntity?

    @Query("UPDATE sessions SET disconnected_at = :disconnectedAt WHERE id = :sessionId")
    fun closeSession(sessionId: Long, disconnectedAt: Long)
}