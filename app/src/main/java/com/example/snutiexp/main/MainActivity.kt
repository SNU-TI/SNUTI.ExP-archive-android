package com.example.snutiexp.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snutiexp.R
import com.example.snutiexp.databinding.ActivityMainBinding // 바인딩 클래스 임포트

class MainActivity : AppCompatActivity() {

    // 바인딩 객체 선언 (나중에 초기화하겠다는 의미)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 메인 리스트 프래그먼트를 처음에 띄웁니다.
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, MainLectureListFragment()) // ActivityMain의 컨테이너 ID 확인 필요
            .commit()

        // 플러스 버튼 클릭 이벤트 구현
        binding.btnAdd.setOnClickListener {
            // AddCourseActivity로 이동하는 의도(Intent) 전달
            val intent = Intent(this, AddCourseActivity::class.java)
            startActivity(intent)
        }

        // 사람 아이콘 클릭 시
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val searchGroup = binding.layoutSearchGroup

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // 키보드의 실시간 높이(인셋)를 가져옵니다.
            val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (isKeyboardVisible) {
                // [키보드가 떴을 때] 키보드 높이만큼 검색 그룹을 위로 올림
                binding.layoutSearchGroup.translationY = -keyboardHeight.toFloat()

                // 디자인 변경: 구분선과 태그 영역 노출
                binding.searchDivider.visibility = View.VISIBLE
                binding.layoutTagArea.visibility = View.VISIBLE
            } else {
                // [키보드가 닫혔을 때] 원래 위치로 복귀 및 포커스 해제
                binding.layoutSearchGroup.translationY = 0f
                binding.etSearch.clearFocus()

                binding.searchDivider.visibility = View.GONE
                binding.layoutTagArea.visibility = View.GONE
            }
            insets
        }

        // 배경 터치 시 포커스 해제 로직 유지
        binding.root.setOnClickListener {
            binding.etSearch.clearFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }
}