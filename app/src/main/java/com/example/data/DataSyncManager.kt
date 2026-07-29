package com.example.data

import android.content.Context
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.pow
import kotlin.random.Random

data class SyncHealthStatus(
    val isOnline: Boolean = true,
    val pendingQueueCount: Int = 0,
    val totalSyncedMacros: Int = 0,
    val totalAppsRegistered: Int = 0,
    val resolvedConflictsCount: Int = 0,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isSyncing: Boolean = false,
    val activeEdgeCaseScenario: String? = null
)

class DataSyncManager(private val db: AppDatabase) {

    private val _syncHealth = MutableStateFlow(SyncHealthStatus())
    val syncHealth: StateFlow<SyncHealthStatus> = _syncHealth.asStateFlow()

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val appCount = db.appRegistryDao().getAppByPackage("com.whatsapp")
        if (appCount == null) {
            // Seed default app registry
            val initialApps = listOf(
                AppRegistryEntity(
                    packageName = "com.whatsapp",
                    displayName = "WhatsApp",
                    launchIntent = "android.intent.action.VIEW",
                    deeplinkSchemes = "whatsapp://send?phone=%s&text=%s",
                    usageCount = 42,
                    syncState = SyncState.SYNCED
                ),
                AppRegistryEntity(
                    packageName = "com.google.android.apps.messaging",
                    displayName = "Messages",
                    launchIntent = "android.intent.action.SENDTO",
                    deeplinkSchemes = "smsto:%s",
                    usageCount = 28,
                    syncState = SyncState.SYNCED
                ),
                AppRegistryEntity(
                    packageName = "com.android.dialer",
                    displayName = "Phone Dialer",
                    launchIntent = "android.intent.action.CALL",
                    deeplinkSchemes = "tel:%s",
                    usageCount = 35,
                    syncState = SyncState.SYNCED
                ),
                AppRegistryEntity(
                    packageName = "com.android.settings",
                    displayName = "Settings",
                    launchIntent = "android.settings.SETTINGS",
                    deeplinkSchemes = "android.settings.DISPLAY_SETTINGS",
                    usageCount = 15,
                    syncState = SyncState.SYNCED
                ),
                AppRegistryEntity(
                    packageName = "org.telegram.messenger",
                    displayName = "Telegram",
                    launchIntent = "android.intent.action.VIEW",
                    deeplinkSchemes = "tg://msg?text=%s",
                    usageCount = 19,
                    syncState = SyncState.SYNCED
                )
            )
            db.appRegistryDao().insertAll(initialApps)

            // Seed initial macros
            val initialMacros = listOf(
                MacroEntity(
                    id = "macro_wa_mom",
                    name = "Quick WhatsApp Mom",
                    triggerPhrase = "whatsapp mom I am on my way",
                    targetPackage = "com.whatsapp",
                    stepsJson = """[{"type":"DEEPLINK","action":"whatsapp://send?phone=+15550199&text=I%20am%20on%20my%20way"},{"type":"ACCESSIBILITY_TAP","targetId":"com.whatsapp:id/send_btn"}]""",
                    uiTreeHash = "a3f89e21b7c01289",
                    successCount = 14,
                    version = 1,
                    syncState = SyncState.SYNCED
                ),
                MacroEntity(
                    id = "macro_toggle_dark_mode",
                    name = "Toggle Display Dark Mode",
                    triggerPhrase = "turn on dark mode",
                    targetPackage = "com.android.settings",
                    stepsJson = """[{"type":"LAUNCH","action":"android.settings.DISPLAY_SETTINGS"},{"type":"ACCESSIBILITY_TOGGLE","targetId":"com.android.settings:id/dark_mode_switch"}]""",
                    uiTreeHash = "88c7d12fef9011a0",
                    successCount = 8,
                    version = 2,
                    syncState = SyncState.SYNCED
                )
            )
            initialMacros.forEach { db.macroDao().insertOrUpdate(it) }

            // Seed initial audit log
            db.syncAuditLogDao().insert(
                SyncAuditLogEntity(
                    eventType = "INITIAL_DATABASE_BOOTSTRAP",
                    details = "Paradim Agent database initialized with default app registry & initial macros",
                    severity = AuditSeverity.SUCCESS
                )
            )
        }
        updateHealthMetrics()
    }

    suspend fun updateHealthMetrics() = withContext(Dispatchers.IO) {
        val pendingMacros = db.macroDao().getPendingSyncMacros().size
        _syncHealth.value = _syncHealth.value.copy(
            pendingQueueCount = pendingMacros,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Edge Case 1: Exponential Backoff & Jitter Sync with Network Retry
     */
    suspend fun syncWithExponentialBackoff(
        maxRetries: Int = 3,
        baseDelayMs: Long = 500
    ): Boolean = withContext(Dispatchers.IO) {
        _syncHealth.value = _syncHealth.value.copy(isSyncing = true)
        var attempt = 0
        var success = false

        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "SYNC_STARTED",
                details = "Initiating edge-case resilient sync cycle with exponential backoff & jitter",
                severity = AuditSeverity.INFO
            )
        )

        while (attempt < maxRetries && !success) {
            attempt++
            try {
                // Check simulate network condition
                if (!_syncHealth.value.isOnline) {
                    throw IllegalStateException("Network unreachable during sync attempt $attempt")
                }

                // Simulate processing pending sync items
                delay(300)

                // Push pending macros to synced state
                val pending = db.macroDao().getPendingSyncMacros()
                for (macro in pending) {
                    db.macroDao().insertOrUpdate(macro.copy(syncState = SyncState.SYNCED))
                }

                success = true
                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "SYNC_SUCCESS",
                        details = "Edge-case sync completed successfully on attempt $attempt. ${pending.size} pending items synchronized.",
                        severity = AuditSeverity.SUCCESS
                    )
                )
            } catch (e: Exception) {
                val backoff = (baseDelayMs * 2.0.pow(attempt - 1)).toLong() + Random.nextLong(100, 300)
                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "SYNC_RETRY_BACKOFF",
                        details = "Attempt $attempt failed: ${e.message}. Retrying in ${backoff}ms...",
                        severity = AuditSeverity.WARNING,
                        resolved = false
                    )
                )
                if (attempt < maxRetries) {
                    delay(backoff)
                }
            }
        }

        _syncHealth.value = _syncHealth.value.copy(isSyncing = false)
        updateHealthMetrics()
        return@withContext success
    }

    /**
     * Edge Case 2: UI Tree Hash Drift & Auto-Healing Logic
     * When an app updates its layout, the recorded UI tree hash fails matching.
     * The agent detects hash drift, uses AI re-planning to patch the step, and syncs the updated macro.
     */
    suspend fun resolveUiHashDrift(macroId: String, currentObservedHash: String): MacroEntity? = withContext(Dispatchers.IO) {
        val macro = db.macroDao().getMacroById(macroId) ?: return@withContext null

        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "UI_HASH_DRIFT_DETECTED",
                details = "Target package '${macro.targetPackage}' UI tree hash changed from '${macro.uiTreeHash}' to '$currentObservedHash'. Initiating auto-heal sequence.",
                severity = AuditSeverity.WARNING,
                affectedEntity = macroId
            )
        )

        // Simulate AI step re-alignment & auto-healing
        delay(600)

        val healedMacro = macro.copy(
            uiTreeHash = currentObservedHash,
            version = macro.version + 1,
            syncState = SyncState.PENDING_SYNC,
            lastUsedTs = System.currentTimeMillis()
        )

        db.macroDao().insertOrUpdate(healedMacro)

        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "UI_HASH_DRIFT_HEALED",
                details = "Successfully auto-healed macro '${macro.name}' to version ${healedMacro.version} with updated UI hash '$currentObservedHash'.",
                severity = AuditSeverity.SUCCESS,
                affectedEntity = macroId
            )
        )

        updateHealthMetrics()
        return@withContext healedMacro
    }

    /**
     * Edge Case 3: Merge Conflict Resolution (Local vs Remote Macro Update)
     * When local edits collide with remote synced versions, apply deterministic vector timestamp & audit resolution.
     */
    suspend fun resolveMergeConflict(
        localMacro: MacroEntity,
        remoteVersion: Int,
        remoteStepsJson: String
    ): MacroEntity = withContext(Dispatchers.IO) {
        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "MERGE_CONFLICT_DETECTED",
                details = "Macro '${localMacro.name}' local version v${localMacro.version} conflicts with remote version v$remoteVersion.",
                severity = AuditSeverity.WARNING,
                resolved = false,
                affectedEntity = localMacro.id
            )
        )

        val mergedMacro = if (localMacro.version >= remoteVersion) {
            // Local wins or vector merge
            localMacro.copy(
                version = localMacro.version + 1,
                syncState = SyncState.SYNCED
            )
        } else {
            // Remote wins
            localMacro.copy(
                stepsJson = remoteStepsJson,
                version = remoteVersion,
                syncState = SyncState.SYNCED
            )
        }

        db.macroDao().insertOrUpdate(mergedMacro)

        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "MERGE_CONFLICT_RESOLVED",
                details = "Conflict resolved for '${localMacro.name}' via version-vector priority. Final version: v${mergedMacro.version}.",
                severity = AuditSeverity.SUCCESS,
                resolved = true,
                affectedEntity = localMacro.id
            )
        )

        _syncHealth.value = _syncHealth.value.copy(
            resolvedConflictsCount = _syncHealth.value.resolvedConflictsCount + 1
        )
        updateHealthMetrics()
        return@withContext mergedMacro
    }

    /**
     * Edge Case 4: Simulate Edge Case Scenarios for User Testing & Diagnostics
     */
    suspend fun triggerSimulatedScenario(scenarioType: String) = withContext(Dispatchers.IO) {
        _syncHealth.value = _syncHealth.value.copy(activeEdgeCaseScenario = scenarioType)

        when (scenarioType) {
            "NETWORK_DROP" -> {
                _syncHealth.value = _syncHealth.value.copy(isOnline = false)
                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "SIMULATED_NETWORK_DROP",
                        details = "Simulating mid-transaction network disconnect. Queueing pending operations locally.",
                        severity = AuditSeverity.WARNING
                    )
                )
            }
            "NETWORK_RESTORE" -> {
                _syncHealth.value = _syncHealth.value.copy(isOnline = true)
                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "SIMULATED_NETWORK_RESTORE",
                        details = "Network connectivity restored. Auto-draining offline sync queue.",
                        severity = AuditSeverity.INFO
                    )
                )
                syncWithExponentialBackoff()
            }
            "INDUCE_HASH_DRIFT" -> {
                val macros = db.macroDao().getPendingSyncMacros().ifEmpty {
                    // Grab any macro
                    listOf(
                        MacroEntity(
                            id = "macro_wa_mom",
                            name = "Quick WhatsApp Mom",
                            triggerPhrase = "whatsapp mom I am on my way",
                            targetPackage = "com.whatsapp",
                            stepsJson = "[]",
                            uiTreeHash = "stale_hash_999"
                        )
                    )
                }
                val target = macros.first()
                val newHash = "drifted_hash_" + UUID.randomUUID().toString().take(8)
                resolveUiHashDrift(target.id, newHash)
            }
            "INDUCE_CONFLICT" -> {
                val target = MacroEntity(
                    id = "macro_toggle_dark_mode",
                    name = "Toggle Display Dark Mode",
                    triggerPhrase = "turn on dark mode",
                    targetPackage = "com.android.settings",
                    stepsJson = "[]",
                    uiTreeHash = "88c7d12fef9011a0",
                    version = 2
                )
                resolveMergeConflict(
                    localMacro = target,
                    remoteVersion = 3,
                    remoteStepsJson = """[{"type":"LAUNCH","action":"android.settings.DISPLAY_SETTINGS"},{"type":"ACCESSIBILITY_TOGGLE","targetId":"com.android.settings:id/dark_mode_switch_v2"}]"""
                )
            }
            "CORRUPT_PAYLOAD_RECOVERY" -> {
                db.syncAuditLogDao().insert(
                    SyncAuditLogEntity(
                        eventType = "CORRUPT_PAYLOAD_REJECTED",
                        details = "Received malformed JSON sync response from remote node. Transaction rolled back atomically.",
                        severity = AuditSeverity.ERROR,
                        resolved = true
                    )
                )
            }
        }

        delay(300)
        _syncHealth.value = _syncHealth.value.copy(activeEdgeCaseScenario = null)
        updateHealthMetrics()
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        db.syncAuditLogDao().clearLogs()
        db.syncAuditLogDao().insert(
            SyncAuditLogEntity(
                eventType = "AUDIT_LOG_CLEARED",
                details = "Audit logs purged by user request",
                severity = AuditSeverity.INFO
            )
        )
    }
}
