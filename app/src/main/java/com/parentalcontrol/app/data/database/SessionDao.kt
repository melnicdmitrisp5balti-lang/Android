<<<<<<< HEAD
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
=======
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
>>>>>>> a1f791063e2890681a681f6a89b0ee2ae0427c6b
}