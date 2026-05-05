package com.example.snutiexp.login

data class LoginResponse(
    val message: String,
    val success: Boolean,
    val accessToken: String?
)