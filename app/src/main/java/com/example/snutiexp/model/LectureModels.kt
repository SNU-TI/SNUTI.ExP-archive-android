package com.example.snutiexp.model

// 강연 생성 요청
data class LectureCreateRequest(
    val title: String,
    val lectureDate: String,
    val location: String?,
    val lectureSummary: String?,
    val lecturerName: String?,
    val topic: String?,
    val status: String,
    val tags: List<String>
)
data class LectureUpdateRequest(
    val title: String?,
    val lectureDate: String?,
    val location: String?,
    val lectureSummary: String?,
    val lecturerName: String?,
    val topic: String?,
    val status: String?,
    val tags: List<String>?
)

// 강연 생성 응답
data class LectureCreateResponse(
    val id: Long,
    val title: String,
    val lectureDate: String,
    val location: String?,
    val lectureSummary: String?,
    val lecturerName: String?,
    val topic: String?,
    val status: String
)

// 페이징 응답 (목록 조회 및 드래프트 조회 공통 사용)
data class LectureListResponse(
    val content: List<LectureListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

// 리스트 아이템
data class LectureListItemResponse(
    val id: Long,
    val title: String,
    val lectureDate: String,
    val location: String?,
    val lecturerName: String?,
    val topic: String?,
    val lectureSummary: String?,
    @com.google.gson.annotations.SerializedName("tags")
    val tags: List<TagResponse>?
)

// 상세 조회 모델
data class LectureDetailResponse(
    val id: Long,
    val title: String,
    val lectureDate: String,
    val location: String?,
    val lectureSummary: String?,
    val lecturerName: String?,
    val topic: String?,
    val status: String,
    val articles: List<ArticleResponse>,
    val videos: List<VideoResponse>,
    val tags: List<TagResponse>
)