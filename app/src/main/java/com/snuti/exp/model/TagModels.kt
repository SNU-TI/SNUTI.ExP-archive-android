package com.snuti.exp.model

data class TagCreateRequest(
    val name: String
)

data class TagResponse(
    val id: Long,
    val name: String
)