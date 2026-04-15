package com.parentalcontrol.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ConnectionCodes")
data class ConnectionCode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
