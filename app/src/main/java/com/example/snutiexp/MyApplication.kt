package com.example.snutiexp

import android.app.Application
import com.example.snutiexp.network.RetrofitClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱이 켜질 때 여기서 딱 한 번 초기화합니다.
        RetrofitClient.init(this)
    }
}