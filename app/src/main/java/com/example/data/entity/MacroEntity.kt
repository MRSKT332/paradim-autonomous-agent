package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerPhrase: String,
    val targetPackage: String,
    val stepsJson: String, // Serialized list of MacroStep
    val uiTreeHash: String, // Stable SHA-256 of UI subtree
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedTs: Long = System.currentTimeMillis(),
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val isAutoHealEnabled: Boolean = true,
    val syncState: SyncState = SyncState.SYNCED,
    val version: Int = 1
)
