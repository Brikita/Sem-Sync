package com.example.semsync

import com.google.firebase.Timestamp

data class AcademicGroup(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val joinCode: String = "",
    val lecturerName: String = "",
    val repId: String = "",
    val memberCount: Int = 0,
    val createdAt: Long? = null
)

data class GroupPost(
    val id: String = "",
    val groupId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Long? = null,
    val type: String = "announcement", // "announcement", "material", "urgent"
    val attachments: List<String> = emptyList()
)

// NOTE: The Task data class has been moved to its own file, Task.kt, to resolve a redeclaration error.

// Added based on UI requirements for the dashboard schedule card
data class TimetableEntry(
    val unitName: String = "",
    val unitCode: String = "",
    val location: String = "",
    val startTime: Long? = null,
    val endTime: Long? = null,
    val dayOfWeek: String = ""
)
