package com.example.myapplication.data_class

data class Alarm(
    val alarmId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val createdBy: String = "",
    val assignedTo: String = "",
    val active: Boolean = true
)









