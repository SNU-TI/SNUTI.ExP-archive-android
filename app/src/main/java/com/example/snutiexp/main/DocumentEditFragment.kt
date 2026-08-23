package com.example.snutiexp.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.databinding.FragmentDocumentEditBinding
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.example.snutiexp.model.ArticleBlockResponse
import com.example.snutiexp.model.ArticleResponse

class DocumentEditFragment : Fragment() {
    private var _binding: FragmentDocumentEditBinding? = null
    private val binding get() = _binding!!

    // 섹션 데이터를 관리할 리스트와 어댑터
    private val sectionList = mutableListOf<EditSection>()

    // 어댑터 변수를 선언 , 나중에 초기화할 것이므로 lateinit을 사용
    private lateinit var sectionAdapter: EditSectionAdapter

    // 사진이 들어갈 위치 저장
    private var selectedPosition: Int = -1

    // 액티비티에서 보낸 데이터가 프래그먼트 뷰 생성보다 먼저 도착할 경우를 대비한 안전 백업 저장소
    private var pendingBlocks: List<ArticleBlockResponse>? = null

    // 수정하다가 지운 아티클 ID 목록
    private val deletedArticleIds = mutableListOf<Long>()
    
    // 데이터가 복구되었는지 확인
    private var isRestored: Boolean = false
    
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

        // 백업 데이터 강제 인플레션 구역
        pendingBlocks?.let {
            restoreArticles(it)
            pendingBlocks = null // 소모 후 비우기
        }
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 및 리사이클러뷰 연결 로직
        sectionAdapter = EditSectionAdapter(sectionList, { position ->
            selectedPosition = position
            pickImageLauncher.launch("image/*")
        }, { removedSection ->
            // 💡 만약 서버에 이미 등록되어 있던 유서 깊은 섹션이라면 삭제 대기열에 추가!
            if (!removedSection.isNew && removedSection.articleId != null) {
                deletedArticleIds.add(removedSection.articleId!!)
            }
        })

        binding.rvSections.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // 기존 아티클 본문 섹션 데이터 원상 복원 구역
    fun restoreArticles(blocks: List<ArticleBlockResponse>, targetArticleId: Long? = null) {
        if (_binding == null || !::sectionAdapter.isInitialized) {
            // 💡 뷰가 아직 안 그려졌다면 백업소에 임시 보관하고 탈출!
            pendingBlocks = blocks
            return
        }
        // 이미 복구가 완료되었다면 현재 상태 유지
        if (isRestored) {
            android.util.Log.d("REGISTER_DEBUG", "이미 복원이 완료된 상태이므로 데이터 유지를 위해 복원 통지를 생략합니다.")
            return
        }
        // 기존의 비어있던 리스트 초기화
        sectionList.clear()
        deletedArticleIds.clear()

        blocks.forEach { block ->
            val restoredSection = EditSection(
                id = block.id.toInt(),
                type = block.type.name, // "TEXT" 또는 "IMAGE"
                content = block.textContent ?: "", // 기존 입력했던 텍스트 복구
                imageUri = block.imageUrl, // 기존 업로드했던 이미지 경로 복구
                existingBlockId = block.id, // 💡 서버 블록의 고유 ID를 existingBlockId에 정확히 매핑
                isNew = false,          // 💡 서버에서 온 것이므로 false
                articleId = targetArticleId
            )
            sectionList.add(restoredSection)
        }

        // 복원 완수 마킹
        isRestored = true

        binding.rvSections.post {
            if (::sectionAdapter.isInitialized) {
                android.util.Log.d("REGISTER_DEBUG", "최초 복원 확정: 어댑터 렌더링 시작 (개수: ${sectionList.size})")
                sectionAdapter.notifyDataSetChanged()
            }
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

    // 아티클 리스트를 통째로 받아 각 섹션에 아티클 ID(articleId)를 매핑해주는 함수
    fun restoreArticlesWithArticleId(articles: List<ArticleResponse>) {
        if (_binding == null || !::sectionAdapter.isInitialized) {
            // 뷰가 아직 안 그려졌다면 블록들을 백업해두고 리턴 (기존 pendingBlocks 활용)
            pendingBlocks = articles.flatMap { it.blocks }.sortedBy { it.orderIndex }
            return
        }
        if (isRestored) {
            return
        }

        sectionList.clear()
        deletedArticleIds.clear()

        articles.forEach { article ->
            val parentArticleId = article.id // 진짜 아티클 고유 ID

            article.blocks.forEach { block ->
                val restoredSection = EditSection(
                    id = block.id.toInt(),
                    type = block.type.name,
                    content = block.textContent ?: "",
                    imageUri = block.imageUrl,
                    articleId = parentArticleId,
                    existingBlockId = block.id,
                    isNew = false
                )
                sectionList.add(restoredSection)
            }
        }

        isRestored = true

        binding.rvSections.post {
            if (::sectionAdapter.isInitialized) {
                sectionAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}