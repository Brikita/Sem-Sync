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
    val createdAt: Timestamp? = null
)

data class GroupPost(
    val id: String = "",
    val groupId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
    val type: String = "announcement", // "announcement", "material", "urgent"
    val attachments: List<String> = emptyList()
)
