package com.example.snutiexp.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.ActivityLectureDetailBinding
import com.example.snutiexp.model.LectureDetailResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LectureDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLectureDetailBinding
    private lateinit var detailAdapter: LectureDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLectureDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 뒤로가기 버튼 기능 구현
        binding.btnBack.setOnClickListener {
            finish() // 현재 액티비티를 종료하고 이전 화면(메인)으로 돌아감
        }

        // 2. 하단 동적 섹션(텍스트/이미지)을 위한 리사이클러뷰 설정
        detailAdapter = LectureDetailAdapter(emptyList())
        binding.rvDetailContents.apply {
            adapter = detailAdapter
            layoutManager = LinearLayoutManager(this@LectureDetailActivity)
            isNestedScrollingEnabled = false // 스크롤 충돌 방지
        }

        // 3. 메인 리스트(LectureAdapter)에서 던져준 강좌 ID 받아오기
        val lectureId = intent.getLongExtra("LECTURE_ID", -1)

        if (lectureId != -1L) {
            // 정상적인 ID가 넘어왔다면 서버에 상세 정보 데이터 요청
            fetchLectureDetail(lectureId)
        } else {
            Toast.makeText(this, "올바르지 않은 강좌입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 서버로부터 강좌 상세 데이터를 받아와서 뷰에 바인딩하는 함수
     */
    private fun fetchLectureDetail(id: Long) {
        RetrofitClient.service.getLectureDetail(id).enqueue(object : Callback<LectureDetailResponse> {
            override fun onResponse(
                call: Call<LectureDetailResponse>,
                response: Response<LectureDetailResponse>
            ) {
                if (response.isSuccessful) {
                    val lectureData = response.body() ?: return

                    // 상단 기본 정보 UI 갱신
                    binding.tvDetailTitle.text = lectureData.title
                    binding.tvDetailLecturer.text = lectureData.lecturerName
                    binding.tvDetailDate.text = lectureData.lectureDate
                    binding.tvDetailTopic.text = lectureData.topic
                    binding.tvDetailLocation.text = lectureData.location
                    binding.tvDetailSummary.text = lectureData.lectureSummary

                    // 영상 링크 처리 (없으면 "-" 표시)
                    binding.tvDetailVideo.text = lectureData.videoUrl ?: "-"

                    // 하단 동적 섹션(articles) 리스트를 어댑터에 넘겨서 그리기
                    detailAdapter.updateList(lectureData.articles)
                } else {
                    Toast.makeText(this@LectureDetailActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureDetailResponse>, t: Throwable) {
                Toast.makeText(this@LectureDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}