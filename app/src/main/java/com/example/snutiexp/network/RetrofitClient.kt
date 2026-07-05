package com.example.snutiexp.network

import com.example.snutiexp.signup.AuthService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://43.200.176.8:8080/"
    private var authToken: String? = null
    fun setToken(token: String) {
        this.authToken = token
    }

    // 1. 모든 요청에 토큰을 자동으로 추가하는 인터셉터 설정
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            authToken?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            chain.proceed(requestBuilder.build())
        }
        // 로그캣에서 통신 내용을 상세히 보기 위한 로그 인터셉터 (선택 사항)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: LectureService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LectureService::class.java)
    }

    // 로그인/회원가입 전용 통신 서비스 인터페이스 연결 생성
    val authService: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java) // ◀️ AuthService를 여기서 생성해 줍니다.
    }
}