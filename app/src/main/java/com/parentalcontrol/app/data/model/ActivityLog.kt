package com.parentalcontrol.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
