package com.example.data.dao

import androidx.room.*
import com.example.data.entity.AppRegistryEntity
import com.example.data.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRegistryDao {
    @Query("SELECT * FROM app_registry ORDER BY usageCount DESC, displayName ASC")
    fun getAllApps(): Flow<List<AppRegistryEntity>>

    @Query("SELECT * FROM app_registry WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): AppRegistryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: AppRegistryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppRegistryEntity>)

    @Query("UPDATE app_registry SET syncState = :state, lastSyncTs = :syncTs WHERE packageName = :packageName")
    suspend fun updateSyncState(packageName: String, state: SyncState, syncTs: Long)

    @Query("DELETE FROM app_registry WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM app_registry")
    suspend fun clearAll()
}
