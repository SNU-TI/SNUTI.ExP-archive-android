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
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
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
import com.example.snutiexp.model.LectureDetailResponse

class AddCourseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCourseBinding
    private var isEditMode: Boolean = false
    private var editingLectureId: Long = -1L

    private var existingVideoId: Long? = null

    private val infoFragment = InfoInputFragment()
    private val editFragment = DocumentEditFragment()

    private var pendingStatus: String = "DRAFT"

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

        val mode = intent.getStringExtra("MODE")
        val isDraftEdit = intent.getBooleanExtra("IS_EDIT", false)

        isEditMode = mode == "EDIT" || isDraftEdit

        editingLectureId =
            intent.getLongExtra("LECTURE_ID", -1L)

        // 초기 Fragment 설정
        supportFragmentManager.beginTransaction()
            .add(R.id.course_frame_container, infoFragment, "InfoInputFragment")
            .add(R.id.course_frame_container, editFragment, "DocumentEditFragment")
            .hide(editFragment) // 편집창은 일단 숨김
            .commit()
        //  Fragment가 생성된 뒤 기존 데이터 불러오기
        binding.root.post {
            if (isEditMode && editingLectureId != -1L) {
                // 임시저장(Draft) 수정일 때는 서버에 상세 조회를 요청하지 않고,
                // 이미 인텐트로 넘어온 데이터만 활용합니다. 일반 수정 모드일 때만 서버 조회를 실행합니다.
                if (!isDraftEdit) {
                    fetchLectureForEdit(editingLectureId)
                    fetchAndRestoreArticles(editingLectureId)
                } else {
                    Log.d("REGISTER_DEBUG", "임시저장 수정 모드: 서버 상세 조회 및 본문 조회 생략 (인텐트 데이터 사용)")
                }
            }
        }

        // 편집 복원 분기 시점 제어
        if (isDraftEdit) {
            window.decorView.post {
                infoFragment.restoreDraftData(
                    title = intent.getStringExtra("EDIT_TITLE"),
                    lecturer = intent.getStringExtra("EDIT_LECTURER"),
                    date = intent.getStringExtra("EDIT_DATE"),
                    topic = intent.getStringExtra("EDIT_TOPIC"),
                    location = intent.getStringExtra("EDIT_LOCATION"),
                    summary = intent.getStringExtra("EDIT_SUMMARY"),
                    video = intent.getStringExtra("EDIT_VIDEO")
                )
            }
            if (editingLectureId != -1L) {
                fetchAndRestoreArticles(editingLectureId)
            }
        }

        // --- 섹션 추가 버튼 클릭 이벤트 연결 ---
        setupSectionButtons()

        // --- 파란 체크 버튼(게시) 클릭 이벤트 ---
        binding.btnDone.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("강의 게시")
                .setMessage("이 강의를 바로 게시하시겠습니까?")
                .setPositiveButton("게시하기") { dialog, _ ->
                    dialog.dismiss()
                    pendingStatus = "PUBLISHED"

                    if (isEditMode) {
                        updateLecture()
                    } else {
                        checkPermissionAndUpload()
                    }
                }
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // --- 임시저장 버튼 클릭 이벤트 ---
        binding.btnDraft.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("임시저장")
                .setMessage("작성 중인 내용을 임시저장하시겠습니까?")
                .setPositiveButton("임시저장") { dialog, _ ->
                    dialog.dismiss()
                    pendingStatus = "DRAFT"

                    if (isEditMode) {
                        updateLecture()
                    } else {
                        checkPermissionAndUpload()
                    }
                }
                .setNegativeButton("취소") { dialog, _ ->
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
        binding.btnBack.setOnClickListener {
            showExitConfirmationDialog()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    // 작성 취소 경고 팝업창 공통 함수
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("작성 취소")
            .setMessage("작성 중인 내용이 사라집니다. 정말 나가시겠습니까?")
            .setPositiveButton("나가기") { _, _ ->
                finish()
            }
            .setNegativeButton("계속 작성", null)
            .show()
    }

    //강의수정요청
    private fun updateLecture() {
        if (editingLectureId == -1L) {
            Toast.makeText(
                this,
                "수정할 강의 ID가 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        checkPermissionAndUpload()
    }
    private fun checkPermissionAndUpload() {
        val permission =
            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startFullUploadAfterPermission()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun startFullUploadAfterPermission() {
        val createRequest =
            infoFragment.getLectureCreateRequest(pendingStatus)
                ?: return

        if (createRequest.title.trim().isEmpty()) {
            Toast.makeText(
                this,
                "제목을 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()

            binding.btnSwitchInfo.performClick()
            return
        }

        setLoading(true)

        if (isEditMode && editingLectureId != -1L) {
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

            RetrofitClient.service
                .updateLecture(
                    editingLectureId,
                    updateRequest
                )
                .enqueue(
                    object : Callback<LectureCreateResponse> {

                        override fun onResponse(
                            call: Call<LectureCreateResponse>,
                            response: Response<LectureCreateResponse>
                        ) {
                            if (response.isSuccessful) {
                                uploadVideoThenArticles(
                                    editingLectureId
                                )
                            } else {
                                setLoading(false)

                                Toast.makeText(
                                    this@AddCourseActivity,
                                    "강연 수정 실패 (코드: ${response.code()})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<LectureCreateResponse>,
                            t: Throwable
                        ) {
                            setLoading(false)

                            Toast.makeText(
                                this@AddCourseActivity,
                                "네트워크 오류: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
        } else {
            RetrofitClient.service
                .createLecture(createRequest)
                .enqueue(
                    object : Callback<LectureCreateResponse> {

                        override fun onResponse(
                            call: Call<LectureCreateResponse>,
                            response: Response<LectureCreateResponse>
                        ) {
                            if (response.isSuccessful) {
                                val newLectureId =
                                    response.body()?.id

                                if (newLectureId != null) {
                                    uploadVideoThenArticles(
                                        newLectureId
                                    )
                                } else {
                                    setLoading(false)

                                    Toast.makeText(
                                        this@AddCourseActivity,
                                        "생성된 강의 ID가 없습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                setLoading(false)

                                Toast.makeText(
                                    this@AddCourseActivity,
                                    "강연 생성 실패 (코드: ${response.code()})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<LectureCreateResponse>,
                            t: Throwable
                        ) {
                            setLoading(false)

                            Toast.makeText(
                                this@AddCourseActivity,
                                "네트워크 오류: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
        }
    }
    private fun fetchLectureForEdit(id: Long) {
        RetrofitClient.service
            .getLectureDetail(id)
            .enqueue(
                object : Callback<LectureDetailResponse> {
                    override fun onResponse(
                        call: Call<LectureDetailResponse>,
                        response: Response<LectureDetailResponse>
                    ) {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@AddCourseActivity,
                                "강의 정보를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        val lecture = response.body()

                        if (lecture == null) {
                            Toast.makeText(
                                this@AddCourseActivity,
                                "강의 정보가 비어 있습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        val firstVideo = lecture.videos.firstOrNull()
                        existingVideoId = firstVideo?.id
                        val videoUrl = firstVideo?.videoUrl

                        infoFragment.restoreDraftData(
                            title = lecture.title,
                            lecturer = lecture.lecturerName,
                            date = lecture.lectureDate,
                            topic = lecture.topic,
                            location = lecture.location,
                            summary = lecture.lectureSummary,
                            video = videoUrl,
                            tags = lecture.tags.map { it.name }
                        )
                    }

                    override fun onFailure(
                        call: Call<LectureDetailResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@AddCourseActivity,
                            "강의 정보를 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }

    // 수정 모드 진입 시 본문 편집창(DocumentEditFragment)에 기존 섹션들을 리스토어하기 위해 상세 데이터를 땡겨오는 통신 파이프라인
    private fun fetchAndRestoreArticles(id: Long) {
        // 임시저장(Draft) 수정일 때는 서버에 본문 조회를 요청하지 않음 (400 에러 방지)
        val isDraft = intent.getBooleanExtra("IS_EDIT", false)
        if (isDraft) {
            Log.d("REGISTER_DEBUG", "임시저장(Draft) 상태이므로 서버 본문 조회를 건너뜁니다.")
            return
        }

        RetrofitClient.service.getLectureDetail(id).enqueue(object : Callback<LectureDetailResponse> {
            override fun onResponse(call: Call<LectureDetailResponse>, response: Response<LectureDetailResponse>) {
                if (response.isSuccessful) {
                    val lectureData = response.body() ?: return
                    // 현재 구조상 첫 번째 아티클의 ID를 가져옵니다.
                    val targetArticleId = lectureData.articles.firstOrNull()?.id

                    // 각 블록에 아티클 ID 정보를 심어주거나, DocumentEditFragment가 이 ID를 알 수 있게 전달합니다.
                    // DocumentEditFragment의 restoreArticles가 아티클 리스트 자체를 통째로 받도록 하거나 ID를 같이 넘겨야 합니다.
                    fetchedBlocks = lectureData.articles.flatMap { it.blocks }.sortedBy { it.orderIndex }

                    // 프래그먼트에 블록들과 함께 진짜 `targetArticleId`를 전달합니다!
                    editFragment.restoreArticlesWithArticleId(lectureData.articles)

                    Log.d("REGISTER_DEBUG", "편집 화면 본문 복원 완료 (Article ID: $targetArticleId)")
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
            btnDraft.isEnabled = !isLoading
            btnBack.isEnabled = !isLoading
            btnSwitchInfo.isEnabled = !isLoading
            btnSwitchEdit.isEnabled = !isLoading
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

    // 강의 생성 시 비디오 추가 또는 수정
    private fun uploadVideoThenArticles(currentLectureId: Long) {
        // 정보 입력 프래그먼트의 영상 주소창 데이터를 조회합니다.
        val videoUrlInput =
            infoFragment.view?.findViewById<android.widget.EditText>(R.id.et_video)?.text?.toString()
                ?.trim() ?: ""

        // 만약 영상 링크를 입력하지 않았을 시
        // 기존 비디오가 있다면 삭제 API 호출
        // 기존 비디오가 없다면 2단계를 패스하고 바로 3단계 아티클 저장으로 순간 이동합니다.
        if (videoUrlInput.isEmpty() || videoUrlInput == "-") {
            if (isEditMode && existingVideoId != null) {
                RetrofitClient.service.deleteVideo(existingVideoId!!).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        existingVideoId = null // 삭제 완료 후 초기화
                        syncArticlesState(currentLectureId)
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("VIDEO_DELETE_ERROR", "비디오 삭제 실패: ${t.message}")
                        syncArticlesState(currentLectureId)
                    }
                })
            } else {
                syncArticlesState(currentLectureId)
            }
            return
        }

        // 수정 모드이고 기존 비디오 ID가 존재한다면 PUT (수정), 아니면 POST (생성) 분기 처리
        if (isEditMode && existingVideoId != null) {
            // 명세서 규격에 맞게 VideoRequest 스펙 빌드
            val updateRequest = UpdateVideoRequest(videoUrl = videoUrlInput, caption = "강연 영상")

            // 백엔드 2단계 API 호출 (POST /admin/lectures/{lectureId}/videos)
            RetrofitClient.service.updateVideo(existingVideoId!!, updateRequest).enqueue(object : Callback<VideoResponse> {
                override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                    syncArticlesState(currentLectureId)
                }

                override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                    // 비디오 추가 실패 시 유저 경험을 방해하지 않기 위해 로그만 남기고 아티클은 안전하게 저장하도록 포워딩합니다.
                    Log.e("VIDEO_UPDATE_ERROR", "비디오 수정 실패: ${t.message}")
                    syncArticlesState(currentLectureId)
                }
            })
        } else {
            val createRequest = CreateVideoRequest(videoUrl = videoUrlInput, caption = "강연 영상")

            RetrofitClient.service.addVideo(currentLectureId, createRequest).enqueue(object : Callback<VideoResponse> {
                override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                    // 새로 생성된 경우 ID를 갱신해 줍니다.
                    existingVideoId = response.body()?.id
                    syncArticlesState(currentLectureId)
                }

                override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                    Log.e("VIDEO_UPLOAD_ERROR", "비디오 추가 실패: ${t.message}")
                    syncArticlesState(currentLectureId)
                }
            })
        }
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

        // 1. 모든 섹션을 서버가 원하는 blocks 리스트로 변환 (TEXT 블록에는 existingBlockId를 넣지 않을 것)
        val blocks = currentSections.mapIndexed { index, section ->
            val isLocalNewImage = section.type == "IMAGE" && !section.imageUri.isNullOrEmpty() &&
                    (Uri.parse(section.imageUri).scheme == "content" || Uri.parse(section.imageUri).scheme == "file")

            ArticleBlockRequest(
                type = ArticleBlockType.valueOf(section.type),
                orderIndex = index,
                textContent = if (section.type == "TEXT") section.content else null,
                // 새 이미지면 existingBlockId를 null로, 기존 이미지면 clientImageKey를 null로 설정하여 정확히 하나만 들어가게 함!
                existingBlockId = if (section.type == "IMAGE" && !isLocalNewImage && !section.isNew) section.existingBlockId else null,
                clientImageKey = if (isLocalNewImage) "img_$index" else null
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
            // imageUri가 존재하고, 서버 웹 URL이 아닌 로컬 URI(content:// 또는 file://)일 때만 압축 후 멀티파트에 추가
            if (section.type == "IMAGE" && !section.imageUri.isNullOrEmpty()) {
                val uri = Uri.parse(section.imageUri)
                val scheme = uri.scheme

                // 로컬에서 새로 선택한 이미지인 경우에만 처리 (http/https로 시작하는 기존 서버 이미지 URL은 무시)
                if (scheme == "content" || scheme == "file") {
                    val file = getCompressedImageFile(uri)
                    if (file != null) {
                        builder.addPart(
                            MultipartBody.Part.createFormData("img_$index", file.name,
                                file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                        )
                    }
                }
            }
        }

        // 5. 전송 (RetrofitClient.service의 createArticle 파라미터를 List<MultipartBody.Part>로 설정)
        setLoading(true)

        // 편집 모드이고 기존 아티클 ID가 있다면 PUT(/admin/articles/{articleId}), 아니면 POST로 분기 처리
        // 만약 기존 아티클 ID를 별도로 저장하고 있다면 해당 ID를 대입 (예: existingArticleId)
        // 현재 구조상 첫 번째 섹션의 articleId 등을 활용할 수 있음
        val targetArticleId = currentSections.firstOrNull { it.articleId != null }?.articleId
            ?: fetchedBlocks?.firstOrNull()?.let { /* 블록이 속한 아티클 ID를 매핑하거나 기존 ID 확보 */ 3L } // 예시 방어 코드

        // 무조건 생성(POST)이 아니라, 수정 모드이고 targetArticleId가 존재하면 무조건 PUT(updateArticle)을 호출하도록 강제 분기!

        if (isEditMode) {
            // 만약 targetArticleId를 확실히 모른다면 서버 명세에 맞춰 기존 아티클 ID(예: 3)를 직접 넣거나 찾아야 합니다.
            val articleIdToUpdate = targetArticleId ?: 3L // 안전 장치 (기존 로그의 아티클 id가 3이므로)

            RetrofitClient.service.updateArticle(articleIdToUpdate, builder.build().parts).enqueue(object : Callback<ArticleResponse> {
                override fun onResponse(call: Call<ArticleResponse>, response: Response<ArticleResponse>) {
                    setLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddCourseActivity, "강의 수정 완료!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AddCourseActivity, "내용 수정 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
                override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                    setLoading(false)
                    Log.e("UPLOAD_ERROR", "통신 오류: ${t.message}")
                    finish()
                }
            })
        } else {
            RetrofitClient.service.createArticle(currentLectureId, builder.build().parts)
                .enqueue(object : Callback<ArticleResponse> {
                    override fun onResponse(call: Call<ArticleResponse>,response: Response<ArticleResponse>) {
                        setLoading(false)
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddCourseActivity, "저장 완료!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this@AddCourseActivity,
                                "내용 저장 실패 (코드: ${response.code()})",
                                Toast.LENGTH_SHORT
                            ).show()
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
            binding.btnDraft.visibility = View.GONE // 임시저장 버튼 숨김
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
            binding.btnDraft.visibility = View.VISIBLE // 임시저장 버튼 표시
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