package com.example.snutiexp.model

import com.google.gson.annotations.SerializedName

// --- [회원가입 / 인증 / 로그인] ---
data class SignupRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String
)

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String
)

data class ChangePasswordRequest(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

// --- [이메일 인증 (로그인 상태)] ---
data class VerifyEmailCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

// --- [이메일 인증 요청 (회원가입용/공통)] ---
data class SendEmailCodeRequest(
    @SerializedName("email") val email: String
)

// --- [비밀번호 재설정 (로그아웃/비밀번호 찾기 상태)] ---
data class PasswordResetSendRequest(
    @SerializedName("email") val email: String
)

data class PasswordResetVerifyRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

data class PasswordResetRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("newPassword") val newPassword: String
)

// --- [공통 에러 응답 모델] ---
data class AuthErrorResponse(
    @SerializedName("message") val message: String?
)