package com.example.snutiexp.model

data class LectureCreateRequest(
    val title: String,
    val lectureDate: String,
    val location: String,
    val lectureSummary: String,
    val lecturerName: String,
    val topic: String,
    val status: String = "PUBLISHED"
)