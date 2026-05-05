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

    private val lectureList = mutableListOf<LectureListItem>()
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
        lectureAdapter = LectureAdapter(lectureList)
        binding.rvLectures.apply {
            adapter = lectureAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadLectures() {
        RetrofitClient.service.getLectures().enqueue(object : Callback<LectureListResponse> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<LectureListResponse>, response: Response<LectureListResponse>) {
                if (response.isSuccessful) {
                    val newItems = response.body()?.content ?: return
                    lectureList.clear()
                    lectureList.addAll(newItems)
                    lectureAdapter.notifyDataSetChanged()
                } else {
                    Log.e("API_ERROR", "Status Code: ${response.code()}")
                    Log.e("API_ERROR", "Error Body: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "강좌 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureListResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}