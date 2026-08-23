package com.example.snutiexp.network

import com.example.snutiexp.model.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface LectureService {
    // 강연 생성 (POST /admin/lectures)
    @POST("/admin/lectures")
    fun createLecture(@Body request: LectureCreateRequest): Call<LectureCreateResponse>

    // 강연에 비디오 추가 (POST /admin/lectures/{lectureId}/videos)
    @POST("/admin/lectures/{lectureId}/videos")
    fun addVideo(
        @Path("lectureId") lectureId: Long,
        @Body request: CreateVideoRequest
    ): Call<VideoResponse> // 응답 객체 반환

    // 비디오 수정 (PUT /admin/videos/{videoId})
    @PUT("/admin/videos/{videoId}")
    fun updateVideo(
        @Path("videoId") videoId: Long,
        @Body request: UpdateVideoRequest
    ): Call<VideoResponse>

    // 비디오 삭제 (DELETE /admin/videos/{videoId})
    @DELETE("/admin/videos/{videoId}")
    fun deleteVideo(
        @Path("videoId") videoId: Long
    ): Call<Void>

    // 강연 수정(POST /admin/lectures/{lectureId})
    @PATCH("/admin/lectures/{lectureId}")
    fun updateLecture(
        @Path("lectureId") lectureId: Long,
        @Body request: LectureUpdateRequest
    ): Call<LectureCreateResponse>

    // 강연에 아티클(섹션) 추가 (POST /admin/lectures/{lectureId}/articles)
    @Multipart
    @POST("/admin/lectures/{lectureId}/articles")
    fun createArticle(
        @Path("lectureId") lectureId: Long,
        @Part parts: List<MultipartBody.Part>
    ): Call<ArticleResponse>

    // 아티클 수정 (PUT /admin/articles/{articleId})
    @Multipart
    @PUT("/admin/articles/{articleId}")
    fun updateArticle(
        @Path("articleId") articleId: Long,
        @Part parts: List<MultipartBody.Part>
    ): Call<ArticleResponse>

    // 아티클 삭제 (DELETE /admin/articles/{articleId})
    @DELETE("/admin/articles/{articleId}")
    fun deleteArticle(
        @Path("articleId") articleId: Long
    ): Call<Void>

    // 강연 목록 조회 (GET /lectures)
    @GET("/lectures")
    fun getLectures(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "id,desc"
    ): Call<LectureListResponse>

    // 강연 상세 조회
    @GET("/lectures/{id}")
    fun getLectureDetail(
        @Path("id") id: Long
    ): Call<LectureDetailResponse>

    // draft 강연 상세 조회
    @GET("/admin/lectures/{lectureId}")
    fun getAdminLectureDetail(
        @Path("lectureId") lectureId: Long
    ): Call<LectureDetailResponse>

    // 임시 저장된 드래프트 강좌들을 List 형태로 받음
    @GET("/admin/lectures/drafts")
    fun getDraftLectures(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<LectureListResponse>

    // 태그 생성/조회 (필요한 경우)
    @POST("/admin/tags")
    fun createTag(@Body request: TagCreateRequest): Call<TagResponse>

    @GET("/admin/tags")
    fun getTags(): Call<List<TagResponse>>
}