
package com.example.snutiexp.main

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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

    // 검색 모드 상태 관리
    private var isSearchMode = false
    private var wasKeyboardVisible = false

    // 현재 드래프트 화면 여부
    private var isCurrentScreenDraft: Boolean = false

    // 현재 선택된 탭 (1: 전체, 2: 맞춤, 3: 저장)
    private var currentTab: Int = 1

    // 서버에서 가져온 전체 태그
    private val allTags = mutableListOf<TagResponse>()

    // 사용자가 선택한 태그
    private val selectedTagNames = mutableSetOf<String>()

    private lateinit var selectedTagAdapter: TagHorizontalAdapter
    private lateinit var recommendedTagAdapter: TagHorizontalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 관리자 여부 확인
        val isAdminUser = checkUserAdminStatus()

        binding.btnAdd.visibility =
            if (isAdminUser) View.VISIBLE else View.GONE

        // 초기 메인 강좌 목록
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    MainLectureListFragment()
                )
                .commit()
        }

        // 초기 검색창 포커스 방지
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()

        // 음성인식 버튼 사용하지 않음
        binding.btnMic.visibility = View.GONE

        // 초기 하단 UI 설정
        updateBottomBarVisibility()

        // 하단 탭 설정
        setupBottomTabs()

        // 태그 RecyclerView 설정
        setupTagRecyclerViews()

        // 서버 태그 조회
        fetchServerTags()

        // 검색어 지우기 버튼
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }

        // 검색어 변경 감지
        binding.etSearch.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val input = s?.toString() ?: ""

                    val currentFragment =
                        supportFragmentManager.findFragmentById(
                            R.id.main_container
                        )

                    when (currentFragment) {
                        is MainLectureListFragment -> {
                            currentFragment.searchLecture(input)
                            refreshRecommendedTagsUI(input)
                        }

                        is CustomLectureListFragment -> {
                            currentFragment.filterCustomLecturesByQuery(input)
                        }

                        is SavedLectureListFragment -> {
                            currentFragment.filterSavedLecturesByQuery(input)
                        }
                    }

                    binding.btnClearSearch.visibility =
                        if (input.isNotEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.btnMic.visibility = View.GONE
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )

        // 관리자 버튼
        binding.btnAdd.setOnClickListener { view ->
            closeSearchMode()
            showAdminPopupMenu(view)
        }

        // 프로필 버튼
        binding.btnProfile.setOnClickListener {
            closeSearchMode()

            val intent = Intent(
                this,
                SettingsActivity::class.java
            )
            startActivity(intent)
        }

        // 검색 버튼
        binding.btnSearchToggle.setOnClickListener {
            openSearchMode()
        }

        // 검색창 포커스 해제 감지
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isSearchMode) {
                closeSearchMode()
            }
        }

        // 키보드 및 시스템 바 높이 처리
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            val ime = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            val isKeyboardVisible = insets.isVisible(
                WindowInsetsCompat.Type.ime()
            )

            // 상단 상태바 대응
            binding.root.setPadding(
                0,
                systemBars.top,
                0,
                0
            )

            // 하단 시스템바 대응
            val bottomBarParams =
                binding.layoutBottomBar.layoutParams
                        as android.view.ViewGroup.MarginLayoutParams

            val defaultBottomMargin =
                (24 * resources.displayMetrics.density).toInt()

            bottomBarParams.bottomMargin =
                defaultBottomMargin + systemBars.bottom

            binding.layoutBottomBar.layoutParams =
                bottomBarParams

            // 키보드가 실제로 열렸다가 닫혔는지 확인
            if (
                isSearchMode &&
                wasKeyboardVisible &&
                !isKeyboardVisible
            ) {
                closeSearchMode()
            }

            wasKeyboardVisible = isKeyboardVisible

            if (isSearchMode && isKeyboardVisible) {

                // 검색창을 키보드 위로 이동
                val raiseHeight = ime.bottom.toFloat()

                binding.layoutSearchBarContainer.translationY =
                    -raiseHeight

                binding.layoutTagSearchContainer.translationY =
                    -raiseHeight

                // 맞춤 탭에서는 태그 검색 영역 숨김
                binding.layoutTagSearchContainer.visibility =
                    if (currentTab == 2) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                if (currentTab != 2 && allTags.isEmpty()) {
                    fetchServerTags()
                }

            } else {

                // 검색창 위치 초기화
                binding.layoutSearchBarContainer.translationY = 0f
                binding.layoutTagSearchContainer.translationY = 0f

                binding.layoutTagSearchContainer.visibility =
                    View.GONE
            }

            updateBottomBarVisibility()

            insets
        }
    }

    private fun openSearchMode() {
        isSearchMode = true

        updateBottomBarVisibility()

        binding.etSearch.requestFocus()

        binding.etSearch.post {
            if (isSearchMode) {
                val imm = getSystemService(
                    INPUT_METHOD_SERVICE
                ) as InputMethodManager

                imm.showSoftInput(
                    binding.etSearch,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    private fun closeSearchMode() {
        if (!isSearchMode) return

        // 먼저 검색 상태 해제
        isSearchMode = false

        // 포커스를 루트로 이동
        binding.root.requestFocus()
        binding.etSearch.clearFocus()

        // 키보드 닫기
        val imm = getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.etSearch.windowToken,
            0
        )

        // 검색창 위치 초기화
        binding.layoutSearchBarContainer.translationY = 0f
        binding.layoutTagSearchContainer.translationY = 0f

        // 원래 하단 UI 복구
        updateBottomBarVisibility()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        if (
            ::binding.isInitialized &&
            isSearchMode &&
            ev.actionMasked == MotionEvent.ACTION_DOWN
        ) {

            val searchRect = Rect()
            val tagRect = Rect()

            // 검색창 내부 터치인지 확인
            val onSearch =
                binding.layoutSearchBarContainer.isShown &&
                        binding.layoutSearchBarContainer.getGlobalVisibleRect(
                            searchRect
                        ) &&
                        searchRect.contains(
                            ev.rawX.toInt(),
                            ev.rawY.toInt()
                        )

            // 태그 검색 영역 내부 터치인지 확인
            val onTags =
                binding.layoutTagSearchContainer.isShown &&
                        binding.layoutTagSearchContainer.getGlobalVisibleRect(
                            tagRect
                        ) &&
                        tagRect.contains(
                            ev.rawX.toInt(),
                            ev.rawY.toInt()
                        )

            // 검색창과 태그 영역 밖을 누르면 검색 종료
            if (!onSearch && !onTags) {
                closeSearchMode()
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()

        if (::binding.isInitialized && isSearchMode) {
            closeSearchMode()
        }
    }

    private fun updateBottomBarVisibility() {

        // 검색 중이면 하단 탭 숨기기
        binding.layoutBottomBar.visibility =
            if (isSearchMode) {
                View.GONE
            } else {
                View.VISIBLE
            }

        // 검색창 가시성
        binding.layoutSearchBarContainer.visibility =
            if (isSearchMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        // 검색 종료 시 태그 검색 영역 숨기기
        if (!isSearchMode) {
            binding.layoutTagSearchContainer.visibility =
                View.GONE
        }

        // 드래프트 화면에서는 기존 탭 및 검색 버튼 숨김
        val showStandardControls = !isCurrentScreenDraft

        binding.layoutSearchGroup.visibility =
            if (showStandardControls) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnSearchToggle.visibility =
            if (showStandardControls) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun setupTagRecyclerViews() {
        val isAdminUser = checkUserAdminStatus()

        selectedTagAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = true,
            onItemClick = { tagName ->
                selectedTagNames.remove(tagName)
                refreshTagUI()
            }
        )

        binding.layoutSelectedTags.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = selectedTagAdapter
        }

        recommendedTagAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = false,

            selectedChecker = { tagName ->
                selectedTagNames.contains(tagName)
            },
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
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = recommendedTagAdapter
        }
    }

    private fun fetchServerTags() {

        RetrofitClient.service.getTagsAll().enqueue(
            object : Callback<List<TagResponse>> {

                override fun onResponse(
                    call: Call<List<TagResponse>>,
                    response: Response<List<TagResponse>>
                ) {
                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {
                        allTags.clear()
                        allTags.addAll(response.body()!!)

                        refreshRecommendedTagsUI(
                            binding.etSearch.text.toString()
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<TagResponse>>,
                    t: Throwable
                ) {
                    Log.e(
                        "API_ERROR",
                        "태그 목록 로드 실패: ${t.message}"
                    )
                }
            }
        )
    }

    private fun refreshTagUI() {

        selectedTagAdapter.updateItems(
            selectedTagNames.toList()
        )

        recommendedTagAdapter.notifyDataSetChanged()

        val currentFragment =
            supportFragmentManager.findFragmentById(
                R.id.main_container
            )

        when (currentFragment) {

            is MainLectureListFragment -> {
                currentFragment.filterByTags(
                    selectedTagNames.toList()
                )
            }

            is SavedLectureListFragment -> {
                currentFragment.filterByTags(
                    selectedTagNames.toList()
                )
            }
        }
    }

    private fun refreshRecommendedTagsUI(keyword: String) {

        val trimmed = keyword.trim()

        val filtered = if (trimmed.isEmpty()) {
            allTags.map { it.name }
        } else {
            allTags.filter {
                it.name.contains(
                    trimmed,
                    ignoreCase = true
                )
            }.map { it.name }
        }

        recommendedTagAdapter.updateItems(filtered)
    }

    private fun setupBottomTabs() {

        // 전체 강좌 목록
        binding.btnTabAll.setOnClickListener {

            closeSearchMode()

            currentTab = 1
            updateTabSelection(selectedTab = 1)

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    MainLectureListFragment()
                )
                .commit()
        }

        // 맞춤 강좌 목록
        binding.btnTabCustom.setOnClickListener {

            closeSearchMode()

            currentTab = 2
            updateTabSelection(selectedTab = 2)

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    CustomLectureListFragment()
                )
                .commit()
        }

        // 저장된 강좌 목록
        binding.btnTabSaved.setOnClickListener {

            closeSearchMode()

            currentTab = 3
            updateTabSelection(selectedTab = 3)

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    SavedLectureListFragment()
                )
                .commit()
        }
    }

    private fun updateTabSelection(selectedTab: Int) {

        val blueColor = ContextCompat.getColor(
            this,
            R.color.blue_main
        )

        val blackColor = Color.BLACK

        // 전체 탭
        val isAllSelected = (selectedTab == 1)

        binding.btnTabAll.setBackgroundResource(
            if (isAllSelected) {
                R.drawable.bg_inner_search_bar
            } else {
                0
            }
        )

        val ivAll =
            binding.btnTabAll.getChildAt(0)
                    as? android.widget.ImageView

        val tvAll =
            binding.btnTabAll.getChildAt(1)
                    as? android.widget.TextView

        ivAll?.setColorFilter(
            if (isAllSelected) blueColor else blackColor
        )

        tvAll?.setTextColor(
            if (isAllSelected) blueColor else blackColor
        )

        // 맞춤 탭
        val isCustomSelected = (selectedTab == 2)

        binding.btnTabCustom.setBackgroundResource(
            if (isCustomSelected) {
                R.drawable.bg_inner_search_bar
            } else {
                0
            }
        )

        val ivCustom =
            binding.btnTabCustom.getChildAt(0)
                    as? android.widget.ImageView

        val tvCustom =
            binding.btnTabCustom.getChildAt(1)
                    as? android.widget.TextView

        ivCustom?.setColorFilter(
            if (isCustomSelected) blueColor else blackColor
        )

        tvCustom?.setTextColor(
            if (isCustomSelected) blueColor else blackColor
        )

        // 저장 탭
        val isSavedSelected = (selectedTab == 3)

        binding.btnTabSaved.setBackgroundResource(
            if (isSavedSelected) {
                R.drawable.bg_inner_search_bar
            } else {
                0
            }
        )

        val ivSaved =
            binding.btnTabSaved.getChildAt(0)
                    as? android.widget.ImageView

        val tvSaved =
            binding.btnTabSaved.getChildAt(1)
                    as? android.widget.TextView

        ivSaved?.setColorFilter(
            if (isSavedSelected) blueColor else blackColor
        )

        tvSaved?.setTextColor(
            if (isSavedSelected) blueColor else blackColor
        )
    }

    private fun showAdminPopupMenu(anchorView: View) {

        val popup = PopupMenu(this, anchorView)

        if (!isCurrentScreenDraft) {

            popup.menuInflater.inflate(
                R.menu.menu_admin_options,
                popup.menu
            )

            popup.setForceShowIcon(true)

            popup.setOnMenuItemClickListener { item ->

                when (item.itemId) {

                    R.id.menu_add_course -> {
                        val intent = Intent(
                            this,
                            AddCourseActivity::class.java
                        )

                        startActivity(intent)
                        true
                    }

                    R.id.menu_draft_list -> {

                        supportFragmentManager.beginTransaction()
                            .replace(
                                R.id.main_container,
                                DraftLectureListFragment()
                            )
                            .commit()

                        isCurrentScreenDraft = true

                        updateTitle()
                        updateBottomBarVisibility()

                        Toast.makeText(
                            this,
                            "드래프트 목록으로 전환되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

                        true
                    }

                    else -> false
                }
            }

        } else {

            popup.menuInflater.inflate(
                R.menu.menu_draft_options,
                popup.menu
            )

            popup.setForceShowIcon(true)

            popup.setOnMenuItemClickListener { item ->

                when (item.itemId) {

                    R.id.menu_add_course_from_draft -> {

                        val intent = Intent(
                            this,
                            AddCourseActivity::class.java
                        )

                        startActivity(intent)
                        true
                    }

                    R.id.menu_go_back_to_main -> {

                        supportFragmentManager.beginTransaction()
                            .replace(
                                R.id.main_container,
                                MainLectureListFragment()
                            )
                            .commit()

                        isCurrentScreenDraft = false

                        updateTitle()
                        updateBottomBarVisibility()

                        Toast.makeText(
                            this,
                            "기존 강좌 목록 화면으로 복귀했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

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
            binding.tvMainTitle.text = "Draft 강좌 목록"
        } else {
            binding.tvMainTitle.text = "강좌 목록"
        }
    }

    private fun checkUserAdminStatus(): Boolean {

        val sharedPref = getSharedPreferences(
            "token_prefs",
            MODE_PRIVATE
        )

        return sharedPref.getBoolean(
            "is_admin_user",
            false
        )
    }
}
