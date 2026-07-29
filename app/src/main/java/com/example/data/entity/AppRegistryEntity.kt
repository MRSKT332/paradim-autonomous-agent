package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncState {
    SYNCED,
    PENDING_SYNC,
    STALE,
    CONFLICT,
    DIRTY
}

@Entity(tableName = "app_registry")
data class AppRegistryEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val launchIntent: String,
    val deeplinkSchemes: String, // Comma separated or JSON list
    val lastUsedTs: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val isExcludedFromSlowPath: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncTs: Long = System.currentTimeMillis()
)
