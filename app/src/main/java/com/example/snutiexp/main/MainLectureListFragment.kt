package com.example.snutiexp.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentMainListBinding
import com.example.snutiexp.model.LectureListItem
import com.example.snutiexp.model.LectureListResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainLectureListFragment : Fragment() {
    private var _binding: FragmentMainListBinding? = null
    private val binding get() = _binding!!

    private lateinit var lectureAdapter: LectureAdapter

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
    }

    override fun onResume() {
        super.onResume()
        loadLectures()
    }

    private fun setupRecyclerView() {
        lectureAdapter = LectureAdapter(emptyList())
        binding.rvLectures.apply {
            adapter = lectureAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadLectures() {
        RetrofitClient.service.getLectures(page = 0, size = 20, sort = "id,desc")
            .enqueue(object : Callback<LectureListResponse> {
            override fun onResponse(call: Call<LectureListResponse>, response: Response<LectureListResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val newItems = responseBody?.content ?: emptyList()

                    // [진단용 로그] 서버가 실제로 몇 개를 보내주는지 로그캣에서 확인하세요.
                    Log.d("API_TEST", "전체 응답 바디: $responseBody")
                    Log.d("API_TEST", "강좌 개수(newItems.size): ${newItems.size}")
                    Log.d("API_TEST", "전체 강좌 수(totalElements): ${responseBody?.totalElements}")

                    // 어댑터의 리스트를 갱신합니다. (회색 밑줄이 사라집니다)
                    lectureAdapter.updateList(newItems)
                } else {
                    Log.e("API_ERROR", "Status Code: ${response.code()}")
                    Log.e("API_ERROR", "Error Body: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "강좌 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}