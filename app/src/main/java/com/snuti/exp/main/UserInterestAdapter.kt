package com.snuti.exp.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.snuti.exp.R
import com.snuti.exp.model.TagResponse

class UserInterestAdapter(
    private var items: List<TagResponse>,
    private val isSelectedMode: Boolean = false,
    private val selectedChecker: ((TagResponse) -> Boolean)? = null,
    private val onItemClick: (TagResponse) -> Unit
) : RecyclerView.Adapter<UserInterestAdapter.TagViewHolder>() {

    // 현재 선택된 태그의 ID를 관리하는 변수 (null이면 선택된 태그 없음)
    private var selectedTagId: Long? = null

    // 외부(Fragment 등)에서 선택된 태그 ID를 변경하고 UI를 갱신할 수 있는 메서드
    fun setSelectedTagId(tagId: Long?) {
        selectedTagId = tagId
        notifyDataSetChanged()
    }

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tv_item_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horizontal_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = items[position]
        holder.tvTag.text = "#${tag.name}"

        val isSelected = (tag.id == selectedTagId)

        // 기존에 지정된 @drawable/bg_tag_item 배경의 색상(Tint)을 동적으로 변경합니다.
        if (isSelected) {
            // 선택됨: 검은색 배경, 흰색 텍스트
            holder.tvTag.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            holder.tvTag.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            // 선택 안 됨: 기본 배경 색상(예: 원래 색상 또는 연한 회색), 어두운 텍스트
            // 만약 원래 bg_tag_item의 기본 색상으로 돌리고 싶다면 null을 주거나 기본 컬러를 지정하세요.
            holder.tvTag.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F3F5")) // 혹은 원하는 기본 배경색
            holder.tvTag.setTextColor(Color.parseColor("#333333"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(tag)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TagResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}