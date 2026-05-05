package com.lauren.serveconnect.models

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",           // "seeker" or "provider"
    val profileImageUrl: String = "",
    val location: String = "",
    val createdAt: Long = 0L
)