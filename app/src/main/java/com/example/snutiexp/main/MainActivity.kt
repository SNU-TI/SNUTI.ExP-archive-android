package com.example.snutiexp.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.R
import com.example.snutiexp.databinding.ActivityMainBinding
import com.example.snutiexp.model.TagResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 현재 띄워진 화면 상태를 추적하는 동적 플래그 변수 (false:초기값/true:드래프트 강좌)
    private var isCurrentScreenDraft: Boolean = false

    // 현재 선택된 탭 번호 추적 변수 (1: 전체, 2: 맞춤, 3: 저장)
    private var currentTab: Int = 1

    // 서버에서 가져온 전체 태그 목록
    private val allTags = mutableListOf<TagResponse>()
    // 사용자가 현재 선택한 태그 이름 목록 (다중 선택 지원)
    private val selectedTagNames = mutableSetOf<String>()

    // 두 개의 리사이클러뷰를 위한 TagHorizontalAdapter 선언
    private lateinit var selectedTagAdapter: TagHorizontalAdapter
    private lateinit var recommendedTagAdapter: TagHorizontalAdapter

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

        // 초기 하단 바 가시성 설정
        updateBottomBarVisibility()

        // 하단 탭 버튼 클릭 리스너 연결 (전체 / 맞춤 / 저장)
        setupBottomTabs()

        // 태그 리사이클러뷰 2개 초기화 (선택된 영역 + 전체 추천 영역)
        setupTagRecyclerViews()

        // 서버 태그 목록을 불러오는 함수 호출
        fetchServerTags()

        // 1. X(클리어) 버튼 클릭 시 텍스트 초기화
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }

        // 2. 검색창 텍스트 변화 감지 리스너 (글자 유무에 따라 마이크 / X 버튼 전환)
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString() ?: ""

                val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
                if (currentFragment is MainLectureListFragment) {
                    currentFragment.searchLecture(input)
                    refreshRecommendedTagsUI(input)
                } else if (currentFragment is CustomLectureListFragment) {
                    currentFragment.filterCustomLecturesByQuery(input)
                } else if (currentFragment is SavedLectureListFragment) {
                    currentFragment.filterSavedLecturesByQuery(input)
                }

                if (input.isNotEmpty()) {
                    // 검색어가 있으면 X 버튼 노출, 마이크 버튼 숨김
                    binding.btnClearSearch.visibility = View.VISIBLE
                    binding.btnMic.visibility = View.GONE
                } else {
                    // 검색어가 없으면 X 버튼 숨김, 마이크 버튼 노출 (마이크를 안 쓰신다면 둘 다 GONE 처리 가능)
                    binding.btnClearSearch.visibility = View.GONE
                    binding.btnMic.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 관리자 버튼
        binding.btnAdd.setOnClickListener { view ->
            showAdminPopupMenu(view)
        }

        // 사람 아이콘 클릭 시
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 우측 독립 검색 버튼 클릭 시: 에디트텍스트에 포커스를 주고 키보드를 강제로 올림
        binding.btnSearchToggle.setOnClickListener {
            binding.etSearch.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // 시스템 인셋(상태바 및 키보드/내비게이션바) 동적 대응 리스너
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // 상단 상태바(카메라 영역) & 하단 시스템바(네비게이션바) 높이 가져오기
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            // [상단 대응] 프로필/타이틀이 펀치홀 카메라나 상태바에 가리지 않도록 루트 뷰에 상단 패딩 적용
            binding.root.setPadding(0, systemBars.top, 0, 0)

            // [하단 대응] 검색바가 제스처바나 내비게이션바와 겹치지 않도록 마진 계산
            val bottomBarParams = binding.layoutBottomBar.layoutParams as android.view.ViewGroup.MarginLayoutParams
            val defaultBottomMargin = (24 * resources.displayMetrics.density).toInt()

            if (isKeyboardVisible) {
                // [키보드가 떴을 때]
                // 1. 태그 검색 영역과 검색바 컨테이너를 화면에 보임
                binding.layoutTagSearchContainer.visibility = View.VISIBLE
                binding.layoutSearchBarContainer.visibility = View.VISIBLE

                // 2. 하단 탭 바와 돋보기 버튼 영역은 키보드에 의해 가려지므로 숨김 처리
                binding.layoutBottomBar.visibility = View.GONE

                // 3. 실제 검색바들을 키보드 높이만큼 위로 올려서 키보드 바로 위에 위치시킴
                val raiseHeight = ime.bottom
                binding.layoutSearchBarContainer.translationY = -raiseHeight.toFloat()

                // 맞춤 탭(currentTab == 2)일 때는 태그 검색 영역을 숨김 처리하고, 다른 탭일 때만 보여줌
                if (currentTab == 2) {
                    binding.layoutTagSearchContainer.visibility = View.GONE
                } else {
                    binding.layoutTagSearchContainer.visibility = View.VISIBLE
                    binding.layoutTagSearchContainer.translationY = -raiseHeight.toFloat()

                    if (allTags.isEmpty()) {
                        fetchServerTags()
                    }
                }
            } else {
                // [키보드가 닫혔을 때]
                // 1. 태그 검색 영역을 다시 숨김
                binding.layoutTagSearchContainer.visibility = View.GONE
                binding.layoutSearchBarContainer.visibility = View.GONE

                // 2. 하단 바를 다시 표시하고 돋보기 버튼과 탭이 동일한 위치에 나란히 안착되도록 함
                binding.layoutBottomBar.visibility = View.VISIBLE
                updateBottomBarVisibility()
                bottomBarParams.bottomMargin = defaultBottomMargin + systemBars.bottom
                binding.layoutBottomBar.layoutParams = bottomBarParams

                // 3. 위치 초기화
                binding.layoutSearchBarContainer.translationY = 0f
                binding.layoutTagSearchContainer.translationY = 0f
                binding.etSearch.clearFocus()
            }
            insets
        }

        // 배경 터치 시 포커스 해제 로직 유지
        binding.root.setOnClickListener {
            binding.etSearch.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }

    // 선택된 영역 및 전체 추천 영역 RecyclerView와 어댑터 설정
    private fun setupTagRecyclerViews() {
        // 1) 상단: 사용자가 선택한 태그들이 표시되는 어댑터 (클릭 시 선택 해제되어 위쪽에서 제거됨)
        selectedTagAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = true,
            onItemClick = { tagName ->
                selectedTagNames.remove(tagName)
                refreshTagUI()
            }
        )
        binding.layoutSelectedTags.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedTagAdapter
        }

        // 2) 하단: 전체/추천 태그 어댑터 (클릭 시 선택되거나 해제됨)
        recommendedTagAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = false,
            selectedChecker = { tagName -> selectedTagNames.contains(tagName) },
            onItemClick = { tagName ->
                if (selectedTagNames.contains(tagName)) {
                    selectedTagNames.remove(tagName)
                } else {
                    selectedTagNames.add(tagName)
                }
                refreshTagUI()
            }
        )
        binding.layoutAllTags.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedTagAdapter
        }
    }

    // 서버로부터 전체 태그 목록을 불러오는 함수
    private fun fetchServerTags() {
        RetrofitClient.service.getTagsAll().enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    allTags.clear()
                    allTags.addAll(response.body()!!)
                    refreshRecommendedTagsUI(binding.etSearch.text.toString())
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {
                Log.e("API_ERROR", "태그 목록 로드 실패: ${t.message}")
            }
        })
    }

    // 태그가 추가/제거될 때 UI를 새로고침하고 프래그먼트에 다중 태그 필터 전달
    private fun refreshTagUI() {
        // 상단 선택된 태그 목록 리스트 갱신
        selectedTagAdapter.updateItems(selectedTagNames.toList())
        // 하단 추천 태그 목록의 선택 상태(회색 처리 등) 갱신
        recommendedTagAdapter.notifyDataSetChanged()

        // 현재 프래그먼트에 선택된 태그 이름(또는 ID) 리스트 전달하여 강좌 목록 필터링 실행
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_container)
        when (currentFragment) {
            is MainLectureListFragment -> {
                currentFragment.filterByTags(selectedTagNames.toList())
            }
            is SavedLectureListFragment -> {
                currentFragment.filterByTags(selectedTagNames.toList())
            }
        }
    }

    // 검색어에 따른 하단 추천 태그 실시간 필터링
    private fun refreshRecommendedTagsUI(keyword: String) {
        val trimmed = keyword.trim()
        val filtered = if (trimmed.isEmpty()) {
            allTags.map { it.name }
        } else {
            allTags.filter { it.name.contains(trimmed, ignoreCase = true) }.map { it.name }
        }
        recommendedTagAdapter.updateItems(filtered)
    }

    // 드래프트 상태에 따라 하단 탭 바의 가시성을 조절하는 함수
    private fun updateBottomBarVisibility() {
        if (isCurrentScreenDraft) {
            // 드래프트 화면: 전체/맞춤/저장 탭 바 숨김, 돋보기 버튼은 유지
            binding.layoutSearchGroup.visibility = View.GONE
            binding.btnSearchToggle.visibility = View.GONE
        } else {
            // 일반 메인 화면: 탭 바 및 돋보기 버튼 모두 표시
            binding.layoutSearchGroup.visibility = View.VISIBLE
            binding.btnSearchToggle.visibility = View.VISIBLE
        }
    }

    // 하단 탭(전체, 맞춤, 저장) 선택 상태 관리 함수
    private fun setupBottomTabs() {
        binding.btnTabAll.setOnClickListener {
            currentTab = 1
            updateTabSelection(selectedTab = 1)
            // 전체 강좌 목록 프래그먼트로 전환
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, MainLectureListFragment())
                .commit()
        }

        binding.btnTabCustom.setOnClickListener {
            currentTab = 2
            updateTabSelection(selectedTab = 2)
            // 맞춤 강좌 목록 프래그먼트로 전환
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, CustomLectureListFragment())
                .commit()
        }

        binding.btnTabSaved.setOnClickListener {
            currentTab = 3
            updateTabSelection(selectedTab = 3)
            // 저장된 강좌 목록 프래그먼트로 전환하도록 추가
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, SavedLectureListFragment())
                .commit()
        }
    }

    // 탭을 눌렀을 때 선택된 탭은 파란색(아이콘 틴트/텍스트/배경)으로, 나머지는 검은색/기본값으로 변경
    private fun updateTabSelection(selectedTab: Int) {
        val blueColor = ContextCompat.getColor(this, R.color.blue_main)
        val blackColor = Color.BLACK

        // 1. 전체 탭 (1)
        val isAllSelected = (selectedTab == 1)
        binding.btnTabAll.setBackgroundResource(if (isAllSelected) R.drawable.bg_inner_search_bar else 0)
        // 아이콘 틴트 색상 적용
        val ivAll = binding.btnTabAll.getChildAt(0) as? android.widget.ImageView
        val tvAll = binding.btnTabAll.getChildAt(1) as? android.widget.TextView
        ivAll?.setColorFilter(if (isAllSelected) blueColor else blackColor)
        tvAll?.setTextColor(if (isAllSelected) blueColor else blackColor)

        // 2. 맞춤 탭 (2)
        val isCustomSelected = (selectedTab == 2)
        binding.btnTabCustom.setBackgroundResource(if (isCustomSelected) R.drawable.bg_inner_search_bar else 0)
        val ivCustom = binding.btnTabCustom.getChildAt(0) as? android.widget.ImageView
        val tvCustom = binding.btnTabCustom.getChildAt(1) as? android.widget.TextView
        ivCustom?.setColorFilter(if (isCustomSelected) blueColor else blackColor)
        tvCustom?.setTextColor(if (isCustomSelected) blueColor else blackColor)

        // 3. 저장 탭 (3)
        val isSavedSelected = (selectedTab == 3)
        binding.btnTabSaved.setBackgroundResource(if (isSavedSelected) R.drawable.bg_inner_search_bar else 0)
        val ivSaved = binding.btnTabSaved.getChildAt(0) as? android.widget.ImageView
        val tvSaved = binding.btnTabSaved.getChildAt(1) as? android.widget.TextView
        ivSaved?.setColorFilter(if (isSavedSelected) blueColor else blackColor)
        tvSaved?.setTextColor(if (isSavedSelected) blueColor else blackColor)
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
                        updateBottomBarVisibility()
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
                        updateTitle()
                        updateBottomBarVisibility()
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