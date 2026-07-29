package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuditSeverity {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

@Entity(tableName = "sync_audit_logs")
data class SyncAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // e.g., OFFLINE_QUEUE, MERGE_CONFLICT, HASH_DRIFT_HEALED, TRANSACTION_ROLLBACK
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: AuditSeverity = AuditSeverity.INFO,
    val resolved: Boolean = true,
    val affectedEntity: String? = null
)
