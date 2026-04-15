package com.parentalcontrol.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "child_device_id")
    val childDeviceId: String,
    @ColumnInfo(name = "parent_device_id")
    val parentDeviceId: String,
    @ColumnInfo(name = "connection_code")
    val connectionCode: String,
    @ColumnInfo(name = "connected_at")
    val connectedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "disconnected_at")
    val disconnectedAt: Long? = null
)
