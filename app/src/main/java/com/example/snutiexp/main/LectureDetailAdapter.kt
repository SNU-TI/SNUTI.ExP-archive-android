package com.example.snutiexp.main

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snutiexp.databinding.ItemDetailSectionBinding
import com.example.snutiexp.databinding.ItemVideoPreviewBinding
import com.example.snutiexp.model.ArticleBlockResponse
import com.example.snutiexp.model.VideoResponse
import com.example.snutiexp.util.YouTubeUtils
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants

class LectureDetailAdapter(private var items: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 뷰 타입 정의
    companion object {
        private const val TYPE_ARTICLE = 0
        private const val TYPE_VIDEO = 1
    }

    // 뷰 홀더 설정 (ItemDetailSectionBinding 바인딩 객체를 가집니다)
    class ArticleViewHolder(val binding: ItemDetailSectionBinding) : RecyclerView.ViewHolder(binding.root)
    class VideoViewHolder(val binding: ItemVideoPreviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ArticleBlockResponse -> TYPE_ARTICLE
            is VideoResponse -> TYPE_VIDEO
            else -> TYPE_ARTICLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_VIDEO -> VideoViewHolder(ItemVideoPreviewBinding.inflate(inflater, parent, false))
            else -> ArticleViewHolder(ItemDetailSectionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if (holder is ArticleViewHolder && item is ArticleBlockResponse) {
            holder.binding.apply {
                // 텍스트 섹션 처리 (글자가 있을 때만 노출)
                if (!item.textContent.isNullOrBlank()) {
                    tvSectionContent.visibility = View.VISIBLE
                    tvSectionContent.text = item.textContent
                } else {
                    tvSectionContent.visibility = View.GONE
                }

                // 이미지 섹션 처리 (이미지 URL이 있을 때만 Glide로 로드하고 노출)
                if (!item.imageUrl.isNullOrBlank()) {
                    ivSectionImage.visibility = View.VISIBLE

                    Glide.with(ivSectionImage.context)
                        .load(item.imageUrl)
                        .into(ivSectionImage)
                } else {
                    ivSectionImage.visibility = View.GONE
                }
            }
        } else if (holder is VideoViewHolder && item is VideoResponse) { // VideoResponse로 캐스팅
            val videoId = YouTubeUtils.getYoutubeVideoId(item.videoUrl)
            if (videoId != null) {
                // 썸네일 이미지 로드 (Glide 라이브러리 사용 중이시므로)
                val thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                Glide.with(holder.binding.ivVideoThumbnail.context)
                    .load(thumbnailUrl)
                    .into(holder.binding.ivVideoThumbnail)
            }

            // 클릭 시 외부 연결
            holder.binding.root.setOnClickListener {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(item.videoUrl)
                )
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // 상세 데이터가 로드되었을 때 섹션 리스트를 새로고침하는 함수
    fun updateList(newList: List<Any>) {
        this.items = newList
        notifyDataSetChanged()
    }
}