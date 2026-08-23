package com.example.snutiexp.main

data class EditSection(
    val id: Int,
    var type: String, // "TEXT" 또는 "IMAGE"
    var content: String = "",
    var imageUri: String? = null,

    var articleId: Long? = null, // 서버에서 받아온 진짜 아티클 고유 ID (신규 생성 시 null)
    var existingBlockId: Long? = null, // 버에서 받아온 개별 블록의 고유 ID (기존 블록 수정용)
    var isNew: Boolean = true    // 새로 추가한 섹션인지, 서버에서 불러온 기존 섹션인지 구분
)