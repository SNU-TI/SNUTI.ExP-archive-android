package com.example.snutiexp.main

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.snutiexp.databinding.ItemLectureBinding
import com.example.snutiexp.model.LectureListItemResponse

class LectureAdapter(
    private var items: List<LectureListItemResponse>,
    private val onItemClick: ((LectureListItemResponse, Int) -> Unit)? = null
) : RecyclerView.Adapter<LectureAdapter.ViewHolder>() {

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
            tvLectureTitle.text = item.title.ifEmptyDash()

            // 2. 강연자 및 장소 정보 (예: "홍길동 | 제1공학관")
            val name = item.lecturerName.ifEmptyDash()
            val location = item.location.ifEmptyDash()
            tvLectureInfo.text = "$name | $location"

            // 3. 날짜 표시 (서버의 ISO 8601 포맷에서 날짜 부분만 추출)
            // 예: "2026-05-04T..." -> "2026-05-04"
            val rawDate = item.lectureDate.toString()
            val isDefaultDate = rawDate.contains("-01-01T00:00:00")

            tvLectureDate.text = if (rawDate.isBlank() || isDefaultDate) {
                "-"
            } else if (rawDate.contains("T")) {
                rawDate.split("T")[0]
            } else {
                rawDate
            }

            // 4. 태그 표시 설정 (한 줄 말줄임 적용)
            val tagList = item.tags
            if (!tagList.isNullOrEmpty()) {
                // 태그 이름을 #태그 형태로 띄어쓰기하여 연결
                tvLectureTags.text = tagList.joinToString(" ") { "#${it.name}" }
                tvLectureTags.visibility = View.VISIBLE
            } else {
                tvLectureTags.visibility = View.GONE
            }

            // 5. 아이템 클릭 시 상세 화면 이동
            root.setOnClickListener {view ->
                if (onItemClick != null) {
                    onItemClick.invoke(item, holder.bindingAdapterPosition)
                } else {
                    val context = view.context
                    val intent = Intent(context, LectureDetailActivity::class.java).apply {
                        putExtra("LECTURE_ID", item.id)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun String?.ifEmptyDash(): String {
        return if (this.isNullOrBlank()) "-" else this
    }

    override fun getItemCount(): Int = items.size

    // 서버에서 새로운 목록을 받아왔을 때 리스트를 갱신합니다.
    fun updateList(newList: List<LectureListItemResponse>) {
        this.items = newList
        notifyDataSetChanged() // 전체 리스트 갱신 알림
    }
}