package com.example.myapplication.data_class

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val friends: Map<String, Boolean> = emptyMap(),
)
