package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MessageSender {
    USER,
    AI_CHAT,
    TELEGRAM_BOT
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAgentTaskTrigger: Boolean = false,
    val linkedTaskId: String? = null
)
