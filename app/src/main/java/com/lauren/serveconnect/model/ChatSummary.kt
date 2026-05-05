package com.lauren.serveconnect.model

data class ChatSummary(
    val otherUserId: String,
    val otherUserName: String,
    val lastMessage: String,
    val timestamp: Long
)
