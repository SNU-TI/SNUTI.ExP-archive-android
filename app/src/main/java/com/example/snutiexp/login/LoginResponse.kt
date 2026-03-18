package com.example.snutiexp.login

data class LoginResponse(
    val message: String,
    val success: Boolean
    // 서버 응답에 토큰(token)이 있다면 여기에 추가해야 합니다.
)