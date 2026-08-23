package com.example.snutiexp.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snutiexp.R

class TagHorizontalAdapter(
    private var items: List<String>,
    private val isSelectedMode: Boolean = false,
    private val selectedChecker: ((String) -> Boolean)? = null,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<TagHorizontalAdapter.TagViewHolder>() {

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tv_item_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        // 태그 아이템 하나를 담을 레이아웃 (아래 XML 생성 필요)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horizontal_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tagName = items[position]
        holder.tvTag.text = "#$tagName"

        if (isSelectedMode) {
            holder.tvTag.setTextColor(Color.parseColor("#333333"))
        } else {
            val isSelected = selectedChecker?.invoke(tagName) ?: false
            holder.tvTag.setTextColor(if (isSelected) Color.parseColor("#CCCCCC") else Color.parseColor("#333333"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(tagName)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}