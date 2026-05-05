package com.example.snutiexp.model

// 아티클 생성 시 request 파라미터에 들어갈 데이터 클래스
data class ArticleCreateRequest(
    val type: String,          // "TEXT" 또는 "IMAGE"
    val textContent: String?,  // 텍스트 내용
    val orderIndex: Int        // 명세서에 있는 순서 번호
)