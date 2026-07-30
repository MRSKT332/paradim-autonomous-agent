package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.TaskExecutionDao
import com.example.data.entity.TaskExecutionEntity

@Database(
    entities = [TaskExecutionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TaskHistoryDatabase : RoomDatabase() {
    abstract fun taskExecutionDao(): TaskExecutionDao

    companion object {
        @Volatile
        private var INSTANCE: TaskHistoryDatabase? = null

        fun getDatabase(context: Context): TaskHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskHistoryDatabase::class.java,
                    "task_history_room_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
