package com.example.snutiexp.model

data class CreateVideoRequest(
    val videoUrl: String,
    val caption: String? = null
)

data class UpdateVideoRequest(
    val videoUrl: String,
    val caption: String? = null
)

data class VideoResponse(
    val id: Long,
    val lectureId: Long,
    val videoUrl: String,
    val caption: String?,
    val createdAt: String
)