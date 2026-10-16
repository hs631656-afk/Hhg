package com.example.model

import java.util.UUID

enum class ExecutionStatus {
    SUCCESS,
    FAILED,
    REQUIRES_CONFIRMATION,
    CANCELLED,
    ACCESSIBILITY_NOT_ENABLED
}

data class ActionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userPrompt: String,
    val language: String = "Hinglish",
    val actionType: String,
    val actionDetails: String,
    val executionStatus: ExecutionStatus,
    val feedbackMessage: String
)
