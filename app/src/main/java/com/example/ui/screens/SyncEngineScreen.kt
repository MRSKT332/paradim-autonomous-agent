package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncHealthStatus
import com.example.data.entity.AuditSeverity
import com.example.data.entity.SyncAuditLogEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncEngineScreen(
    syncHealth: SyncHealthStatus,
    auditLogs: List<SyncAuditLogEntity>,
    onTriggerScenario: (String) -> Unit,
    onForceSync: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Health Overview Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Data Sync & Edge-Case Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Resilient Offline Queueing & Conflict Resolver",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onForceSync,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !syncHealth.isSyncing,
                            modifier = Modifier.testTag("force_sync_btn")
                        ) {
                            if (syncHealth.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Stat Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SyncMetricTile(
                            title = "Network",
                            value = if (syncHealth.isOnline) "ONLINE" else "OFFLINE",
                            color = if (syncHealth.isOnline) EmeraldSuccess else RoseError,
                            icon = if (syncHealth.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            modifier = Modifier.weight(1f)
                        )
                        SyncMetricTile(
                            title = "Pending Queue",
                            value = "${syncHealth.pendingQueueCount} items",
                            color = if (syncHealth.pendingQueueCount > 0) AmberWarning else CyanBright,
                            icon = Icons.Default.Queue,
                            modifier = Modifier.weight(1f)
                        )
                        SyncMetricTile(
                            title = "Conflicts Resolved",
                            value = "${syncHealth.resolvedConflictsCount}",
                            color = EmeraldSuccess,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Edge Case Simulator Panel
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edge-Case Diagnostic & Simulation Suite",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Interactive",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Trigger edge cases to verify offline backoff, hash drift auto-healing, version vector conflict resolution, and atomic database rollbacks:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Scenario Buttons Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (syncHealth.isOnline) onTriggerScenario("NETWORK_DROP")
                                    else onTriggerScenario("NETWORK_RESTORE")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (syncHealth.isOnline) RoseError else EmeraldSuccess
                                ),
                                border = BorderStroke(1.dp, if (syncHealth.isOnline) RoseError else EmeraldSuccess),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_network_btn")
                            ) {
                                Icon(
                                    imageVector = if (syncHealth.isOnline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (syncHealth.isOnline) "Simulate Net Loss" else "Restore Network",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onTriggerScenario("INDUCE_HASH_DRIFT") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                                border = BorderStroke(1.dp, AmberWarning),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("induce_hash_drift_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trigger Hash Drift",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onTriggerScenario("INDUCE_CONFLICT") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                                border = BorderStroke(1.dp, CyanBright),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("induce_conflict_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Simulate Conflict",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onTriggerScenario("CORRUPT_PAYLOAD_RECOVERY") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = BorderStroke(1.dp, CyberBorderDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("corrupt_payload_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestorePage,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test Rollback",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Audit Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sync Audit & Transaction Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                TextButton(onClick = onClearLogs) {
                    Text("Clear Logs", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Audit Log Items
        items(auditLogs, key = { it.id }) { log ->
            AuditLogCard(log = log)
        }
    }
}

@Composable
fun SyncMetricTile(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberSurfaceVariantDark,
        border = BorderStroke(1.dp, CyberBorderDark),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
        }
    }
}

@Composable
fun AuditLogCard(log: SyncAuditLogEntity) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val (badgeBg, badgeText, label) = when (log.severity) {
        AuditSeverity.INFO -> Triple(CyanPrimary.copy(alpha = 0.15f), CyanBright, "INFO")
        AuditSeverity.WARNING -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, "WARN")
        AuditSeverity.ERROR -> Triple(RoseError.copy(alpha = 0.15f), RoseError, "ERROR")
        AuditSeverity.SUCCESS -> Triple(EmeraldSuccess.copy(alpha = 0.15f), EmeraldSuccess, "SUCCESS")
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = BorderStroke(1.dp, CyberBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.eventType,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (log.affectedEntity != null) {
                Text(
                    text = "Affected Entity ID: ${log.affectedEntity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanGlow,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
