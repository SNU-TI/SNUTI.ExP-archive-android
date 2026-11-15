package com.example.snutiexp.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentSavedLectureListBinding
import com.example.snutiexp.model.LectureListItemResponse
import com.example.snutiexp.model.LectureListResponse
import com.example.snutiexp.model.TagResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SavedLectureListFragment : Fragment() {

    private var _binding: FragmentSavedLectureListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lectureAdapter: LectureAdapter
    // 서버에서 받아온 원본 저장 목록을 보관하는 변수 추가
    private var originalList = listOf<LectureListItemResponse>()

    // 서버에서 가져온 전체 태그 목록 및 현재 선택된 태그 ID 저장 변수
    private val allTags = mutableListOf<TagResponse>()
    private var currentTagId: Long? = null

    // 현재 사용자가 입력한 검색어 상태를 기억할 변수
    private var currentSearchKeyword: String = ""

    // 진행 중인 네트워크 요청을 추적할 Call 객체 변수 선언
    private var savedLecturesCall: Call<LectureListResponse>? = null
    private var tagCall: Call<List<TagResponse>>? = null

    // 상세 화면에서 돌아왔을 때 데이터를 새로고침하기 위한 런처
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        loadSavedLectures()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedLectureListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        // 서버 태그 목록 로드 (태그 필터링을 위해 필요)
        fetchServerTags()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadSavedLectures()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedLectures()
    }

    private fun setupRecyclerView() {
        // 어댑터 생성 (위치 기반 파라미터 전달로 타입 추론 오류 방지)
        lectureAdapter = LectureAdapter(
            emptyList(),
            { clickedItem, clickedPosition ->
                if (clickedPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    val context = requireContext()
                    val intent = Intent(context, LectureDetailActivity::class.java).apply {
                        putExtra("LECTURE_ID", clickedItem.id)
                    }
                    detailLauncher.launch(intent)
                }
            },
            { clickedItem, _ ->
                // 저장 탭에서 꾹 누르면 저장 해제 여부를 묻는 다이얼로그 표시
                showUnsaveDialog(clickedItem)
            }
        )

        binding.rvSavedLectures.apply {
            adapter = lectureAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun fetchServerTags() {
        tagCall?.cancel()
        tagCall = RetrofitClient.service.getTagsAll()
        tagCall?.enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful && response.body() != null) {
                    allTags.clear()
                    allTags.addAll(response.body()!!)
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {
                if (call.isCanceled) return
                if (!isAdded || _binding == null) return
                Log.e("API_ERROR", "태그 목록 로드 실패: ${t.message}")
            }
        })
    }

    // MainActivity에서 태그를 선택/해제할 때 호출되는 함수
    fun filterByTags(tagNames: List<String>) {
        currentTagId = if (tagNames.isEmpty()) {
            null
        } else {
            allTags.find { it.name == tagNames.first() }?.id
        }
        applyFilters()
    }

    private fun loadSavedLectures() {
        // 새로운 요청 전 이전 요청 취소
        savedLecturesCall?.cancel()

        if (_binding != null && !binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = true
        }

        // 서버의 즐겨찾기 목록 조회 API 호출 (LectureListResponse 타입으로 수신)
        savedLecturesCall = RetrofitClient.service.getFavorites()
        savedLecturesCall?.enqueue(object : Callback<LectureListResponse> {
            override fun onResponse(
                call: Call<LectureListResponse>,
                response: Response<LectureListResponse>
            ) {
                // 응답 시점에 프래그먼트가 살아있는지 검사
                if (!isAdded || _binding == null) return

                binding.swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    val body = response.body()
                    originalList = body?.content ?: emptyList() // 원본 데이터 저장

                    // 데이터 로드 후 현재 적용된 필터(태그 + 검색어) 반영
                    applyFilters()
                } else {
                    Log.e("API_ERROR", "저장된 강좌 로드 실패: ${response.code()}")
                    Toast.makeText(requireContext(), "저장된 강좌를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                if (call.isCanceled) return
                if (!isAdded || _binding == null) return

                binding.swipeRefreshLayout.isRefreshing = false
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 태그 필터와 검색어 필터를 동시에 적용하는 통합 함수
    private fun applyFilters() {
        if (_binding == null) return

        var filtered = originalList

        // 1. 태그 필터 적용
        if (currentTagId != null) {
            filtered = filtered.filter { lecture ->
                lecture.tags?.any { it.id == currentTagId } == true
            }
        }

        // 2. 검색어 필터 적용
        val trimmed = currentSearchKeyword.trim()
        if (trimmed.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                        (it.lecturerName?.contains(trimmed, ignoreCase = true) == true)
            }
        }

        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvSavedLectures.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvSavedLectures.visibility = View.VISIBLE
            lectureAdapter.updateList(filtered)
        }
    }

    // MainActivity의 검색창에서 호출할 검색 필터링 함수
    fun filterSavedLecturesByQuery(query: String) {
        currentSearchKeyword = query
        applyFilters()
    }

    private fun showUnsaveDialog(lecture: LectureListItemResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("저장 해제")
            .setMessage("강좌 저장을 해제하시겠습니까?")
            .setPositiveButton("해제") { _, _ ->
                removeFavoriteLecture(lecture.id)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun removeFavoriteLecture(lectureId: Long) {
        RetrofitClient.service.removeFavorite(lectureId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "강좌 저장이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    loadSavedLectures() // 목록 새로고침
                } else {
                    Toast.makeText(requireContext(), "저장 해제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (!isAdded || _binding == null) return
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 화면이 파괴될 때 진행 중인 네트워크 요청 취소
        savedLecturesCall?.cancel()
        tagCall?.cancel()
        _binding = null
    }
}