package com.example.snutiexp.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentMainListBinding
import com.example.snutiexp.model.LectureListResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.snutiexp.model.LectureListItemResponse
import com.example.snutiexp.model.TagResponse
import android.content.Context

class MainLectureListFragment : Fragment() {
    private var _binding: FragmentMainListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lectureAdapter: LectureAdapter

    private var allLectures = mutableListOf<LectureListItemResponse>()

    // 서버에서 가져온 전체 태그 목록 및 현재 선택된 태그 ID 저장 변수
    private val allTags = mutableListOf<TagResponse>()
    private var currentTagId: Long? = null

    // 현재 사용자가 입력한 검색어 상태를 기억할 변수 추가
    private var currentSearchKeyword: String = ""

    // 진행 중인 네트워크 요청을 추적할 Call 객체 변수 선언
    private var lectureCall: Call<LectureListResponse>? = null
    private var tagCall: Call<List<TagResponse>>? = null

    // 상세 화면에서 돌아올 때 결과를 처리할 런처 등록
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 상세 화면(LectureDetailActivity)이나 수정 화면에서 돌아왔을 때 데이터 갱신
        loadLectures()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        fetchServerTags()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadLectures() // 다시 데이터 호출
        }
    }

    override fun onResume() {
        super.onResume()
        loadLectures()
    }

    private fun setupRecyclerView() {
        val sharedPref = requireContext()
            .getSharedPreferences("token_prefs", Context.MODE_PRIVATE)

        val isAdminUser = sharedPref.getBoolean("is_admin_user", false)

        lectureAdapter = LectureAdapter(
            items = emptyList(),
            onItemClick = { clickedItem, clickedPosition ->
                if (clickedPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    val context = requireContext()
                    val intent = android.content.Intent(context, LectureDetailActivity::class.java).apply {
                        putExtra("LECTURE_ID", clickedItem.id)
                    }
                    detailLauncher.launch(intent)
                }
            },
            onItemLongClick = { clickedItem, _ ->
                // 1. 길게 누른 강좌가 이미 저장된 상태인지 서버에 먼저 확인
                if (isAdminUser) {
                    // 관리자: 강좌 삭제 확인 다이얼로그
                    AlertDialog.Builder(requireContext())
                        .setTitle("강좌 삭제")
                        .setMessage("'${clickedItem.title}' 강좌를 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            deleteLecture(clickedItem.id)
                        }
                        .setNegativeButton("취소", null)
                        .show()

                } else {
                    RetrofitClient.service.isFavorite(clickedItem.id)
                        .enqueue(object : Callback<Boolean> {
                            override fun onResponse(
                                call: Call<Boolean>,
                                response: Response<Boolean>
                            ) {
                                if (!isAdded || _binding == null) return

                                if (response.isSuccessful) {
                                    val isAlreadySaved = response.body() ?: false

                                    if (isAlreadySaved) {
                                        // 2-A. 이미 저장된 강좌라면 팝업 없이 곧바로 토스트 메시지만 출력
                                        Toast.makeText(
                                            requireContext(),
                                            "이미 저장된 강좌입니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // 2-B. 아직 저장되지 않은 강좌라면 저장 확인 팝업(다이얼로그) 띄우기
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("강좌 저장")
                                            .setMessage("강좌를 저장하시겠습니까?")
                                            .setPositiveButton("저장") { _, _ ->
                                                // 사용자가 '저장'을 누르면 실제 즐겨찾기 추가 API 호출
                                                RetrofitClient.service.addFavorite(clickedItem.id)
                                                    .enqueue(object : Callback<Void> {
                                                        override fun onResponse(
                                                            call: Call<Void>,
                                                            response: Response<Void>
                                                        ) {
                                                            if (!isAdded || _binding == null) return

                                                            if (response.isSuccessful) {
                                                                Toast.makeText(
                                                                    requireContext(),
                                                                    "강좌가 저장되었습니다.",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                Toast.makeText(
                                                                    requireContext(),
                                                                    "저장에 실패했습니다.",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }

                                                        override fun onFailure(
                                                            call: Call<Void>,
                                                            t: Throwable
                                                        ) {
                                                            if (!isAdded || _binding == null) return
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "네트워크 오류가 발생했습니다.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    })
                                            }
                                            .setNegativeButton("취소", null)
                                            .show()
                                    }
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "저장 여부를 확인하지 못했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                                if (!isAdded || _binding == null) return
                                Toast.makeText(
                                    requireContext(),
                                    "네트워크 오류가 발생했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }
        )
        binding.rvLectures.apply {
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

    fun filterByTags(tagNames: List<String>) {
        currentTagId = if (tagNames.isEmpty()) {
            null
        } else {
            // 선택된 태그 이름과 일치하는 서버 태그의 ID를 매핑
            allTags.find { it.name == tagNames.first() }?.id
        }
        loadLectures()
    }

    fun filterByTag(tagId: Long?) {
        currentTagId = tagId
        loadLectures()
    }

    private fun loadLectures() {
        // 새로 요청을 보내기 전, 이전에 진행 중이던 강좌 요청이 있다면 즉시 취소
        lectureCall?.cancel()

        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = true
        }

        lectureCall = RetrofitClient.service.getLectures(tagId = currentTagId, page = 0, size = 20, sort = "id,desc")
        lectureCall?.enqueue(object : Callback<LectureListResponse> {
                override fun onResponse(call: Call<LectureListResponse>, response: Response<LectureListResponse>) {
                    // 응답 시점에 프래그먼트가 살아있는지 검사
                    if (!isAdded || _binding == null) return

                    binding.swipeRefreshLayout.isRefreshing = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        val newItems = body?.content ?: emptyList()
                        allLectures.clear()
                        allLectures.addAll(newItems)

                        if (currentSearchKeyword.isNotBlank()) {
                            searchLecture(currentSearchKeyword)
                        } else {
                            if (newItems.isEmpty()) {
                                binding.layoutEmpty.visibility = View.VISIBLE
                                binding.rvLectures.visibility = View.GONE
                            } else {
                                binding.layoutEmpty.visibility = View.GONE
                                binding.rvLectures.visibility = View.VISIBLE
                                lectureAdapter.updateList(newItems)
                            }
                        }
                    } else {
                        Log.e("API_ERROR", "Status Code: ${response.code()}")
                        Log.e("API_ERROR", "Error Body: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "강좌 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
    fun searchLecture(keyword: String) {
        if (_binding == null) return

        // 검색할 때마다 키워드를 전역 변수에 저장
        currentSearchKeyword = keyword
        val trimmedKeyword = keyword.trim()

        val filteredList =
            if (trimmedKeyword.isBlank()) {
                allLectures
            } else {
                allLectures.filter { lecture ->
                    lecture.title.contains(
                        trimmedKeyword,
                        ignoreCase = true
                    )
                }
            }

        if (_binding == null) return
        lectureAdapter.updateList(filteredList)

        if (_binding == null) return
        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvLectures.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvLectures.visibility = View.VISIBLE
        }
    }
    private fun deleteLecture(lectureId: Long) {
        RetrofitClient.service.deleteLecture(lectureId)
            .enqueue(object : Callback<Void> {

                override fun onResponse(
                    call: Call<Void>,
                    response: Response<Void>
                ) {
                    if (!isAdded || _binding == null) return

                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "강좌가 삭제되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 강좌 목록 새로고침
                        loadLectures()

                    } else {
                        Toast.makeText(
                            requireContext(),
                            "강좌 삭제에 실패했습니다. (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.e(
                            "DELETE_LECTURE",
                            "삭제 실패: ${response.code()}"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<Void>,
                    t: Throwable
                ) {
                    if (!isAdded || _binding == null) return

                    Toast.makeText(
                        requireContext(),
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.e("DELETE_LECTURE", "삭제 요청 실패", t)
                }
            })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // 화면이 파괴될 때 진행 중인 네트워크 요청을 안전하게 모두 취소
        lectureCall?.cancel()
        tagCall?.cancel()
        _binding = null
    }
}