package com.example.myapplication.data_class

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)