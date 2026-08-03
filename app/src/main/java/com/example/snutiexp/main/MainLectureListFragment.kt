package com.example.snutiexp.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentMainListBinding
import com.example.snutiexp.model.LectureListResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.snutiexp.model.LectureListItemResponse

class MainLectureListFragment : Fragment() {
    private var _binding: FragmentMainListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lectureAdapter: LectureAdapter

    private var allLectures =
        mutableListOf<LectureListItemResponse>()

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

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadLectures() // 다시 데이터 호출
        }
    }

    override fun onResume() {
        super.onResume()
        loadLectures()
    }

    private fun setupRecyclerView() {
        lectureAdapter = LectureAdapter(emptyList()) { clickedItem, clickedPosition ->
            if (clickedPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                val context = requireContext()
                val intent = android.content.Intent(context, LectureDetailActivity::class.java).apply {
                    putExtra("LECTURE_ID", clickedItem.id)
                }
                context.startActivity(intent)
            }
        }
        binding.rvLectures.apply {
            adapter = lectureAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadLectures() {
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = true
        }

        RetrofitClient.service.getLectures(page = 0, size = 20, sort = "id,desc")
            .enqueue(object : Callback<LectureListResponse> {
                override fun onResponse(call: Call<LectureListResponse>, response: Response<LectureListResponse>) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (response.isSuccessful) {
                        val newItems = response.body()?.content ?: emptyList()
                        allLectures.clear()
                        allLectures.addAll(newItems)

                        if (newItems.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.rvLectures.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvLectures.visibility = View.VISIBLE
                            lectureAdapter.updateList(newItems)
                        }
                    } else {
                        Log.e("API_ERROR", "Status Code: ${response.code()}")
                        Log.e("API_ERROR", "Error Body: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "강좌 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }
    fun searchLecture(keyword: String) {
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

        lectureAdapter.updateList(filteredList)

        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvLectures.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvLectures.visibility = View.VISIBLE
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}