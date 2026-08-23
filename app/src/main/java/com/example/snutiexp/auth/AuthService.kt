package com.example.snutiexp.auth

import com.example.snutiexp.model.*
import retrofit2.Call
import retrofit2.http.*

interface AuthService {

    // --- [1. 회원가입 및 로그인 (비로그인 상태)] ---
    @POST("/auth/register")
    fun signUp(
        @Body request: SignupRequest
    ): Call<AuthResponse>

    @POST("/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>


    // --- [2. 이메일 인증 관련 (회원가입용)] ---
    @POST("/auth/email/send")
    fun sendEmailVerification(
        @Body request: SendEmailCodeRequest
    ): Call<Void>

    @POST("/auth/email/verify")
    fun verifyEmailCode(
        @Body request: VerifyEmailCodeRequest
    ): Call<Void>


    // --- [3. 비밀번호 변경 (로그인 상태 - AuthInterceptor가 토큰 자동 첨부)] ---
    @PATCH("/auth/password")
    fun changePassword(
        @Body request: ChangePasswordRequest
    ): Call<Void>


    // --- [4. 비밀번호 재설정 (비로그인 상태 - 로그인 잊었을 때)] ---
    @POST("/auth/password/reset/send")
    fun sendPasswordResetCode(
        @Body request: PasswordResetSendRequest
    ): Call<Void>

    @POST("/auth/password/reset/verify")
    fun verifyPasswordResetCode(
        @Body request: PasswordResetVerifyRequest
    ): Call<Void>

    @POST("/auth/password/reset")
    fun resetPassword(
        @Body request: PasswordResetRequest
    ): Call<Void>


    // --- [5. 회원 탈퇴 (로그인 상태)] ---
    @DELETE("/auth/me")
    fun deleteAccount(): Call<Void>
}