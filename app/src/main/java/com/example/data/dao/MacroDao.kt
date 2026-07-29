package com.example.data.dao

import androidx.room.*
import com.example.data.entity.MacroEntity
import com.example.data.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY lastUsedTs DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getMacroById(id: String): MacroEntity?

    @Query("SELECT * FROM macros WHERE LOWER(triggerPhrase) = LOWER(:phrase) OR triggerPhrase LIKE '%' || :phrase || '%' LIMIT 1")
    suspend fun findMatchingMacro(phrase: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(macro: MacroEntity)

    @Query("UPDATE macros SET successCount = successCount + 1, lastUsedTs = :ts WHERE id = :id")
    suspend fun incrementSuccess(id: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE macros SET failureCount = failureCount + 1, lastUsedTs = :ts WHERE id = :id")
    suspend fun incrementFailure(id: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE macros SET uiTreeHash = :newHash, version = version + 1, syncState = :syncState WHERE id = :id")
    suspend fun updateUiHash(id: String, newHash: String, syncState: SyncState)

    @Delete
    suspend fun delete(macro: MacroEntity)

    @Query("SELECT * FROM macros WHERE syncState = 'PENDING_SYNC' OR syncState = 'DIRTY'")
    suspend fun getPendingSyncMacros(): List<MacroEntity>
}
