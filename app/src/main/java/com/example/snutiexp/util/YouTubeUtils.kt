package com.example.snutiexp.util

object YouTubeUtils {
    fun getYoutubeVideoId(url: String): String? {
        val regex = "(?<=watch\\?v=|/videos/|embed/|youtu\\.be/)[^#&?]*"
        val pattern = Regex(regex)
        return pattern.find(url)?.value
    }
}