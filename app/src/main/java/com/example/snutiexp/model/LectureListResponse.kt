package com.example.snutiexp.model

// 서버의 전체 페이징 응답 객체
data class LectureListResponse(
    val content: List<LectureListItem>, // 실제 강의 데이터 리스트
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

// 목록에 표시될 개별 강의 아이템
data class LectureListItem(
    val id: Long,
    val title: String,
    val lectureDate: String,
    val location: String,
    val lecturerName: String,
    val topic: String,
    val lectureSummary: String
)