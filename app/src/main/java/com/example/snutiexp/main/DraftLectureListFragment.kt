package com.example.snutiexp.main

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class DraftLectureListFragment : Fragment() {

    private var _binding: FragmentMainListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lectureAdapter: LectureAdapter
    
    // 자동 새로고침
    private val courseActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("DRAFT_DEBUG", "수정/생성 완료 신호 감지: 목록을 자동으로 새로고침합니다.")
            loadDraftLecturesFromServer()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        loadDraftLecturesFromServer()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadDraftLecturesFromServer() // 다시 데이터 호출
        }
    }

    private fun setupRecyclerView() {
        lectureAdapter = LectureAdapter(emptyList()) { clickedItem, _ ->
            val intent = android.content.Intent(requireContext(), AddCourseActivity::class.java).apply {
                putExtra("IS_EDIT", true)
                putExtra("LECTURE_ID", clickedItem.id)
            }
            courseActivityResultLauncher.launch(intent)
        }

        binding.rvLectures.apply {
            adapter = lectureAdapter
            layoutManager = LinearLayoutManager(requireContext())
            clipToPadding = false
        }
    }

    // 기존 페이징 응답 객체를 수신하여 드래프트 리스트 뷰를 갱신하는 비동기 네트워크 코어
    private fun loadDraftLecturesFromServer() {
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = true
        }
        RetrofitClient.service.getDraftLectures().enqueue(object : Callback<LectureListResponse> {
            override fun onResponse(
                call: Call<LectureListResponse>,
                response: Response<LectureListResponse>
            ) {
                binding.swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    // 기존 모델의 content 리스트를 그대로 긁어와 매핑을 수행
                    val draftList = response.body()?.content ?: emptyList()

                    Log.d("DRAFT_DEBUG", "서버 응답 성공: 드래프트 강좌 개수 -> ${draftList.size}")

                    if (draftList.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvLectures.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvLectures.visibility = View.VISIBLE
                        lectureAdapter.updateList(draftList)

                        Toast.makeText(
                            requireContext(),
                            "성공적으로 ${draftList.size}개의 드래프트 강좌를 가져왔습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("DRAFT_DEBUG", "HTTP 실패 코드 수신: ${response.code()}")
                    Toast.makeText(requireContext(), "드래프트 목록을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                binding.swipeRefreshLayout.isRefreshing = false
                Log.e("DRAFT_DEBUG", "네트워크 에러: ${t.message}")
                Toast.makeText(requireContext(), "네트워크 연결 상태를 확인해 주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}