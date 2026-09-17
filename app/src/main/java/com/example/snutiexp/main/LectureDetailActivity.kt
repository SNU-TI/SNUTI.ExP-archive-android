package com.example.snutiexp.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var tagAdapter: TagHorizontalAdapter

    private var lectureId: Long = -1L

    // 수정 화면에서 돌아왔을 때 강의 상세 정보 새로고침
    private val editLectureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (lectureId != -1L) {
            fetchLectureDetail(lectureId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLectureDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 이전 화면에서 강의 ID 받기
        lectureId = intent.getLongExtra("LECTURE_ID", -1L)

        if (lectureId == -1L) {
            Toast.makeText(
                this,
                "올바르지 않은 강좌입니다.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // 2. 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 3. 관리자 여부 확인
        val isAdminUser = checkUserAdminStatus()

        binding.btnEditLecture.visibility =
            if (isAdminUser) View.VISIBLE else View.GONE

        // 4. 수정 버튼
        binding.btnEditLecture.setOnClickListener {
            val editIntent = Intent(
                this,
                AddCourseActivity::class.java
            ).apply {
                putExtra("MODE", "EDIT")
                putExtra("LECTURE_ID", lectureId)
            }

            editLectureLauncher.launch(editIntent)
        }

        // 5. 상세 콘텐츠 RecyclerView
        detailAdapter = LectureDetailAdapter(emptyList())

        binding.rvDetailContents.apply {
            adapter = detailAdapter

            layoutManager = LinearLayoutManager(
                this@LectureDetailActivity
            )

            isNestedScrollingEnabled = false

            addItemDecoration(
                object :
                    androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

                    override fun getItemOffsets(
                        outRect: android.graphics.Rect,
                        view: View,
                        parent: androidx.recyclerview.widget.RecyclerView,
                        state: androidx.recyclerview.widget.RecyclerView.State
                    ) {
                        outRect.bottom = 40
                    }
                }
            )
        }

        // 6. 태그 RecyclerView
        tagAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = true,
            onItemClick = {
                // 상세 화면에서는 태그 클릭 기능 없음
            }
        )

        binding.rvDetailTags.apply {
            adapter = tagAdapter

            layoutManager = LinearLayoutManager(
                this@LectureDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )

            // NestedScrollView 안에 있으므로 세로 스크롤 충돌 방지
            isNestedScrollingEnabled = false
        }

        // 7. 서버에서 강의 상세 조회
        fetchLectureDetail(lectureId)
    }

    /**
     * SharedPreferences에 저장된 관리자 여부 확인
     */
    private fun checkUserAdminStatus(): Boolean {
        val sharedPref = getSharedPreferences(
            "token_prefs",
            MODE_PRIVATE
        )

        return sharedPref.getBoolean(
            "is_admin_user",
            false
        )
    }

    /**
     * 서버에서 강좌 상세 정보를 받아와 UI에 표시
     */
    private fun fetchLectureDetail(id: Long) {

        RetrofitClient.service
            .getLectureDetail(id)
            .enqueue(
                object : Callback<LectureDetailResponse> {

                    override fun onResponse(
                        call: Call<LectureDetailResponse>,
                        response: Response<LectureDetailResponse>
                    ) {

                        if (response.isSuccessful) {

                            val lectureData =
                                response.body() ?: return

                            // -------------------------
                            // 기본 정보
                            // -------------------------

                            binding.tvDetailTitle.text =
                                lectureData.title.ifEmptyDash()

                            binding.tvDetailLecturer.text =
                                lectureData.lecturerName.ifEmptyDash()

                            // 날짜
                            val rawDate = lectureData.lectureDate

                            val isDefaultDate =
                                rawDate.contains("-01-01T00:00:00")

                            binding.tvDetailDate.text =
                                if (
                                    rawDate.isBlank() ||
                                    isDefaultDate
                                ) {
                                    "-"
                                } else if (
                                    rawDate.contains("T")
                                ) {
                                    rawDate
                                        .replace("T", " ")
                                        .substringBeforeLast(":")
                                } else {
                                    rawDate
                                }

                            binding.tvDetailTopic.text =
                                lectureData.topic.ifEmptyDash()

                            binding.tvDetailLocation.text =
                                lectureData.location.ifEmptyDash()

                            binding.tvDetailSummary.text =
                                lectureData.lectureSummary.ifEmptyDash()

                            // -------------------------
                            // 영상 링크
                            // -------------------------

                            binding.tvDetailVideo.text =
                                lectureData.videos
                                    .firstOrNull()
                                    ?.videoUrl
                                    ?: "-"

                            // -------------------------
                            // 태그
                            // -------------------------

                            val tagNames =
                                lectureData.tags.map { tag ->
                                    tag.name
                                }

                            if (tagNames.isEmpty()) {

                                // 태그가 하나도 없으면 행 자체 숨김
                                binding.rvDetailTags.visibility =
                                    View.GONE

                            } else {

                                binding.rvDetailTags.visibility =
                                    View.VISIBLE

                                tagAdapter.updateItems(
                                    tagNames
                                )
                            }

                            // -------------------------
                            // Article blocks
                            // -------------------------

                            val allBlocks =
                                lectureData.articles
                                    .flatMap {
                                        it.blocks
                                    }
                                    .sortedBy {
                                        it.orderIndex
                                    }

                            Log.d(
                                "DETAIL_ARTICLES_DEBUG",
                                "수신 및 병합 완료된 블록(섹션) 총 개수: ${allBlocks.size}"
                            )

                            // -------------------------
                            // 영상 + Article block 병합
                            // -------------------------

                            val combinedList =
                                mutableListOf<Any>()

                            if (
                                lectureData.videos.isNotEmpty()
                            ) {
                                combinedList.addAll(
                                    lectureData.videos
                                )
                            }

                            combinedList.addAll(
                                allBlocks
                            )

                            detailAdapter.updateList(
                                combinedList
                            )

                            binding.rvDetailContents.post {
                                binding
                                    .rvDetailContents
                                    .requestLayout()
                            }

                        } else {

                            Log.e(
                                "DETAIL_DEBUG",
                                "서버 응답 에러 코드: ${response.code()}"
                            )

                            Toast.makeText(
                                this@LectureDetailActivity,
                                "데이터를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<LectureDetailResponse>,
                        t: Throwable
                    ) {

                        Log.e(
                            "DETAIL_DEBUG",
                            "통신 실패 에러 원인: ${t.message}"
                        )

                        Toast.makeText(
                            this@LectureDetailActivity,
                            "네트워크 오류가 발생했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }

    /**
     * null 또는 빈 문자열이면 "-" 반환
     */
    private fun String?.ifEmptyDash(): String {
        return if (this.isNullOrBlank()) {
            "-"
        } else {
            this
        }
    }
}