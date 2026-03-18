// AuthService.kt 내용
package com.example.snutiexp.signup

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.snutiexp.login.LoginRequest
import com.example.snutiexp.login.LoginResponse

interface AuthService {
    @POST("/auth/register")
    fun signUp(  // 이 이름이 중요해요!
        @Body request: SignupRequest
    ): Call<SignupResponse>

    // 로그인 통로 추가! (주소는 스웨거에서 꼭 확인해보세요)
    @POST("/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}