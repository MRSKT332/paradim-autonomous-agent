package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credential_vault")
data class CredentialVaultEntity(
    @PrimaryKey val id: String,
    val serviceName: String,
    val username: String?,
    val encryptedSecretRef: String, // Keystore alias reference
    val biometricRequired: Boolean = true,
    val lastUsedTs: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.SYNCED,
    val notes: String? = null
)
