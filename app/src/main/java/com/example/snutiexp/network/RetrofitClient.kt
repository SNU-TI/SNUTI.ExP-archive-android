package com.example.snutiexp.network

import android.content.Context
import com.example.snutiexp.signup.AuthService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://43.200.176.8:8080/"

    private var okHttpClient: OkHttpClient? = null
    private var _service: LectureService? = null
    private var _authService: AuthService? = null

    fun init(context: Context) {
        // Interceptor 생성 시에만 context 사용 (ApplicationContext를 사용하는 것이 가장 안전)
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(context.applicationContext)) // 👈 중요: applicationContext 사용
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val service: LectureService
        get() = _service ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LectureService::class.java).also { _service = it }

    // 로그인/회원가입 전용 통신 서비스 인터페이스 연결 생성
    val authService: AuthService
        get() = _authService ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java).also { _authService = it }
}