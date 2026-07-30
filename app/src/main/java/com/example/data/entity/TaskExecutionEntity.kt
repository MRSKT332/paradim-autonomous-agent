package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskStatus {
    QUEUED,
    EXECUTING,
    COMPLETED,
    FAILED,
    AWAITING_BIOMETRIC
}

enum class ExecutionPathType {
    FAST_PATH,
    MACRO_REPLAY,
    SLOW_PATH_AI
}

@Entity(tableName = "task_executions")
data class TaskExecutionEntity(
    @PrimaryKey val id: String,
    val rawPrompt: String,
    val pathType: ExecutionPathType,
    val status: TaskStatus = TaskStatus.QUEUED,
    val confidence: Float = 1.0f,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 1,
    val logTrace: String, // Step by step reasoning log
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
