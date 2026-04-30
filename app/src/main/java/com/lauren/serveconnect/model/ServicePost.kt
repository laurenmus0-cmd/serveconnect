package com.lauren.serveconnect.model

data class ServicePost(
    val id: String,
    val providerName: String,
    val title: String,
    val description: String,
    val price: String,
    val category: String,
    val phoneNumber: String
)
