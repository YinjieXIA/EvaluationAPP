package com.example.EvaluationStu.models

data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val sender: String = "",
    val componentId: String? = null
)

