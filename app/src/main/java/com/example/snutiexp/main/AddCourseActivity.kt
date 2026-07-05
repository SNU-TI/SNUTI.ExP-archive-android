package com.example.snutiexp.main

import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.snutiexp.R
import com.example.snutiexp.databinding.ActivityAddCourseBinding
import com.example.snutiexp.network.RetrofitClient
import com.example.snutiexp.model.LectureCreateRequest
import com.example.snutiexp.model.LectureCreateResponse
import com.example.snutiexp.model.ArticleCreateRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout

class AddCourseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCourseBinding

    private val infoFragment = InfoInputFragment()
    private val editFragment = DocumentEditFragment()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한이 허용됨: 기존 업로드 로직 실행
            startFullUploadAfterPermission()
        } else {
            // 권한 거부됨
            Toast.makeText(this, "이미지 업로드를 위해 저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            setLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCourseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 Fragment 설정 (태그도 추후 추가해야 함)
        supportFragmentManager.beginTransaction()
            .add(R.id.course_frame_container, infoFragment, "InfoInputFragment")
            .add(R.id.course_frame_container, editFragment, "DocumentEditFragment")
            .hide(editFragment) // 편집창은 일단 숨김
            .commit()

        // --- 섹션 추가 버튼 클릭 이벤트 연결 ---
        setupSectionButtons()

        // --- 파란 체크 버튼(완료) 클릭 이벤트 ---
        binding.btnDone.setOnClickListener {
            checkPermissionAndUpload()
        }

        // 스위처 클릭 이벤트 (정보 <-> 편집 전환)
        binding.btnSwitchInfo.setOnClickListener {
            updateSwitcherUI(isInfo = true)
            supportFragmentManager.beginTransaction()
                .show(infoFragment)
                .hide(editFragment)
                .commit()
        }

        binding.btnSwitchEdit.setOnClickListener {
            updateSwitcherUI(isInfo = false)
            supportFragmentManager.beginTransaction()
                .show(editFragment)
                .hide(infoFragment)
                .commit()
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }
    }

    // 로딩 상태를 관리하는 함수
    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            // 로딩 바와 배경 흐림 처리
            loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE

            // 중복 클릭 방지를 위해 버튼 비활성화
            btnDone.isEnabled = !isLoading
            btnBack.isEnabled = !isLoading
            btnSwitchInfo.isEnabled = !isLoading
            btnSwitchEdit.isEnabled = !isLoading
        }
    }

    private fun checkPermissionAndUpload() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // 이미 권한이 있는 경우
                startFullUploadAfterPermission()
            }
            else -> {
                // 권한 요청
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // 강연 정보 생성 요청
    private fun startFullUploadAfterPermission() {
        val createRequest = infoFragment.getLectureCreateRequest()

        if (createRequest == null) {
            Toast.makeText(this, "정보 입력창이 준비되지 않았습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (createRequest.title.trim().isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            binding.btnSwitchInfo.performClick()
            return
        }

        // --- [시작] 로딩 표시 ---
        setLoading(true)

        RetrofitClient.service.createLecture(createRequest).enqueue(object : Callback<LectureCreateResponse> {
            override fun onResponse(call: Call<LectureCreateResponse>, response: Response<LectureCreateResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    // [Step 1] 로그를 통해 서버가 준 응답 전체를 확인합니다.
                    Log.d("REGISTER_DEBUG", "서버 응답 성공: $responseBody")

                    // [Step 1 & 2] ID 추출 시도 (id 혹은 lectureId 필드명 불일치 체크)
                    val lectureId = responseBody?.id

                    if (lectureId != null) {
                        Log.d("REGISTER_DEBUG", "추출된 ID: $lectureId")
                        // 3. 편집 프래그먼트에서 섹션 데이터를 가져옵니다.
                        val sections = editFragment.getSectionData()

                        if (sections.isEmpty()) {
                            // --- [수정 포인트] 섹션(내용)이 없으면 여기서 즉시 종료 ---
                            // 내용(섹션)이 없는 경우: 여기서 바로 등록 완료 처리
                            Log.d("REGISTER_DEBUG", "섹션이 비어있음. 강연 정보만 등록하고 종료합니다.")
                            setLoading(false)
                            Toast.makeText(
                                this@AddCourseActivity,
                                "강연 정보가 등록되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish() // 강연 정보만 생성하고 바로 메인으로 돌아감
                        } else {
                            // 섹션이 있으면 기존대로 아티클 업로드 진행
                            Log.d("REGISTER_DEBUG", "섹션 존재 (${sections.size}개). 아티클 업로드를 시작합니다.")
                            uploadArticles(lectureId)
                        }
                    }
//                  val lectureId = response.body()?.id ?: return
//                  uploadArticles(lectureId)
                } else {
                    // --- [실패] 로딩 해제 ---
                    setLoading(false)
                    Log.e("REGISTER_DEBUG", "ID 추출 실패! 서버 응답 모델(LectureCreateResponse)의 필드명을 확인하세요.")
                    Toast.makeText(this@AddCourseActivity, "강연 생성 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LectureCreateResponse>, t: Throwable) {
                // --- [오류] 로딩 해제 ---
                setLoading(false)
                Log.e("REGISTER_DEBUG", "네트워크 실패: ${t.message}")
                Toast.makeText(this@AddCourseActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri, proj, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = columnIndex?.let { cursor.getString(it) }
        cursor?.close()
        return path
    }

    private fun getCompressedImageFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // 1. 임시 파일 생성
            val storageDir = cacheDir // 앱 전용 캐시 디렉토리 사용
            val tempFile = File.createTempFile("compressed_", ".jpg", storageDir)

            // 2. 비트맵 압축 (JPEG 형식, 품질 70%)
            val outputStream = FileOutputStream(tempFile)
            // 70 정도로 설정하면 화질 저하가 거의 없으면서 용량은 획기적으로 줄어듭니다.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.flush()
            outputStream.close()

            return tempFile
        } catch (e: Exception) {
            Log.e("IMAGE_OPTIMIZE", "이미지 압축 중 오류 발생: ${e.message}")
            return null
        }
    }

    // 문서 섹션들을 아티클로 등록
    private fun uploadArticles(lectureId: Long) {
        val sections = editFragment.getSectionData()
        var completedCount = 0

        if (sections.isEmpty()) {
            setLoading(false)
            Toast.makeText(this, "강연 등록 완료!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        sections.forEachIndexed { index, section ->
            // JSON 데이터 생성 (String -> RequestBody)
            val articleData = ArticleCreateRequest(
                type = section.type,
                textContent = if (section.type == "TEXT") section.content else null,
                orderIndex = index
            )
            val json = Gson().toJson(articleData)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

            // 이미지 파일 처리 (IMAGE 타입인 경우만)
            var imagePart: MultipartBody.Part? = null
            if (section.type == "IMAGE" && section.imageUri != null) {
                val compressedFile = getCompressedImageFile(Uri.parse(section.imageUri!!))
                if (compressedFile != null) {
                    val fileRequestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    // 첫 번째 파라미터는 서버의 Key 값인 "image"
                    imagePart = MultipartBody.Part.createFormData("image", compressedFile.name, fileRequestBody)
                }
            }

            // 섹션별 아티클 등록 (POST /admin/lectures/{lectureId}/articles)
            RetrofitClient.service.addArticle(lectureId, requestBody, imagePart).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    completedCount++
                    if (completedCount == sections.size) {
                        // --- [최종 완료] 로딩 해제 ---
                        setLoading(false)
                        Toast.makeText(this@AddCourseActivity, "모든 내용이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    completedCount++
                    Log.e("API_ERROR", "섹션 ${index} 전송 실패: ${t.message}")
                    if (completedCount == sections.size) {
                        setLoading(false)
                        Toast.makeText(this@AddCourseActivity, "일부 내용 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            })
        }
    }

    // --- 버튼 클릭 시 Fragment의 함수를 호출하는 로직 ---
    private fun setupSectionButtons() {
        binding.btnAddTextActivity.setOnClickListener {
            // 현재 container에 있는 프래그먼트를 찾아 DocumentEditFragment라면 함수 실행
            val currentFragment = supportFragmentManager.findFragmentById(R.id.course_frame_container)
            if (currentFragment is DocumentEditFragment) {
                currentFragment.addTextSection()
            }
        }

        binding.btnAddImageActivity.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.course_frame_container)
            if (currentFragment is DocumentEditFragment) {
                currentFragment.addImageSection()
            }
        }
    }

    // UI 업데이트 함수: 화면이 바뀔 때 상단 타이틀, 체크 버튼, 하단 아이콘 색상을 관리합니다.
    private fun updateSwitcherUI(isInfo: Boolean) {
        if (isInfo) {
            // 정보 입력 화면일 때
            binding.tvAddTitle.text = "정보 입력"
            binding.btnDone.visibility = View.GONE // 체크 버튼 숨김
            binding.layoutSectionAddButtons.visibility = View.GONE // 섹션 추가 묶음 숨김

            // 하단 스위처 아이콘 및 텍스트 색상 변경
            binding.ivInfoIcon.setImageResource(R.drawable.ic_diamond_blue)
            binding.tvInfoLabel.setTextColor(ContextCompat.getColor(this, R.color.blue_main))
            binding.btnSwitchInfo.setBackgroundResource(R.drawable.bg_inner_search_bar)

            binding.ivEditIcon.setImageResource(R.drawable.ic_circle_black)
            binding.tvEditLabel.setTextColor(Color.BLACK)
            binding.btnSwitchEdit.background = null
        } else {
            // 문서 편집 화면일 때
            binding.tvAddTitle.text = "문서 편집"
            binding.btnDone.visibility = View.VISIBLE // 체크 버튼 표시
            binding.layoutSectionAddButtons.visibility = View.VISIBLE // 섹션 추가 버튼 묶음 보이기

            // 하단 스위처 아이콘 및 텍스트 색상 변경
            binding.ivInfoIcon.setImageResource(R.drawable.ic_diamond_black)
            binding.tvInfoLabel.setTextColor(Color.BLACK)
            binding.btnSwitchInfo.background = null

            binding.ivEditIcon.setImageResource(R.drawable.ic_circle_blue)
            binding.tvEditLabel.setTextColor(ContextCompat.getColor(this, R.color.blue_main))
            binding.btnSwitchEdit.setBackgroundResource(R.drawable.bg_inner_search_bar)
        }
    }
}