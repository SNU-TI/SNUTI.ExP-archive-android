package com.example.snutiexp.main

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snutiexp.databinding.ItemEditSectionBinding

class EditSectionAdapter(
    private val items: MutableList<EditSection>,
    private val onImageClick: (Int) -> Unit,
    private val onSectionRemoved: (EditSection) -> Unit
) : RecyclerView.Adapter<EditSectionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemEditSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {

            // 섹션 번호
            tvSectionNumber.text = "Section ${position + 1}"

            // RecyclerView 재활용 시 기존 TextWatcher 제거
            val oldWatcher = etSectionContent.tag as? TextWatcher
            if (oldWatcher != null) {
                etSectionContent.removeTextChangedListener(oldWatcher)
            }
            etSectionContent.tag = null

            if (item.type == "TEXT") {

                // -------------------------
                // TEXT SECTION
                // -------------------------

                etSectionContent.visibility = View.VISIBLE

                // 이미지 관련 View는 모두 숨김
                ivSectionImage.visibility = View.GONE
                btnAddImage.visibility = View.GONE

                // 이전 이미지 로딩 제거
                Glide.with(ivSectionImage.context)
                    .clear(ivSectionImage)

                // 저장되어 있던 내용 복원
                etSectionContent.setText(item.content)

                val textWatcher = object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        val currentPos = holder.adapterPosition

                        if (currentPos != RecyclerView.NO_POSITION) {
                            items[currentPos].content = s.toString()
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                    }
                }

                etSectionContent.addTextChangedListener(textWatcher)
                etSectionContent.tag = textWatcher

            } else {

                // -------------------------
                // IMAGE SECTION
                // -------------------------

                etSectionContent.visibility = View.GONE

                if (!item.imageUri.isNullOrEmpty()) {

                    // 이미지가 있는 경우
                    ivSectionImage.visibility = View.VISIBLE
                    btnAddImage.visibility = View.GONE

                    // 이전에 설정됐을 수 있는 배경 제거
                    ivSectionImage.background = null

                    Glide.with(ivSectionImage.context)
                        .load(item.imageUri)
                        .into(ivSectionImage)

                    // 이미지 자체를 누르면 다시 이미지 선택 가능
                    ivSectionImage.setOnClickListener {
                        val currentPos = holder.adapterPosition

                        if (currentPos != RecyclerView.NO_POSITION) {
                            onImageClick(currentPos)
                        }
                    }

                } else {

                    // 이미지가 없는 경우
                    Glide.with(ivSectionImage.context)
                        .clear(ivSectionImage)

                    ivSectionImage.setImageDrawable(null)
                    ivSectionImage.visibility = View.GONE

                    // "+ 이미지 추가" 버튼 표시
                    btnAddImage.visibility = View.VISIBLE

                    btnAddImage.setOnClickListener {
                        val currentPos = holder.adapterPosition

                        if (currentPos != RecyclerView.NO_POSITION) {
                            onImageClick(currentPos)
                        }
                    }
                }
            }

            // -------------------------
            // SECTION DELETE
            // -------------------------

            btnDeleteSection.setOnClickListener {

                val currentPos = holder.adapterPosition

                if (currentPos != RecyclerView.NO_POSITION) {

                    val context = holder.itemView.context

                    AlertDialog.Builder(context)
                        .setTitle("섹션 삭제")
                        .setMessage(
                            "Section ${currentPos + 1}을 정말로 삭제하시겠습니까?\n" +
                                    "작성된 내용이 모두 사라집니다."
                        )
                        .setPositiveButton("삭제") { dialog, _ ->

                            // Dialog가 떠 있는 동안 position이 변경되었을 수도 있으므로
                            // 다시 현재 position 확인
                            val deletePos = holder.adapterPosition

                            if (deletePos != RecyclerView.NO_POSITION) {
                                val targetItem = items[deletePos]

                                onSectionRemoved(targetItem)

                                items.removeAt(deletePos)

                                notifyItemRemoved(deletePos)
                                notifyItemRangeChanged(
                                    deletePos,
                                    items.size - deletePos
                                )
                            }

                            dialog.dismiss()
                        }
                        .setNegativeButton("취소") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}