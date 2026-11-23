package com.snuti.exp.network

import android.content.Context
import android.content.Intent
import com.snuti.exp.auth.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlPath = originalRequest.url.encodedPath

        // 로그인, 회원가입 등 인증 관련 엔드포인트는 403이 떠도 강제 로그아웃시키면 안 됩니다.
        // (비밀번호 틀림 등은 정상적인 실패 응답이기 때문)
        if (urlPath.contains("/auth/login") || urlPath.contains("/auth/signup") || urlPath.contains("/auth/register")) {
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
        //    403(Forbidden - 권한 없음)은 서버 권한 정책이나 데이터 상태에 따라 다를 수 있으므로
        //    무조건적인 토큰 삭제 및 강제 로그아웃 대상에서 제외하는 것이 안전합니다.
        if (response.code == 401) {
            synchronized(this) {
                val currentToken = sharedPref.getString("access_token", null)
                if (!currentToken.isNullOrEmpty()) {
                    // 이동 전에 세션 정보를 확실히 비웁니다.
                    sharedPref.edit().clear().commit()

                    // 스택 정리 플래그를 추가해 LoginActivity로 이동합니다.
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(intent)
                }
            }
        }

        return response
    }
}