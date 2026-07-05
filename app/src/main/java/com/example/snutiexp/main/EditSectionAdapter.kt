package com.example.snutiexp.main

import android.net.Uri
import com.example.snutiexp.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.snutiexp.databinding.ItemEditSectionBinding

class EditSectionAdapter(
    private val items: MutableList<EditSection>,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<EditSectionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemEditSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEditSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.etSectionContent.tag = null

        holder.binding.apply {
            // 섹션 번호 설정 (예: Section 1, Section 2)
            tvSectionNumber.text = "Section ${position + 1}"

            if (item.type == "TEXT") {
                // 텍스트 섹션일 때: 입력창은 보이고, 이미지 영역은 숨김
                etSectionContent.visibility = android.view.View.VISIBLE
                ivSectionImage.visibility = android.view.View.GONE

                // 데이터 복원 (리사이클러뷰 재사용 시 텍스트 보존)
                etSectionContent.setText(item.content)

                // 실시간 데이터 저장 로직
                val textWatcher = object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        // 사용자가 입력한 내용을 즉시 데이터 모델에 반영
                        val currentPos = holder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION) {
                            items[currentPos].content = s.toString()
                        }
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                }
                etSectionContent.addTextChangedListener(textWatcher)
                etSectionContent.tag = textWatcher
            } else {
                // 이미지 섹션일 때: 입력창은 숨기고, 이미지 영역을 보임
                etSectionContent.visibility = android.view.View.GONE
                ivSectionImage.visibility = android.view.View.VISIBLE

                // 1. 이미지 표시 로직 (URI가 있으면 이미지를, 없으면 기본 배경/아이콘)
                if (item.imageUri != null) {
                    ivSectionImage.setImageURI(Uri.parse(item.imageUri))
                } else {
                    // 이미지가 없을 때 보여줄 기본 이미지나 색상
                    ivSectionImage.setImageResource(R.drawable.ic_btn_add)
                    ivSectionImage.setBackgroundColor(android.graphics.Color.LTGRAY)
                }

                // 2. 이미지 영역 클릭 시 갤러리 호출
                ivSectionImage.setOnClickListener {
                    onImageClick(holder.adapterPosition)
                }
            }

            // 삭제 버튼 클릭 리스너
            btnDeleteSection.setOnClickListener {
                // 안전하게 현재 위치를 가져와서 삭제
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    // 다이얼로그를 띄우기 위해 버튼(View)이 속한 컨텍스트(Context)를 가져옵니다.
                    val context = holder.itemView.context

                    // 1. 삭제 확인 알림창(AlertDialog) 생성
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("섹션 삭제") // 다이얼로그 제목
                        .setMessage("Section ${currentPos + 1}을 정말로 삭제하시겠습니까?\n작성된 내용이 모두 사라집니다.") // 다이얼로그 본문
                        .setPositiveButton("삭제") { dialog, _ ->
                            // 사용자가 '삭제'를 눌렀을 때만 실제 데이터 제거 및 화면 갱신을 진행합니다.
                            items.removeAt(currentPos)
                            notifyItemRemoved(currentPos)
                            notifyItemRangeChanged(currentPos, items.size) // 삭제 후 하위 섹션 번호들 갱신
                            dialog.dismiss() // 알림창 닫기
                        }
                        .setNegativeButton("취소") { dialog, _ ->
                            // '취소'를 누르면 데이터 변경 없이 알림창만 닫습니다.
                            dialog.dismiss()
                        }
                        .show() // 2. 화면에 알림창 표시
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}