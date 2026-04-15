package com.parentalcontrol.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.parentalcontrol.app.data.model.ActivityLog
import com.parentalcontrol.app.data.model.ConnectionCode
import com.parentalcontrol.app.data.model.SessionEntity
import com.parentalcontrol.app.utils.Constants

@Database(
    entities = [ActivityLog::class, ConnectionCode::class, SessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityLogDao(): ActivityLogDao
    abstract fun connectionCodeDao(): ConnectionCodeDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
