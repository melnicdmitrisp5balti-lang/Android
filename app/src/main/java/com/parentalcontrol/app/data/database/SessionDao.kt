package com.parentalcontrol.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parentalcontrol.app.data.model.SessionEntity

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions WHERE connection_code = :connectionCode AND disconnected_at IS NULL")
    suspend fun getActiveByCode(connectionCode: String): SessionEntity?

    @Query("UPDATE sessions SET disconnected_at = :disconnectedAt WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, disconnectedAt: Long)
}