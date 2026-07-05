package com.example.snutiexp.model

import com.google.gson.annotations.SerializedName

/**
 * 강좌 상세 조회 응답을 담는 데이터 클래스
 */
data class LectureDetailResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("lectureDate")
    val lectureDate: String,

    @SerializedName("location")
    val location: String,

    @SerializedName("lectureSummary")
    val lectureSummary: String,

    @SerializedName("lecturerName")
    val lecturerName: String,

    @SerializedName("topic")
    val topic: String,

    @SerializedName("videoUrl")
    val videoUrl: String?,

    @SerializedName("status")
    val status: String,

    // 상세 화면 하단에 순서대로 표시될 섹션(아티클) 리스트
    @SerializedName("articles")
    val articles: List<ArticleResponse>
)

/**
 * 강좌 내 개별 섹션(텍스트 또는 이미지) 정보를 담는 데이터 클래스
 */
data class ArticleResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("content")
    val content: String?,    // 텍스트 내용 (이미지 섹션일 경우 null일 수 있음)

    @SerializedName("imageUrl")
    val imageUrl: String?,   // 이미지 경로 (텍스트 섹션일 경우 null일 수 있음)

    @SerializedName("sequence")
    val sequence: Int        // 섹션 표시 순서
)