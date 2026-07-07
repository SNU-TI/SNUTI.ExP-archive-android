package com.example.snutiexp.main

import android.graphics.Color
import android.os.Bundle
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
import com.example.snutiexp.model.*
import com.example.snutiexp.network.RetrofitClient
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

class AddCourseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCourseBinding

    private val infoFragment = InfoInputFragment()
    private val editFragment = DocumentEditFragment()

    private var pendingStatus: String = "DRAFT"

    private var isEdit: Boolean = false
    private var lectureId: Long = -1L

    private var fetchedBlocks: List<ArticleBlockResponse>? = null
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

        // 편집 복원 분기 시점 제어
        isEdit = intent.getBooleanExtra("IS_EDIT", false)
        lectureId = intent.getLongExtra("LECTURE_ID", -1)

        if (isEdit) {
            window.decorView.post {
                infoFragment.restoreDraftData(
                    title = intent.getStringExtra("EDIT_TITLE"),
                    lecturer = intent.getStringExtra("EDIT_LECTURER"),
                    date = intent.getStringExtra("EDIT_DATE"),
                    topic = intent.getStringExtra("EDIT_TOPIC"),
                    location = intent.getStringExtra("EDIT_LOCATION"),
                    summary = intent.getStringExtra("EDIT_SUMMARY")
                )
            }
            if (lectureId != -1L) {
                fetchAndRestoreArticles(lectureId)
            }
        }

        // --- 섹션 추가 버튼 클릭 이벤트 연결 ---
        setupSectionButtons()

        // --- 파란 체크 버튼(완료) 클릭 이벤트 ---
        binding.btnDone.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("강의 저장 설정")
                .setMessage("이 강의를 바로 게시하시겠습니까, 아니면 임시저장 하시겠습니까?")
                .setPositiveButton("강의 게시") { dialog, _ ->
                    dialog.dismiss()
                    pendingStatus = "PUBLISHED"
                    checkPermissionAndUpload()
                }
                .setNegativeButton("임시저장") { dialog, _ ->
                    dialog.dismiss()
                    pendingStatus = "DRAFT"
                    checkPermissionAndUpload()
                }
                .setNeutralButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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

            // 문서 편집 창으로 전환되는 교차점에 백업 데이터 강제 밀어넣기 작동
            fetchedBlocks?.let { blocks ->
                editFragment.restoreArticles(blocks)
            }
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }
    }

    // 수정 모드 진입 시 본문 편집창(DocumentEditFragment)에 기존 섹션들을 리스토어하기 위해 상세 데이터를 땡겨오는 통신 파이프라인
    private fun fetchAndRestoreArticles(id: Long) {
        RetrofitClient.service.getAdminLectureDetail(id).enqueue(object : Callback<LectureDetailResponse> {
            override fun onResponse(call: Call<LectureDetailResponse>, response: Response<LectureDetailResponse>) {
                if (response.isSuccessful) {
                    val lectureData = response.body() ?: return
                    // Swagger 2중 blocks 구조를 순서대로 병합
                    val allBlocks = lectureData.articles.flatMap { it.blocks }.sortedBy { it.orderIndex }

                    // 💡 핵심: 찾아온 본문 블록 리스트를 문서 편집 프래그먼트에 바인딩하여 복원 완료!
                    fetchedBlocks = allBlocks
                    editFragment.restoreArticles(allBlocks)
                    Log.d("REGISTER_DEBUG", "편집 화면 본문 복원 완료: ${allBlocks.size}개의 섹션")
                } else {
                    Log.e("REGISTER_DEBUG", "드래프트 본문 복구 조회 실패 코드: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LectureDetailResponse>, t: Throwable) {
                Log.e("REGISTER_DEBUG", "드래프트 본문 복구 네트워크 통신 오류")
            }
        })
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
        val createRequest = infoFragment.getLectureCreateRequest(pendingStatus) ?: return

        if (createRequest.title.trim().isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            binding.btnSwitchInfo.performClick()
            return
        }

        // --- [시작] 로딩 표시 ---
        setLoading(true)

        if (isEdit && lectureId != -1L) {
            // 💡 수정 시 LectureUpdateRequest 객체로 변환하여 전송
            val updateRequest = LectureUpdateRequest(
                title = createRequest.title,
                lectureDate = createRequest.lectureDate,
                location = createRequest.location,
                lectureSummary = createRequest.lectureSummary,
                lecturerName = createRequest.lecturerName,
                topic = createRequest.topic,
                status = createRequest.status,
                tags = createRequest.tags
            )

            // 💡 기존 드래프트 '수정' API 호출 파이프라인 작동
            RetrofitClient.service.updateLecture(lectureId, updateRequest).enqueue(object : Callback<LectureCreateResponse> {
                override fun onResponse(call: Call<LectureCreateResponse>, response: Response<LectureCreateResponse>) {
                    if (response.isSuccessful) {
                        uploadVideoThenArticles(lectureId)
                    } else {
                        setLoading(false)
                        Toast.makeText(this@AddCourseActivity, "강연 수정 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LectureCreateResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(this@AddCourseActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            RetrofitClient.service.createLecture(createRequest).enqueue(object : Callback<LectureCreateResponse> {
                override fun onResponse(call: Call<LectureCreateResponse>, response: Response<LectureCreateResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.id?.let { uploadVideoThenArticles(it) } ?: setLoading(false)
                    } else {
                        setLoading(false)
                        Toast.makeText(this@AddCourseActivity, "강연 생성 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LectureCreateResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(this@AddCourseActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
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

    // 강의 생성 시 비디오 추가
    private fun uploadVideoThenArticles(currentLectureId: Long) {
        // 정보 입력 프래그먼트의 영상 주소창 데이터를 조회합니다.
        val videoUrlInput = infoFragment.view?.findViewById<android.widget.EditText>(R.id.et_video)?.text?.toString()?.trim() ?: ""

        // 만약 영상 링크를 입력하지 않았다면 2단계를 패스하고 바로 3단계 아티클 저장으로 순간 이동합니다.
        if (videoUrlInput.isEmpty() || videoUrlInput == "-") {
            syncArticlesState(currentLectureId)
            return
        }

        // 명세서 규격에 맞게 VideoRequest 스펙 빌드
        val videoRequest = CreateVideoRequest(videoUrl = videoUrlInput, caption = "강연 영상")

        // 백엔드 2단계 API 호출 (POST /admin/lectures/{lectureId}/videos)
        RetrofitClient.service.addVideo(currentLectureId, videoRequest).enqueue(object : Callback<VideoResponse> {
            override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                // 비디오 연동이 완수되면 (201 Created) 최종 3단계 아티클 저장망을 기동합니다.
                syncArticlesState(currentLectureId)
            }

            override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                // 비디오 추가 실패 시 유저 경험을 방해하지 않기 위해 로그만 남기고 아티클은 안전하게 저장하도록 포워딩합니다.
                Log.e("VIDEO_UPLOAD_ERROR", "2단계 비디오 연동 실패: ${t.message}")
                syncArticlesState(currentLectureId)
            }
        })
    }

    // 삭제 대기열(DELETE), 기존 수정 분기(PUT), 신규 삽입(POST) 통신망을 복합 결합하여 일괄 병렬 처리
    private fun syncArticlesState(currentLectureId: Long) {
        val currentSections = editFragment.getSectionData()

        // 섹션이 없다면 굳이 서버 통신하지 않고 성공 처리
        if (currentSections.isEmpty()) {
            Toast.makeText(this@AddCourseActivity, "강의 저장 완료!", Toast.LENGTH_SHORT).show()
            setLoading(false)
            finish() // 여기서 종료!
            return
        }

        // 1. 모든 섹션을 서버가 원하는 blocks 리스트로 변환
        val blocks = currentSections.mapIndexed { index, section ->
            ArticleBlockRequest(
                type = ArticleBlockType.valueOf(section.type), // 💡 String -> Enum 변환
                orderIndex = index,
                textContent = if (section.type == "TEXT") section.content else null,
                clientImageKey = if (section.type == "IMAGE") "img_$index" else null
            )
        }

        // 2. 서버가 요구하는 상위 객체(CreateArticleRequest) 생성
        val requestData = CreateArticleRequest(
            articleTitle = "강의 노트", // 또는 infoFragment에서 가져온 제목
            author = "Admin",
            blocks = blocks
        )

        // 3. JSON 변환
        val requestJson = Gson().toJson(requestData)

        // 4. 멀티파트 구성 (이미지가 있을 경우 추가)
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        builder.addPart(
            MultipartBody.Part.createFormData("request", null,
                requestJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
        )

        currentSections.forEachIndexed { index, section ->
            if (section.type == "IMAGE" && section.imageUri != null) {
                val file = getCompressedImageFile(Uri.parse(section.imageUri))
                if (file != null) {
                    // 중요: clientImageKey와 파일 파트 이름("img_0", "img_1"...)을 일치시킴
                    builder.addPart(
                        MultipartBody.Part.createFormData("img_$index", file.name,
                            file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    )
                }
            }
        }

        // 5. 전송 (RetrofitClient.service의 createArticle 파라미터를 List<MultipartBody.Part>로 설정)
        setLoading(true)
        RetrofitClient.service.createArticle(currentLectureId, builder.build().parts).enqueue(object : Callback<ArticleResponse> {
            override fun onResponse(call: Call<ArticleResponse>, response: Response<ArticleResponse>) {
                setLoading(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@AddCourseActivity, "저장 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AddCourseActivity, "내용 저장 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                }
                finish() // 성공/실패 여부와 관계없이 강의 추가 창은 닫음
            }
            override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                setLoading(false)
                Log.e("UPLOAD_ERROR", "통신 오류: ${t.message}")
                finish() // 네트워크 문제로 내용만 못 저장한 것이므로 창은 닫음
            }
        })
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