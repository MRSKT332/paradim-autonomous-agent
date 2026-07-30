package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        AppRegistryEntity::class,
        MacroEntity::class,
        CredentialVaultEntity::class,
        SyncAuditLogEntity::class,
        TaskExecutionEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRegistryDao(): AppRegistryDao
    abstract fun macroDao(): MacroDao
    abstract fun credentialVaultDao(): CredentialVaultDao
    abstract fun syncAuditLogDao(): SyncAuditLogDao
    abstract fun taskExecutionDao(): TaskExecutionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paradim_agent_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
