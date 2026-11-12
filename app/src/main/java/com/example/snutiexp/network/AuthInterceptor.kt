package com.example.snutiexp.network

import android.content.Context
import android.content.Intent
import com.example.snutiexp.auth.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlPath = originalRequest.url.encodedPath

        // 로그인, 회원가입 등 인증 관련 엔드포인트는 401이 떠도 강제 로그아웃시키면 안 됩니다.
        // (비밀번호 틀림 등은 정상적인 실패 응답이기 때문)
        if (urlPath.contains("/login") || urlPath.contains("/signup") || urlPath.contains("/auth")) {
            return chain.proceed(originalRequest)
        }

        // 1. SharedPreferences에서 현재 저장된 토큰을 가져옵니다.
        val sharedPref = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", null)

        // 2. 토큰이 있다면 요청 헤더에 Authorization을 추가합니다.
        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // 3. 서버 응답 코드가 401(Unauthorized)이면 토큰이 만료된 것으로 간주합니다.
        if (response.code == 401 || response.code == 403) {
            // 동시 다발적인 401 호출 시 중복 처리를 막기 위한 방어 코드 (선택 사항이지만 안전)
            synchronized(this) {
                val currentToken = sharedPref.getString("access_token", null)
                if (currentToken != null) {
                    // 토큰을 완전히 비워줍니다.
                    sharedPref.edit().clear().apply()

                    // 로그인 화면으로 강제 이동 (기존 액티비티 스택을 전부 클리어하여 뒤로 가기로 메인에 다시 못 오게 차단)
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
            }
        }

        return response
    }
}