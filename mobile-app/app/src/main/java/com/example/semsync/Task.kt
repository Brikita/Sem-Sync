package com.example.semsync

import com.google.firebase.Timestamp

data class Task(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val courseCode: String = "",
    val completed: Boolean = false,
    val priority: String = "medium",
    val dueDate: Long? = null,
    val taskType: String = "personal", // "personal" or "academic"
    val createdAt: Long? = null,
    val status: String = "pending"
)