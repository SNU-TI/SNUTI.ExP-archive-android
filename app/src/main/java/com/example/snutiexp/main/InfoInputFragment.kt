package com.example.snutiexp.main

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.snutiexp.R
import com.example.snutiexp.databinding.FragmentInfoInputBinding
import com.example.snutiexp.model.LectureCreateRequest
import com.google.android.flexbox.FlexboxLayout
import java.util.Calendar

class InfoInputFragment : Fragment() {
    private var _binding: FragmentInfoInputBinding? = null
    private val binding get() = _binding!!

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

        // 날짜 EditText 클릭 시 달력 띄우기
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // 태그 입력창 엔터 키 리스너 추가
        binding.etTagInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagText = binding.etTagInput.text.toString().trim()

                if (tagText.isNotEmpty()) {
                    addTagToLayout(tagText) // 태그 생성 함수 호출
                    binding.etTagInput.setText("") // 입력창 비우기
                }
                true
            } else false
        }
    }

    // 새로운 태그를 생성하여 FlexboxLayout에 추가하는 함수
    private fun addTagToLayout(text: String) {
        val tagView = TextView(requireContext()).apply {
            val formattedText = if (text.startsWith("#")) text else "#$text"
            this.text = formattedText

            // bg_tag_item은 이전에 만든 drawable 리소스입니다.
            setBackgroundResource(R.drawable.bg_tag_item)
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            textSize = 13f
            setTextColor(Color.parseColor("#333333"))

            val layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
            this.layoutParams = layoutParams

            // 태그 클릭 시 삭제 기능
            setOnClickListener {
                binding.flexboxTags.removeView(this)
            }
        }

        binding.flexboxTags.addView(tagView)
    }

    // dp 단위를 px로 변환해주는 유틸리티 (추가됨)
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()

        // 현재 날짜를 기준으로 달력 다이얼로그 생성
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            // 사용자가 날짜를 선택하면 EditText에 "YYYY-MM-DD" 형식으로 입력
            val dateString = "${year}-${month + 1}-${dayOfMonth}"
            binding.etDate.setText(dateString)
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 제목과 설명을 가져오는 함수 예시
    fun getLectureCreateRequest(): LectureCreateRequest? {
        val currentBinding = _binding ?: return null

        return LectureCreateRequest(
            title = currentBinding.etTitle.text.toString(),
            lectureSummary = currentBinding.etSummary.text.toString(), // 요약/설명 입력창 ID
            lectureDate = currentBinding.etDate.text.toString(),          // 날짜 (추후 DatePicker 연동)
            location = currentBinding.etLocation.text.toString(),      // 장소 입력창 ID
            lecturerName = currentBinding.etSpeaker.text.toString(),  // 강연자 입력창 ID
            topic = currentBinding.etSubject.text.toString(),            // 주제 입력창 ID
            status = "DRAFT"                                    // 초기 상태
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}