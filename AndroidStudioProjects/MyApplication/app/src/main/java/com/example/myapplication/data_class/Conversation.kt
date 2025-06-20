package com.example.myapplication.data_class

data class Conversation(
    val participants: Map<String, Boolean> = emptyMap(), // id-uri participanți
    val lastMessage: Message? = null,
    val name: String? = null
)
