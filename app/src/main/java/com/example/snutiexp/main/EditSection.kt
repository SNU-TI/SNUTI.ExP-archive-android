package com.example.snutiexp.main

data class EditSection(
    val id: Int,
    var type: String, // "TEXT" 또는 "IMAGE"
    var content: String = "",
    var imageUri: String? = null
)