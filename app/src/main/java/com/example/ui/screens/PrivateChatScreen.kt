package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.MessageSender
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrivateChatScreen(
    chatMessages: List<ChatMessageEntity>,
    chatInput: String,
    isChatLoading: Boolean,
    onChatInputChanged: (String) -> Unit,
    onSendChatMessage: () -> Unit,
    onDelegateToAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Who can I call in my contacts?",
        "Draft a message on WhatsApp",
        "How do I toggle dark mode?",
        "Check system data sync health",
        "Summarize agent capabilities"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = CyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Private Conversational AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Direct LLM & Assistant Workspace",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onClearChat, modifier = Modifier.testTag("clear_chat_btn")) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = TextMuted
                )
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    onClick = {
                        onChatInputChanged(prompt)
                        onSendChatMessage()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = CyberSurfaceDark,
                    border = BorderStroke(1.dp, CyberBorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onDelegateToAgent = { onDelegateToAgent(msg.text) }
                )
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyanBright
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Private AI is thinking...",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanGlow
                        )
                    }
                }
            }
        }

        // Chat Input Box
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CyberSurfaceDark,
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = onChatInputChanged,
                    placeholder = { Text("Type a message to chat...", color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("private_chat_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = onSendChatMessage,
                    enabled = chatInput.isNotBlank() && !isChatLoading,
                    modifier = Modifier.testTag("send_chat_msg_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (chatInput.isNotBlank()) CyanBright else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onDelegateToAgent: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val isTelegram = message.sender == MessageSender.TELEGRAM_BOT
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        isUser -> CyanPrimary.copy(alpha = 0.25f)
        isTelegram -> AmberWarning.copy(alpha = 0.2f)
        else -> CyberSurfaceVariantDark
    }
    val borderColor = when {
        isUser -> CyanPrimary.copy(alpha = 0.5f)
        isTelegram -> AmberWarning.copy(alpha = 0.5f)
        else -> CyberBorderDark
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                Icon(
                    imageVector = if (isTelegram) Icons.Default.SendToMobile else Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = if (isTelegram) AmberWarning else CyanBright,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = bubbleColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(message.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        if (!isUser && !message.isAgentTaskTrigger) {
                            Surface(
                                onClick = onDelegateToAgent,
                                shape = RoundedCornerShape(6.dp),
                                color = CyanPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = CyanBright,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Run as Agent",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyanBright,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
