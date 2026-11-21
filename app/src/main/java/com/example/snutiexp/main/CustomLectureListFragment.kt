package com.example.snutiexp.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.app.AlertDialog
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.R
import com.example.snutiexp.databinding.FragmentCustomLectureListBinding
import com.example.snutiexp.model.LectureListItemResponse
import com.example.snutiexp.model.LectureListResponse
import com.example.snutiexp.model.TagResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomLectureListFragment : Fragment(R.layout.fragment_custom_lecture_list) {

    private var _binding: FragmentCustomLectureListBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagPreviewAdapter: UserInterestAdapter
    private lateinit var lectureAdapter: LectureAdapter // 기존에 존재하는 강좌 어댑터

    private var allCustomLectures = listOf<LectureListItemResponse>()
    private var currentQuery: String = "" // 현재 검색어 기억용
    private var selectedTagId: Long? = null // 현재 선택된 필터 태그 ID (단일 선택)

    // 진행 중인 네트워크 요청을 추적할 Call 객체 변수 선언
    private var interestsCall: Call<List<TagResponse>>? = null
    private var lecturesCall: Call<LectureListResponse>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCustomLectureListBinding.bind(view)

        setupRecyclerViews()
        setupListeners()

        // 새로고침 리스너 추가
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchCustomData()
        }
    }

    override fun onResume() {
        super.onResume()
        // 관심 태그 설정 화면에서 돌아올 때마다 데이터 자동 갱신
        fetchCustomData()
    }

    private fun setupRecyclerViews() {
        // 1. 상단 태그 선택 박스 내부 미리보기 어댑터 (가로 스크롤)
        tagPreviewAdapter = UserInterestAdapter(
            items = emptyList(),
            isSelectedMode = true
        ) { clickedTag ->
            // 이미 선택된 태그를 다시 누르면 선택 해제 (null), 아니면 해당 태그 ID로 설정
            if (selectedTagId == clickedTag.id) {
                selectedTagId = null
            } else {
                selectedTagId = clickedTag.id
            }

            // 어댑터에 현재 선택된 태그 ID 전달하여 UI(검은색 칠하기 등) 갱신
            tagPreviewAdapter.setSelectedTagId(selectedTagId)

            // 강좌 목록 다시 필터링 적용
            applyFilters()
        }
        binding.rvSelectedTagsPreview.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = tagPreviewAdapter
        }

        // 2. 맞춤 강좌 목록 어댑터 (클릭 및 롱클릭 리스너 추가)
        lectureAdapter = LectureAdapter(
            items = emptyList(),
            onItemClick = { clickedItem, clickedPosition ->
                if (clickedPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    val context = requireContext()
                    val intent = android.content.Intent(context, LectureDetailActivity::class.java).apply {
                        putExtra("LECTURE_ID", clickedItem.id)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { clickedItem, _ ->
                // 1. 길게 누른 강좌가 이미 저장된 상태인지 서버에 먼저 확인
                RetrofitClient.service.isFavorite(clickedItem.id).enqueue(object : Callback<Boolean> {
                    override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                        if (!isAdded || _binding == null) return

                        if (response.isSuccessful) {
                            val isAlreadySaved = response.body() ?: false

                            if (isAlreadySaved) {
                                // 2-A. 이미 저장된 강좌라면 팝업 없이 곧바로 토스트 메시지만 출력
                                Toast.makeText(requireContext(), "이미 저장된 강좌입니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                // 2-B. 아직 저장되지 않은 강좌라면 저장 확인 팝업(다이얼로그) 띄우기
                                AlertDialog.Builder(requireContext())
                                    .setTitle("강좌 저장")
                                    .setMessage("강좌를 저장하시겠습니까?")
                                    .setPositiveButton("저장") { _, _ ->
                                        // 사용자가 '저장'을 누르면 실제 즐겨찾기 추가 API 호출
                                        RetrofitClient.service.addFavorite(clickedItem.id).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (!isAdded || _binding == null) return

                                                if (response.isSuccessful) {
                                                    Toast.makeText(requireContext(), "강좌가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else if (response.code() != 401) {
                                                    Toast.makeText(requireContext(), "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                                if (!isAdded || _binding == null) return

                                                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    }
                                    .setNegativeButton("취소", null)
                                    .show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "저장 여부를 확인하지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Boolean>, t: Throwable) {
                        if (!isAdded || _binding == null) return

                        Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        )
        binding.rvCustomLectures.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lectureAdapter
        }
    }

    private fun setupListeners() {
        // "태그 선택" 박스 클릭 시 UserInterestActivity로 이동
        binding.cardTagSelector.setOnClickListener {
            openInterestSetting()
        }
    }

    private fun openInterestSetting() {
        val intent = Intent(requireContext(), UserInterestActivity::class.java)
        startActivity(intent)
    }

    private fun fetchCustomData() {
        // 로딩 시작 시 SwipeRefreshLayout 스피너 표시
        if (_binding != null && !binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = true
        }

        // 새로운 요청 전 이전 관심 태그 조회 요청 취소
        interestsCall?.cancel()
        interestsCall = RetrofitClient.service.getInterests()
        interestsCall?.enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                // 응답 시점에 뷰가 살아있는지 검사
                if (!isAdded || _binding == null) return

                if (response.isSuccessful) {
                    val myTags = response.body() ?: emptyList()
                    // 만약 기존에 선택했던 태그가 삭제되었거나 했다면 선택 해제
                    if (selectedTagId != null && myTags.none { it.id == selectedTagId }) {
                        selectedTagId = null
                    }

                    tagPreviewAdapter.updateItems(myTags)
                    tagPreviewAdapter.setSelectedTagId(selectedTagId)

                    fetchCustomLectures(myTags)
                } else {
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (response.code() != 401) {
                        Toast.makeText(requireContext(), "관심 태그를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {
                if (call.isCanceled) return
                if (!isAdded || _binding == null) return
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun fetchCustomLectures(myTags: List<TagResponse>) {
        val tagIds = myTags.map { it.id }.toSet()

        // 새로운 요청 전 이전 강좌 조회 요청 취소
        lecturesCall?.cancel()
        lecturesCall = RetrofitClient.service.getLectures()
        // 전체 강좌를 조회한 뒤, 사용자의 관심 태그 ID가 하나라도 포함된 강좌만 필터링
        lecturesCall?.enqueue(object : Callback<LectureListResponse> {
            override fun onResponse(call: Call<LectureListResponse>, response: Response<LectureListResponse>) {
                if (!isAdded || _binding == null) return

                // 데이터 로드 완료 시 로딩 스피너 해제 (Main과 동일)
                binding.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val allLectures = response.body()?.content ?: emptyList()

                    allCustomLectures = if (tagIds.isEmpty()) {
                        emptyList() // 관심 태그가 없으면 빈 목록
                    } else {
                        allLectures.filter { lecture ->
                            lecture.tags?.any { tag -> tagIds.contains(tag.id) } == true
                        }
                    }

                    // 필터 적용 및 목록 갱신
                    applyFilters()
                } else if (response.code() != 401) {
                    Toast.makeText(requireContext(), "강좌 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                if (call.isCanceled) return
                if (!isAdded || _binding == null) return

                // 네트워크 실패 시 로딩 스피너 해제
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    // 검색어와 개별 태그 선택 필터를 동시에 고려하여 강좌 목록을 갱신하는 공통 함수
    private fun applyFilters() {
        if (_binding == null) return

        var filtered = allCustomLectures

        // 1. 개별 태그 필터 적용 (선택된 태그가 있다면 해당 태그를 포함한 강좌만)
        if (selectedTagId != null) {
            filtered = filtered.filter { lecture ->
                lecture.tags?.any { it.id == selectedTagId } == true
            }
        }

        // 2. 검색어 필터 적용
        val trimmed = currentQuery.trim()
        if (trimmed.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(trimmed, ignoreCase = true) }
        }

        if (_binding == null) return
        lectureAdapter.updateList(filtered)

        // 빈 화면 제어
        if (_binding == null) return
        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvCustomLectures.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvCustomLectures.visibility = View.VISIBLE
        }
    }

    // MainActivity 등에서 검색어 입력 시 호출
    fun filterCustomLecturesByQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 화면이 파괴될 때 진행 중인 네트워크 요청을 안전하게 모두 취소
        interestsCall?.cancel()
        lecturesCall?.cancel()
        _binding = null
    }
}