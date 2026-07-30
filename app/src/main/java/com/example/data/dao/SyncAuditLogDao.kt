package com.example.data.dao

import androidx.room.*
import com.example.data.entity.SyncAuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncAuditLogDao {
    @Query("SELECT * FROM sync_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<SyncAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncAuditLogEntity)

    @Query("DELETE FROM sync_audit_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM sync_audit_logs WHERE resolved = 0")
    fun getUnresolvedCount(): Flow<Int>
}
