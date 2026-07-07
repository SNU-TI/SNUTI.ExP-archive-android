package com.example.snutiexp.network

import android.content.Context
import android.content.Intent
import com.example.snutiexp.login.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

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
            // 토큰을 삭제하여 자동 로그인을 해제합니다.
            sharedPref.edit().clear().apply() // remove("access_token") 대신 clear()를 써서 설정 전체를 초기화하는 것도 좋습니다.

            // 로그인 화면으로 강제 이동합니다.
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}