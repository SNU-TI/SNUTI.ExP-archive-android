package com.example.snutiexp.model

// 서버의 CreateArticleRequest 대응
data class CreateArticleRequest(
    val articleTitle: String,
    val author: String? = "Admin",
    val blocks: List<ArticleBlockRequest>
)

data class ArticleBlockRequest(
    val type: ArticleBlockType, // String 대신 Enum 사용
    val orderIndex: Int,
    val textContent: String? = null,
    val clientImageKey: String? = null
)

// 서버의 ArticleResponse 대응 (상세 조회 등에 사용)
data class ArticleResponse(
    val id: Long,
    val lectureId: Long,
    val articleTitle: String,
    val author: String?,
    val blocks: List<ArticleBlockResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class ArticleBlockResponse(
    val id: Long,
    val type: ArticleBlockType,
    val orderIndex: Int,
    val textContent: String?,
    val imageUrl: String?,
    val originalFileName: String?
)