package com.example.snutiexp.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivityMainBinding // 바인딩 클래스 임포트

class MainActivity : AppCompatActivity() {

    // 1. 바인딩 객체 선언 (나중에 초기화하겠다는 의미)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. 바인딩 초기화: XML 레이아웃을 코틀린 객체로 변환합니다.
        binding = ActivityMainBinding.inflate(layoutInflater)

        // 3. 화면 설정: 바인딩된 지도의 최상위 뷰(root)를 화면에 그립니다.
        setContentView(binding.root)

        // 4. 플러스 버튼 클릭 이벤트 구현
        binding.btnAdd.setOnClickListener {
            // AddCourseActivity로 이동하는 의도(Intent) 전달
            val intent = Intent(this, AddCourseActivity::class.java)
            startActivity(intent)
        }
    }
}

//package com.example.snutiexp.main
//
//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.example.snutiexp.R
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//}