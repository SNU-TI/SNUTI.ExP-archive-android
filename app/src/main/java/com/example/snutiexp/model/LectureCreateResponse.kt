package com.example.snutiexp.model

// 강연 생성 성공 시 서버로부터 받는 응답 데이터 모델입니다.
data class LectureCreateResponse(
    val id: Long,               // 생성된 강연의 고유 ID (이후 아티클 추가 시 사용)
    val title: String,          // 강연 제목
    val lectureDate: String,    // 강연 날짜
    val location: String,       // 강연 장소
    val lectureSummary: String, // 강연 요약
    val lecturerName: String,   // 강연자 이름
    val topic: String,          // 강연 주제
    val status: String          // 강연 상태 (예: DRAFT)
)