package com.example.snutiexp.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snutiexp.databinding.ItemDetailSectionBinding
import com.example.snutiexp.model.ArticleResponse

class LectureDetailAdapter(private var items: List<ArticleResponse>) :
    RecyclerView.Adapter<LectureDetailAdapter.ViewHolder>() {

    // 1. 뷰 홀더 설정 (ItemDetailSectionBinding 바인딩 객체를 가집니다)
    class ViewHolder(val binding: ItemDetailSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetailSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {
            // 2. 텍스트 섹션 처리 (글자가 있을 때만 노출)
            if (!item.content.isNullOrBlank()) {
                tvSectionContent.visibility = View.VISIBLE
                tvSectionContent.text = item.content
            } else {
                tvSectionContent.visibility = View.GONE
            }

            // 3. 이미지 섹션 처리 (이미지 URL이 있을 때만 Glide로 로드하고 노출)
            if (!item.imageUrl.isNullOrBlank()) {
                ivSectionImage.visibility = View.VISIBLE
                Glide.with(ivSectionImage.context)
                    .load(item.imageUrl)
                    .into(ivSectionImage)
            } else {
                ivSectionImage.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // 4. 상세 데이터가 로드되었을 때 섹션 리스트를 새로고침하는 함수
    fun updateList(newList: List<ArticleResponse>) {
        this.items = newList
        notifyDataSetChanged()
    }
}