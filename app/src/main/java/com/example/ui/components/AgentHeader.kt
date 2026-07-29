package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncHealthStatus
import com.example.ui.theme.*

@Composable
fun AgentHeader(
    syncHealth: SyncHealthStatus,
    isPanicActive: Boolean = false,
    onTogglePanic: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onOpenVoiceInput: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sidebar Hamburger Button
                    IconButton(
                        onClick = onOpenSidebar,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("open_sidebar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Task History Sidebar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SproutPrimary, SproutPrimaryBright)
                                )
                            )
                            .border(1.5.dp, SproutBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Agent Logo",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(pulseScale)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "PARADIM AGENT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (syncHealth.isOnline) SproutEmerald else SproutRose)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (syncHealth.isOnline) "Autonomous AI Active" else "Offline Queue Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Command Launcher Button
                    IconButton(
                        onClick = onOpenVoiceInput,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SproutPrimaryBright.copy(alpha = 0.2f))
                            .testTag("header_voice_cmd_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Command Control",
                            tint = SproutPrimaryBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Emergency Panic Kill Switch Button
                    IconButton(
                        onClick = onTogglePanic,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isPanicActive) SproutRose else SproutRose.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isPanicActive) Icons.Default.GppBad else Icons.Default.Block,
                            contentDescription = "Panic Kill Switch",
                            tint = if (isPanicActive) Color.White else SproutRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Resilient Sync Badge Button
                    Surface(
                        onClick = onSyncClick,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (syncHealth.pendingQueueCount > 0) SproutAmber else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (syncHealth.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Sync Status",
                                tint = if (syncHealth.pendingQueueCount > 0) SproutAmber else SproutEmerald,
                                modifier = Modifier.size(16.dp)
                            )

                            if (syncHealth.pendingQueueCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${syncHealth.pendingQueueCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SproutAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            if (isPanicActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SproutRose.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SproutRose),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.GppBad, contentDescription = null, tint = SproutRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚨 EMERGENCY KILL SWITCH ACTIVE: AGENT BLOCKED",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SproutRose,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (syncHealth.activeEdgeCaseScenario != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SproutAmber.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SproutAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = SproutAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scenario Active: ${syncHealth.activeEdgeCaseScenario}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SproutAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
