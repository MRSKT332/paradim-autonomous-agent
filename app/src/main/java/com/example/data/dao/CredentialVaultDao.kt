package com.example.data.dao

import androidx.room.*
import com.example.data.entity.CredentialVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialVaultDao {
    @Query("SELECT * FROM credential_vault ORDER BY serviceName ASC")
    fun getAllCredentials(): Flow<List<CredentialVaultEntity>>

    @Query("SELECT * FROM credential_vault WHERE id = :id")
    suspend fun getById(id: String): CredentialVaultEntity?

    @Query("SELECT * FROM credential_vault WHERE LOWER(serviceName) LIKE '%' || LOWER(:service) || '%' LIMIT 1")
    suspend fun findByService(service: String): CredentialVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(credential: CredentialVaultEntity)

    @Delete
    suspend fun delete(credential: CredentialVaultEntity)
}
