package com.example.snutiexp.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snutiexp.R
import com.example.snutiexp.databinding.ActivityMainBinding
import androidx.core.widget.addTextChangedListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 현재 띄워진 화면 상태를 추적하는 동적 플래그 변수 (false:초기값/true:드래프트 강좌)
    private var isCurrentScreenDraft: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // (현재 앱의 로그인 방식이나 전역 상태 관리 로직에 맞춰서 검증 로직 변수를 대입하세요)
        val isAdminUser = checkUserAdminStatus() // 예시: SharedPreferences 나 싱글톤 객체 등에서 가져옴

        if (isAdminUser) {
            // 관리자 계정이 맞다면 숨겨져 있던 플러스 버튼을 화면에 다시 노출시킵니다.
            binding.btnAdd.visibility = View.VISIBLE
        } else {
            // 일반 사용자 계정이라면 확실히 보이지 않도록 잠가 둡니다.
            binding.btnAdd.visibility = View.GONE
        }

        // 메인 리스트 프래그먼트를 처음에 띄웁니다.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    MainLectureListFragment()
                )
                .commit()
        }


// 검색창 입력 내용을 강의 목록 Fragment에 전달
        binding.etSearch.addTextChangedListener { editable ->
            val keyword = editable?.toString().orEmpty()

            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.main_container)

            if (currentFragment is MainLectureListFragment) {
                currentFragment.searchLecture(keyword)
            }
        }
        // 관리자 버튼
        binding.btnAdd.setOnClickListener { view ->
            showAdminPopupMenu(view)
//            // AddCourseActivity로 이동하는 의도(Intent) 전달
//            val intent = Intent(this, AddCourseActivity::class.java)
//            startActivity(intent)
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
            } else {
                // [키보드가 닫혔을 때] 원래 위치로 복귀 및 포커스 해제
                binding.layoutSearchGroup.translationY = 0f
                binding.etSearch.clearFocus()
            }
            insets
        }

        // 배경 터치 시 포커스 해제 로직 유지
        binding.root.setOnClickListener {
            binding.etSearch.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    // 관리자 전용 드롭다운 팝업창
    private fun showAdminPopupMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        if (!isCurrentScreenDraft) {
            // 일반 강좌 목록 상태일 때: 기존의 '새 강좌 추가' / '드래프트 보기' 메뉴를 팽창시킵니다.
            popup.menuInflater.inflate(R.menu.menu_admin_options, popup.menu)
            // 기본 팝업 메뉴에서 아이콘을 강제로 보여주게 만드는 핵심 기법
            popup.setForceShowIcon(true)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_add_course -> {
                        val intent = Intent(this, AddCourseActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.menu_draft_list -> {
                        // 드래프트 보기 클릭 시: 드래프트 전용 프래그먼트로 화면을 갈아끼우고 플래그를 true로 스위칭합니다.
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, DraftLectureListFragment()) // 추후 구현할 드래프트 프래그먼트 가정
                            .commit()
                        isCurrentScreenDraft = true
                        updateTitle()
                        Toast.makeText(this, "드래프트 목록으로 전환되었습니다.", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
        } else {
            // 드래프트 강좌 목록에 이미 진입한 상태일 때: 새로운 메뉴 스펙을 팽창시킵니다.
            popup.menuInflater.inflate(R.menu.menu_draft_options, popup.menu)
            // 기본 팝업 메뉴에서 아이콘을 강제로 보여주게 만드는 핵심 기법
            popup.setForceShowIcon(true)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_add_course_from_draft -> {
                        // 새 강좌 추가하기 선택 시: 동일하게 강좌 추가 액티비티 화면으로 연결합니다.
                        val intent = Intent(this, AddCourseActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.menu_go_back_to_main -> {
                        // 기존 화면으로 돌아가기 선택 시: 다시 메인 리스트 프래그먼트를 안착시키고 플래그를 false로 원복합니다.
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, MainLectureListFragment())
                            .commit()
                        isCurrentScreenDraft = false
                        binding.etSearch.setText("")
                        updateTitle()
                        Toast.makeText(this, "기존 강좌 목록 화면으로 복귀했습니다.", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
        }
        popup.show()
    }

    private fun updateTitle() {
        if (isCurrentScreenDraft) {
            binding.tvMainTitle.text = "Draft 강좌 목록" // ID는 실제 XML에 맞게 수정
        } else {
            binding.tvMainTitle.text = "강좌 목록"
        }
    }

    // 로그인 시 SharedPreferences에 판별 보관된 관리자 플래그를 꺼내 연동
    private fun checkUserAdminStatus(): Boolean {
        // LoginActivity와 동일한 이름의 세션 저장소 파일을 참조합니다.
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)

        // 저장된 플래그를 꺼내옵니다. (보안 및 미인증 유저 예방을 위해 기본값은 false 처리)
        return sharedPref.getBoolean("is_admin_user", false)
    }
}