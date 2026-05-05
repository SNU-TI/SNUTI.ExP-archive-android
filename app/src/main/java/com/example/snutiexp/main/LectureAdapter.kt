package com.example.snutiexp.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.snutiexp.databinding.ItemLectureBinding
import com.example.snutiexp.model.LectureListItem

class LectureAdapter(private var items: List<LectureListItem>) :
    RecyclerView.Adapter<LectureAdapter.ViewHolder>() {

    // 각 아이템의 뷰를 보관하는 홀더입니다.
    class ViewHolder(val binding: ItemLectureBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 아이템 레이아웃을 바인딩하여 생성합니다.
        val binding = ItemLectureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {
            // 1. 강연 제목 설정
            tvLectureTitle.text = item.title

            // 2. 강연자 및 장소 정보 (예: "홍길동 | 제1공학관")
            tvLectureInfo.text = "${item.lecturerName} | ${item.location}"

            // 3. 날짜 표시 (서버의 ISO 8601 포맷에서 날짜 부분만 추출)
            // 예: "2026-05-04T..." -> "2026-05-04"
            tvLectureDate.text = item.lectureDate.split("T")[0]

            // 4. 아이템 클릭 시 상세 화면 이동 (추후 구현)
            root.setOnClickListener {
                // TODO: 상세 페이지 이동 로직 (lectureId 전달)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * 서버에서 새로운 목록을 받아왔을 때 리스트를 갱신합니다.
     */
    fun updateList(newList: List<LectureListItem>) {
        this.items = newList
        notifyDataSetChanged() // 전체 리스트 갱신 알림
    }
}