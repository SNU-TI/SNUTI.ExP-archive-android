package com.example.snutiexp.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.ActivityUserInterestBinding
import com.example.snutiexp.model.TagResponse
import com.example.snutiexp.model.UserInterestUpdateRequest
import com.example.snutiexp.network.RetrofitClient
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInterestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInterestBinding

    private var allTagsList = listOf<TagResponse>()

    // 사용자가 선택한 관심 태그 Map (id -> TagResponse)
    private val selectedTagsMap = mutableMapOf<Long, TagResponse>()

    private lateinit var selectedTagAdapter: UserInterestAdapter
    private lateinit var availableTagAdapter: UserInterestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInterestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setupRecyclerViews()
        setupListeners()
        fetchInitialData()
    }

    private fun setupRecyclerViews() {
        // 1. 상단 관심 태그 영역 (Flexbox로 자동 줄바꿈)
        val flexboxLayoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }

        selectedTagAdapter = UserInterestAdapter(
            items = emptyList(),
            isSelectedMode = true
        ) { clickedTag ->
            // 상단 태그 누르면 선택 해제
            selectedTagsMap.remove(clickedTag.id)
            refreshSelectedTagsUI()
        }
        binding.rvSelectedTags.apply {
            layoutManager = flexboxLayoutManager
            adapter = selectedTagAdapter
        }

        // 2. 하단 태그 추가 영역 (가로 스크롤)
        availableTagAdapter = UserInterestAdapter(
            items = emptyList(),
            isSelectedMode = false,
            selectedChecker = { tag -> selectedTagsMap.containsKey(tag.id) }
        ) { clickedTag ->
            // 하단 태그 누르면 관심 태그에 추가
            if (selectedTagsMap.containsKey(clickedTag.id)) {
                Toast.makeText(this, "이미 추가된 관심 태그입니다.", Toast.LENGTH_SHORT).show()
                return@UserInterestAdapter
            }
            if (selectedTagsMap.size >= 30) {
                Toast.makeText(this, "관심 태그는 최대 30개까지 등록할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@UserInterestAdapter
            }
            selectedTagsMap[clickedTag.id] = clickedTag
            refreshSelectedTagsUI()
        }
        binding.rvAvailableTags.apply {
            layoutManager = LinearLayoutManager(this@UserInterestActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = availableTagAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.etTagSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAvailableTags(s.toString())
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnSave.setOnClickListener {
            saveUserInterests()
        }
    }

    private fun fetchInitialData() {
        // 내 관심 태그 조회
        RetrofitClient.service.getInterests().enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (response.isSuccessful) {
                    val myInterests = response.body() ?: emptyList()
                    selectedTagsMap.clear()
                    myInterests.forEach { selectedTagsMap[it.id] = it }
                    refreshSelectedTagsUI()
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {}
        })

        // 전체 태그 조회 (충돌 방지된 getTagsAll 사용)
        RetrofitClient.service.getTagsAll().enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (response.isSuccessful) {
                    allTagsList = response.body() ?: emptyList()
                    availableTagAdapter.updateItems(allTagsList)
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {}
        })
    }

    private fun refreshSelectedTagsUI() {
        selectedTagAdapter.updateItems(selectedTagsMap.values.toList())
        availableTagAdapter.notifyDataSetChanged() // 하단 태그들의 선택 상태(컬러 등) 갱신
        binding.tvTagCount.text = "${selectedTagsMap.size} / 30"
    }

    private fun filterAvailableTags(query: String) {
        val filtered = if (query.isBlank()) {
            allTagsList
        } else {
            allTagsList.filter { it.name.contains(query, ignoreCase = true) }
        }
        availableTagAdapter.updateItems(filtered)
    }

    private fun saveUserInterests() {
        val request = UserInterestUpdateRequest(tagIds = selectedTagsMap.keys.toList())
        RetrofitClient.service.replaceInterests(request).enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@UserInterestActivity, "관심 태그가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@UserInterestActivity, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {
                Toast.makeText(this@UserInterestActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}