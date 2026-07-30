package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityPolicyManager
import com.example.ui.theme.*

data class PermissionItemState(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val badgeText: String,
    val onActionClick: (Context) -> Unit
)

@Composable
fun PermissionsCenterScreen(
    isAccessibilityActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blacklistedKeywords by SecurityPolicyManager.blacklistedKeywords.collectAsState()
    var newKeywordInput by remember { mutableStateOf("") }

    // Check system overlay permission
    val canDrawOverlays = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    val permissionsList = listOf(
        PermissionItemState(
            title = "Accessibility Service",
            description = "Allows Autonomous Agent to click buttons, scroll views, and automate UI macros",
            icon = Icons.Default.AccessibilityNew,
            isGranted = isAccessibilityActive,
            badgeText = if (isAccessibilityActive) "SERVICE ACTIVE" else "PERMISSION REQUIRED",
            onActionClick = { ctx ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(intent)
            }
        ),
        PermissionItemState(
            title = "Display Over Other Apps (Overlay)",
            description = "Enables floating agent action controls and live macro status over other applications",
            icon = Icons.Default.Layers,
            isGranted = canDrawOverlays,
            badgeText = if (canDrawOverlays) "GRANTED" else "PERMISSION REQUIRED",
            onActionClick = { ctx ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    ctx.startActivity(intent)
                }
            }
        ),
        PermissionItemState(
            title = "Phone Calls & Dialer",
            description = "Allows agent to initiate direct calls and trigger call dialing workflows",
            icon = Icons.Default.Phone,
            isGranted = true, // Granted in Manifest / Runtime prompt
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "SMS & Messaging",
            description = "Allows agent to compose and send direct SMS messages to contacts",
            icon = Icons.Default.Sms,
            isGranted = true,
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "Contacts Reader",
            description = "Allows agent to match names with phone numbers and draft communication",
            icon = Icons.Default.Contacts,
            isGranted = true,
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "GPS & Anti-Theft Location Tracker",
            description = "Allows agent to locate lost phone, reverse geocode street address, and send Google Maps coordinates via Telegram",
            icon = Icons.Default.MyLocation,
            isGranted = true,
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "Camera & Anti-Theft Selfie Capture",
            description = "Allows agent to capture anti-theft photos of intruders when lost phone command is issued",
            icon = Icons.Default.CameraAlt,
            isGranted = true,
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "Flashlight & Torch Control",
            description = "Allows agent to activate device torch mode remotely or upon request",
            icon = Icons.Default.FlashOn,
            isGranted = true,
            badgeText = "HARDWARE READY",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "Biometric Vault & Hardware Security",
            description = "Unlocks sensitive credential vault using Android Fingerprint / Face Unlock",
            icon = Icons.Default.Fingerprint,
            isGranted = true,
            badgeText = "HARDWARE READY",
            onActionClick = { ctx -> openAppSettings(ctx) }
        ),
        PermissionItemState(
            title = "Post System Notifications",
            description = "Displays foreground service notifications and status updates for tasks",
            icon = Icons.Default.NotificationsActive,
            isGranted = true,
            badgeText = "MANIFEST DECLARED",
            onActionClick = { ctx -> openAppSettings(ctx) }
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Permissions",
                        tint = CyanBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Agentic Permissions & Access Center",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Manage system access for autonomous device work",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(permissionsList.size) { index ->
            val perm = permissionsList[index]
            PermissionCardItem(perm = perm, context = context)
        }

        // Security Command Policy & Blacklist Sandbox Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoseError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Command Sandbox & Blacklist Policy",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Prevents dangerous agent commands from executing",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Active Blocked Keywords / Phrases:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blacklistedKeywords.forEach { kw ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoseError.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = kw,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoseError
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = RoseError,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                SecurityPolicyManager.removeBlacklistedKeyword(context, kw)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newKeywordInput,
                            onValueChange = { newKeywordInput = it },
                            placeholder = { Text("e.g. format sdcard") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_blacklist_kw_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoseError,
                                unfocusedBorderColor = CyberBorderDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newKeywordInput.isNotBlank()) {
                                    SecurityPolicyManager.addBlacklistedKeyword(context, newKeywordInput)
                                    newKeywordInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Block")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCardItem(perm: PermissionItemState, context: Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = BorderStroke(1.dp, if (perm.isGranted) EmeraldSuccess.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (perm.isGranted) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = perm.icon,
                    contentDescription = null,
                    tint = if (perm.isGranted) EmeraldSuccess else AmberWarning,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = perm.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (perm.isGranted) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = perm.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (perm.isGranted) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { perm.onActionClick(context) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, if (perm.isGranted) CyberBorderDark else AmberWarning),
                        modifier = Modifier.testTag("perm_action_${perm.title.replace(" ", "_").lowercase()}")
                    ) {
                        Text(
                            text = if (perm.isGranted) "Settings" else "Grant Access",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (perm.isGranted) TextSecondary else AmberWarning
                        )
                    }
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
