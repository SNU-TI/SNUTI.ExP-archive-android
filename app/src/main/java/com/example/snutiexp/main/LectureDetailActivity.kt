package com.example.snutiexp.main

import android.os.Bundle
import android.util.Log
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

        // 뒤로가기 버튼 기능 구현
        binding.btnBack.setOnClickListener {
            finish() // 현재 액티비티를 종료하고 이전 화면(메인)으로 돌아감
        }

        // 관리자 계정일 때 강의 수정 버튼 가시화
        val isAdminUser = checkUserAdminStatus()
        if (isAdminUser) {
            binding.btnEditLecture.visibility = View.VISIBLE
        } else {
            binding.btnEditLecture.visibility = View.GONE
        }

        // 강의 수정 버튼 클릭 리스너 설정 (추후 가공할 액티비티 연결 등 확장 구역)
        binding.btnEditLecture.setOnClickListener {
            Toast.makeText(this, "강의 수정 모드로 진입합니다.", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, EditCourseActivity::class.java)
            // intent.putExtra("LECTURE_ID", intent.getLongExtra("LECTURE_ID", -1))
            // startActivity(intent)
        }

        // 하단 동적 섹션(텍스트/이미지)을 위한 리사이클러뷰 설정
        detailAdapter = LectureDetailAdapter(emptyList())
        binding.rvDetailContents.apply {
            adapter = detailAdapter
            layoutManager = LinearLayoutManager(this@LectureDetailActivity)
            isNestedScrollingEnabled = false // 스크롤 충돌 방지

            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: android.view.View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    // 모든 아이템의 하단에 간격 추가
                    outRect.bottom = 40 // 이 값을 조절해서 간격을 더 넓히거나 좁힐 수 있습니다.
                }
            })
        }

        val lectureId = intent.getLongExtra("LECTURE_ID", -1)
        if (lectureId != -1L) {
            fetchLectureDetail(lectureId)
        } else {
            Toast.makeText(this, "올바르지 않은 강좌입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // SharedPreferences에 저장된 관리자 플래그를 불러오는 헬퍼 함수
    private fun checkUserAdminStatus(): Boolean {
        // MainActivity, LoginActivity와 동일한 이름의 토큰 prefs 보관함을 참조합니다.
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
        // 로그인 시 기포팅해 둔 관리자 플래그를 꺼내옵니다. (기본값 false)
        return sharedPref.getBoolean("is_admin_user", false)
    }

    //서버로부터 강좌 상세 데이터를 받아와서 뷰에 바인딩하는 함수
    private fun fetchLectureDetail(id: Long) {
        RetrofitClient.service.getLectureDetail(id).enqueue(object : Callback<LectureDetailResponse> {
            override fun onResponse(
                call: Call<LectureDetailResponse>,
                response: Response<LectureDetailResponse>
            ) {
                if (response.isSuccessful) {
                    val lectureData = response.body() ?: return

                    // 상단 기본 정보 UI 갱신
                    binding.tvDetailTitle.text = lectureData.title.ifEmptyDash()
                    binding.tvDetailLecturer.text = lectureData.lecturerName.ifEmptyDash()

                    val rawDate = lectureData.lectureDate
                    // 기본값이면 "-"이 뜨도록 설정
                    val isDefaultDate = rawDate.contains("-01-01T00:00:00")

                    binding.tvDetailDate.text = if (rawDate.isBlank() || isDefaultDate) {
                        "-"
                    } else if (rawDate.contains("T")) {
                        // ISO 형식 날짜 포맷팅 (T 제거 및 초 단위 생략)
                        rawDate.replace("T", " ").substringBeforeLast(":")
                    } else {
                        rawDate
                    }

                    binding.tvDetailTopic.text = lectureData.topic.ifEmptyDash()
                    binding.tvDetailLocation.text = lectureData.location.ifEmptyDash()
                    binding.tvDetailSummary.text = lectureData.lectureSummary.ifEmptyDash()

                    // 영상 링크 처리 (없으면 "-" 표시)
                    binding.tvDetailVideo.text = lectureData.videos.firstOrNull()?.videoUrl ?: "-"

                    // 아티클 처리
                    val allBlocks = lectureData.articles.flatMap { it.blocks }.sortedBy { it.orderIndex }
                    Log.d("DETAIL_ARTICLES_DEBUG", "수신 및 병합 완료된 블록(섹션) 총 개수: ${allBlocks.size}")

                    // 아티클과 영상을 합칠 리스트 생성
                    val combinedList = mutableListOf<Any>()

                    // 영상 먼저 추가 (서버에서 받은 비디오 리스트가 있다면)
                    if (!lectureData.videos.isNullOrEmpty()) {
                        combinedList.addAll(lectureData.videos)
                    }

                    // 아티클 추가
                    combinedList.addAll(allBlocks)

                    // 추출 및 정렬이 끝난 실제 알맹이 데이터를 어댑터에 주입하여 즉시 부활시킵니다.
                    detailAdapter.updateList(combinedList)

                    binding.rvDetailContents.post {
                        binding.rvDetailContents.requestLayout()
                    }
                } else {
                    Log.e("DETAIL_DEBUG", "서버 응답 에러 코드: ${response.code()}")
                    Toast.makeText(this@LectureDetailActivity, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureDetailResponse>, t: Throwable) {
                Log.e("DETAIL_DEBUG", "통신 실패 에러 원인: ${t.message}")
                Toast.makeText(this@LectureDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun String?.ifEmptyDash(): String {
        return if (this.isNullOrBlank()) "-" else this
    }
}