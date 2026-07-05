package com.example.snutiexp.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snutiexp.R
import com.example.snutiexp.databinding.FragmentMainListBinding // 기존 메인 목록 레이아웃 바인딩 재활용 가정

// 발행되지 않고 임시 저장된 DRAFT 강좌 목록을 비동기식 카드뷰 리스트로 화면에 렌더링하는 프래그먼트 클래스
class DraftLectureListFragment : Fragment() {

    // 메모리 누수 방지 기법을 적용하여 프래그먼트 생명주기에 맞춤 설계한 뷰바인딩 변수 구역
    private var _binding: FragmentMainListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 기존 강좌 목록 화면에서 사용하던 fragment_main_list 레이아웃을 인플레이트하여 일체화된 UI 테마를 유지
        _binding = FragmentMainListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 리사이클러뷰에 수직 배치 리니어 레이아웃 매니저를 결합하여 수직 스크롤 인프라를 구축
        setupRecyclerView()

        // 로컬 데이터베이스 또는 임시 메모리 저장소에서 드래프트 데이터를 가져와 리스트를 팽창시킵니다.
        loadDraftLectures()
    }

    /**
     * [주석: 리사이클러뷰의 레이아웃 매니저 초기화 및 초기 설정을 동기화하는 메서드]
     */
    private fun setupRecyclerView() {
        binding.rvLectures.layoutManager = LinearLayoutManager(requireContext())
        // 메인 화면 하단에 플로팅 검색바가 존재하므로, 리스트 하단 아이템이 가려지지 않도록 패딩클립 속성을 예비해 둡니다.
        binding.rvLectures.clipToPadding = false
    }

    /**
     * [주석: 서버 연결 전 단계에서 더미 데이터를 바인딩하여 드래프트 강좌 목록 디자인을 검증하는 로직 메서드]
     */
    private fun loadDraftLectures() {
        // 백엔드 소통 해결 전 가시화 상태를 체크하기 위해 가상 임시 데이터 리스트를 빌드
        val draftDummyList = listOf(
            "시안 검증용 임시저장 강좌 1",
            "UI 완성도 테스트용 드래프트 2",
            "서버 연동 대기 중인 가짜 강좌 3"
        )

        /* 리사이클러뷰 어댑터 연결 구역
        - 추후 드래프트 전용 어댑터(DraftLectureAdapter) 또는 기존 어댑터에
                위의 draftDummyList 데이터를 넘겨주어 화면에 카드뷰 형태로 출력하게 됩니다.
        - 현재는 디자인 고도화 상태이므로 토스트 알림으로 데이터 바인딩 시점을 명시해 둡니다.
        */
        if (draftDummyList.isNotEmpty()) {
            Toast.makeText(requireContext(), "총 ${draftDummyList.size}개의 임시 저장 강좌를 불러왔습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 프래그먼트 가시화 뷰가 파괴되는 시점에 바인딩 참조를 해제하여 가비지 컬렉션 메모리 누수를 원천 차단
        _binding = null
    }
}