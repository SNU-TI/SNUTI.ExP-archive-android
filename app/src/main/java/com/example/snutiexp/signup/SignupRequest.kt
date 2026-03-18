package com.example.snutiexp.signup

import com.google.gson.annotations.SerializedName

// 서버에 보낼 데이터 가방
data class SignupRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// 서버에서 받을 결과 가방
data class SignupResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("success") val isSuccess: Boolean
)