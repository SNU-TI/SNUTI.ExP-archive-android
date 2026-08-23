package com.example.snutiexp.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.R
import com.example.snutiexp.databinding.FragmentInfoInputBinding
import com.example.snutiexp.model.LectureCreateRequest
import com.example.snutiexp.model.TagResponse
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class InfoInputFragment : Fragment() {
    private var _binding: FragmentInfoInputBinding? = null
    private val binding get() = _binding!!

    // 서버에서 가져온 전체 추천 태그 목록
    private val allRecommendedTags = mutableListOf<TagResponse>()
    // 사용자가 현재 선택/추가한 태그 이름 목록 (중복 방지 및 순서 유지)
    private val selectedTagNames = mutableSetOf<String>()

    private lateinit var selectedAdapter: TagHorizontalAdapter
    private lateinit var recommendedAdapter: TagHorizontalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 이 프래그먼트가 fragment_info_input.xml 레이아웃을 사용하도록 연결합니다.
        _binding = FragmentInfoInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()

        // 날짜 EditText 클릭 시 달력 띄우기
        binding.etDate.setOnClickListener {
            showDateTimePicker()
        }

        // 태그 입력 영역 토글 버튼 (+ 버튼)
        var isTagInputVisible = false
        binding.btnToggleTagInput.setOnClickListener {
            isTagInputVisible = !isTagInputVisible
            binding.layoutTagInputContainer.visibility = if (isTagInputVisible) View.VISIBLE else View.GONE
            if (isTagInputVisible) {
                // 열렸을 때 (위쪽 화살표 또는 닫기 아이콘)
                binding.btnToggleTagInput.setImageResource(R.drawable.ic_arrow_up) // 또는 ic_close 등
            } else {
                // 닫혔을 때 (아래쪽 화살표 또는 + 아이콘)
                binding.btnToggleTagInput.setImageResource(R.drawable.ic_arrow_down) // 또는 ic_btn_add
            }

            if (isTagInputVisible && allRecommendedTags.isEmpty()) {
                fetchServerTags()
            }
        }

        // '추가하기' 버튼 클릭 시 직접 입력한 태그 추가
        binding.btnAddTag.setOnClickListener {
            val inputText = binding.etTagSearch.text.toString().trim()
            if (inputText.isNotEmpty()) {
                val cleanName = if (inputText.startsWith("#")) inputText.substring(1) else inputText
                if (selectedTagNames.add(cleanName)) {
                    refreshSelectedTagsUI()
                    refreshRecommendedTagsUI()
                }
                binding.etTagSearch.setText("")
            }
        }

        // 태그 검색창에 글자를 입력할 때마다 추천 목록 실시간 갱신
        binding.etTagSearch.addTextChangedListener { editable ->
            refreshRecommendedTagsUI()
        }
    }
    // 선택된 태그 RecyclerView 초기화
    private fun setupRecyclerViews() {
        selectedAdapter = TagHorizontalAdapter(
            items = selectedTagNames.toList(),
            isSelectedMode = true,
            onItemClick = { tagName ->
                selectedTagNames.remove(tagName)
                refreshSelectedTagsUI()
                refreshRecommendedTagsUI()
            }
        )
        binding.recyclerSelectedTags.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedAdapter
        }

        // 추천 태그 RecyclerView 초기화
        recommendedAdapter = TagHorizontalAdapter(
            items = emptyList(),
            isSelectedMode = false,
            selectedChecker = { tagName -> selectedTagNames.contains(tagName) },
            onItemClick = { tagName ->
                if (selectedTagNames.contains(tagName)) {
                    selectedTagNames.remove(tagName)
                } else {
                    selectedTagNames.add(tagName)
                }
                refreshSelectedTagsUI()
                refreshRecommendedTagsUI()
            }
        )
        binding.recyclerRecommendedTags.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedAdapter
        }
    }

    // 서버로부터 전체 태그 목록을 불러오는 함수
    private fun fetchServerTags() {
        RetrofitClient.service.getTags().enqueue(object : Callback<List<TagResponse>> {
            override fun onResponse(call: Call<List<TagResponse>>, response: Response<List<TagResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    allRecommendedTags.clear()
                    allRecommendedTags.addAll(response.body()!!)
                    refreshRecommendedTagsUI()
                }
            }
            override fun onFailure(call: Call<List<TagResponse>>, t: Throwable) {
                // 네트워크 오류 시 조용히 처리하거나 토스트
            }
        })
    }

    // 태그 목록 (강좌 포함)
    private fun refreshSelectedTagsUI() {
        selectedAdapter.updateItems(selectedTagNames.toList())
    }

    // 태그 목록 (전체)
    private fun refreshRecommendedTagsUI() {
        val keyword = binding.etTagSearch.text.toString().trim()
        val filtered = if (keyword.isEmpty()) {
            allRecommendedTags.map { it.name }
        } else {
            allRecommendedTags.filter { it.name.contains(keyword, ignoreCase = true) }.map { it.name }
        }
        recommendedAdapter.updateItems(filtered)
    }

    // 드래프트 기본 정보 입력창 원상 복원 구역
    fun restoreDraftData(
        title: String?,
        lecturer: String?,
        date: String?,
        topic: String?,
        location: String?,
        summary: String?,
        video: String? = null,
        tags: List<String>? = null
    ) {
        val currentBinding = _binding ?: return

        currentBinding.etTitle.setText(title.orEmpty())
        currentBinding.etSpeaker.setText(lecturer.orEmpty())

        val displayDate = if (date?.contains("T") == true) {
            date.replace("T", " ").substringBeforeLast(":")
        } else {
            date.orEmpty()
        }

        currentBinding.etDate.setText(displayDate)
        currentBinding.etSubject.setText(topic.orEmpty())
        currentBinding.etLocation.setText(location.orEmpty())
        currentBinding.etSummary.setText(summary.orEmpty())
        currentBinding.etVideo.setText(video.orEmpty())

        // 기존 태그가 있다면 복원
        if (!tags.isNullOrEmpty()) {
            selectedTagNames.clear()
            selectedTagNames.addAll(tags)
            refreshSelectedTagsUI()
        }
    }

    // dp 단위를 px로 변환해주는 유틸리티
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()

        // 현재 날짜를 기준으로 달력 다이얼로그 생성
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            // 사용자가 날짜를 선택하면 EditText에 "YYYY-MM-DD" 형식으로 입력
            val dateString = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                // 고른 시간을 "HH:MM" 형태로 정돈합니다.
                val timeString = String.format("%02d:%02d", hourOfDay, minute)

                // [3단계] 최종적으로 날짜와 시간 사이에 공백 한 칸을 두고 입력창에 세팅합니다. (예: "2026-05-20 14:30")
                binding.etDate.setText("$dateString $timeString")
            }

            // 날짜 리스너가 끝나는 지점에서 시간 선택 팝업창을 즉시 화면에 띄웁니다.
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), // 기본값으로 설정할 현재 시간
                cal.get(Calendar.MINUTE),      // 기본값으로 설정할 현재 분
                true                           // 24시간 표기법 사용 여부 (true = 14시, false = 오후 2시)
            ).show()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 서버로 보낼 LectureCreateRequest 객체 생성 함수
    fun getLectureCreateRequest(targetStatus: String): LectureCreateRequest? {
        val currentBinding = _binding ?: return null

        // 입력창에서 "2026-05-20 14:30" 같은 문자열을 가져옵니다.
        val inputDateTime = currentBinding.etDate.text.toString().trim()
        var formattedDate = "2026-01-01T00:00:00.000Z"

        if (inputDateTime.isNotEmpty()) {
            // parts[0]에는 날짜("2026-05-20"), parts[1]에는 시간("14:30")이 할당됩니다.
            val parts = inputDateTime.split(" ")

            if (parts.size == 2) {
                // 날짜와 시간이 모두 들어있는 정상적인 상황이라면
                // 서버가 요구하는 표준 시간 포맷인 "YYYY-MM-DDT_HH:MM:SS.000Z" 형태로 조합합니다.
                formattedDate = "${parts[0]}T${parts[1]}:00.000Z"
            } else {
                // 혹시 모를 예외(시간 입력을 안 했거나 오류가 생겼을 때)를 대비하여 안전장치로 기존 방식을 적용합니다.
                formattedDate = "${parts[0]}T00:00:00.000Z"
            }
        }

        // 사용자가 선택/입력한 태그 이름 리스트를 그대로 반환
        return LectureCreateRequest(
            title = currentBinding.etTitle.text.toString(),
            lectureSummary = currentBinding.etSummary.text.toString(), // 요약/설명 입력창 ID
            lectureDate = formattedDate,            // 날짜
            location = currentBinding.etLocation.text.toString(),      // 장소 입력창 ID
            lecturerName = currentBinding.etSpeaker.text.toString(),  // 강연자 입력창 ID
            topic = currentBinding.etSubject.text.toString(),            // 주제 입력창 ID
            status = targetStatus,                                    // 초기 상태
            tags = selectedTagNames.toList()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}