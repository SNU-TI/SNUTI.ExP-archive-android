package com.example.snutiexp.main // 본인의 패키지명과 맞는지 확인하세요

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentDocumentEditBinding
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

class DocumentEditFragment : Fragment() {
    private var _binding: FragmentDocumentEditBinding? = null
    private val binding get() = _binding!!

    // 섹션 데이터를 관리할 리스트와 어댑터
    private val sectionList = mutableListOf<EditSection>()

    // 어댑터 변수를 선언 , 나중에 초기화할 것이므로 lateinit을 사용
    private lateinit var sectionAdapter: EditSectionAdapter

    // 사진이 들어갈 위치 저장
    private var selectedPosition: Int = -1

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (selectedPosition != -1) {
                // 리스트 데이터 업데이트
                sectionList[selectedPosition].imageUri = it.toString()
                // 해당 아이템만 갱신
                sectionAdapter.notifyItemChanged(selectedPosition)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 이 프래그먼트가 fragment_document_edit.xml 레이아웃을 사용하도록 연결합니다.
        _binding = FragmentDocumentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 및 리사이클러뷰 연결 로직
        sectionAdapter = EditSectionAdapter(sectionList) { position ->
            selectedPosition = position
            pickImageLauncher.launch("image/*") // 갤러리 열기
        }
        binding.rvSections.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    //Activity의 '텍스트 섹션 추가' 버튼 클릭 시 호출됨
    fun addTextSection() {
        val newSection = EditSection(id = sectionList.size + 1, type = "TEXT")
        sectionList.add(newSection)
        sectionAdapter.notifyItemInserted(sectionList.size - 1)
        binding.rvSections.scrollToPosition(sectionList.size - 1)
    }

    //Activity의 '이미지 섹션 추가' 버튼 클릭 시 호출됨
    fun addImageSection() {
        val newSection = EditSection(id = sectionList.size + 1, type = "IMAGE")
        sectionList.add(newSection)
        sectionAdapter.notifyItemInserted(sectionList.size - 1)
        binding.rvSections.scrollToPosition(sectionList.size - 1)
    }

    // 현재까지 작성된 섹션 리스트를 반환하는 함수
    fun getSectionData(): List<EditSection> {
        return sectionList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}