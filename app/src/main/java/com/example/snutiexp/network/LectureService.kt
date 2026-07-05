package com.example.snutiexp.network

import com.example.snutiexp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface LectureService {
    // 강연 생성 (POST /admin/lectures)
    @POST("/admin/lectures")
    fun createLecture(@Body request: LectureCreateRequest): Call<LectureCreateResponse>

    // 강연에 아티클(섹션) 추가 (POST /admin/lectures/{lectureId}/articles)
    @Multipart
    @POST("/admin/lectures/{lectureId}/articles")
    fun addArticle(
        @Path("lectureId") lectureId: Long,
        @Part("request") request: RequestBody,      // JSON 데이터를 RequestBody로 감싸서 보냅니다.
        @Part image: MultipartBody.Part? = null     // 실제 이미지 파일입니다. (선택 사항)
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
}