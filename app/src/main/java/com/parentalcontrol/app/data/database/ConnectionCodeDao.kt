package com.parentalcontrol.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parentalcontrol.app.data.model.ConnectionCode

@Dao
interface ConnectionCodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connectionCode: ConnectionCode): Long

    @Query("SELECT * FROM ConnectionCodes WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1")
    suspend fun getActiveCode(): ConnectionCode?

    @Query("SELECT * FROM ConnectionCodes WHERE code = :code AND is_active = 1 LIMIT 1")
    suspend fun getActiveByCode(code: String): ConnectionCode?

    @Query("UPDATE ConnectionCodes SET is_active = 0")
    suspend fun deactivateAll()
}
