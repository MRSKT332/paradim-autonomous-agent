package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MacroEntity
import com.example.data.entity.SyncState
import com.example.ui.theme.*

@Composable
fun MacroStoreScreen(
    macros: List<MacroEntity>,
    onRunMacro: (String) -> Unit,
    onHealHashDrift: (String) -> Unit,
    onDeleteMacro: (MacroEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Macro Repository",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Recorded Sub-200ms Replay Sequences",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyanPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${macros.size} Recorded",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanGlow,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (macros.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                    border = BorderStroke(1.dp, CyberBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Recorded Macros Yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Execute tasks in the Command Hub to automatically generate and save high-speed replay macros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(macros, key = { it.id }) { macro ->
                MacroCard(
                    macro = macro,
                    onRunMacro = { onRunMacro(macro.triggerPhrase) },
                    onHealHash = { onHealHashDrift(macro.id) },
                    onDelete = { onDeleteMacro(macro) }
                )
            }
        }
    }
}

@Composable
fun MacroCard(
    macro: MacroEntity,
    onRunMacro: () -> Unit,
    onHealHash: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = BorderStroke(1.dp, CyberBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = macro.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Trigger: \"${macro.triggerPhrase}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanGlow
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (macro.syncState == SyncState.SYNCED) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (macro.syncState == SyncState.SYNCED) EmeraldSuccess.copy(alpha = 0.3f) else AmberWarning.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (macro.syncState == SyncState.SYNCED) "v${macro.version} SYNCED" else "v${macro.version} PENDING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (macro.syncState == SyncState.SYNCED) EmeraldSuccess else AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Stats & Hash Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "UI Hash: ${macro.uiTreeHash.take(10)}...",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Runs: ${macro.successCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRunMacro, modifier = Modifier.size(32.dp).testTag("run_macro_${macro.id}")) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = CyanBright)
                    }
                    IconButton(onClick = onHealHash, modifier = Modifier.size(32.dp).testTag("heal_macro_${macro.id}")) {
                        Icon(imageVector = Icons.Default.Healing, contentDescription = "Auto Heal", tint = AmberWarning)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_macro_${macro.id}")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                    }
                }
            }

            // Expandable Step Inspector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Macro Payload" else "Inspect Replay Steps",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = macro.stepsJson,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
