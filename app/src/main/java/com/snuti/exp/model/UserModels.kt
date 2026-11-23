package com.snuti.exp.model

// 사용자의 관심 태그를 일괄 수정/등록할 때 사용하는 요청 모델
data class UserInterestUpdateRequest(
    val tagIds: List<Long>
)