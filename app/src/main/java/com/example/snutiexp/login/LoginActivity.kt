package com.example.snutiexp.login

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivityLoginBinding
import com.example.snutiexp.main.MainActivity
import com.example.snutiexp.network.RetrofitClient
import com.example.snutiexp.signup.SignupActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private var isAutoLoginChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 토큰 존재 여부 확인부터 수행
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
        val token = sharedPref.getString("access_token", null)

        // 저장된 토큰 확인
        if (!token.isNullOrEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return // 아래 UI 초기화 코드 실행 안 함
        }

        // 토큰이 없을 때만 UI 초기화 진행
        initLoginUI()
    }

    private fun initLoginUI() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 체크박스 클릭 리스너 설정
        binding.tvAutoLogin.setOnClickListener {
            isAutoLoginChecked = !isAutoLoginChecked
            updateCheckboxUI()
        }

        // [회원가입하러 가기] 버튼 기능
        binding.btnGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // [로그인] 버튼 기능 추가
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginId.text.toString()
            val password = binding.etLoginPw.text.toString()

            // 빈 칸 체크
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버에 로그인 요청 전송
            val loginRequest = LoginRequest(email, password)
            RetrofitClient.authService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        val token = loginResponse?.accessToken

                        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
                        if (!token.isNullOrEmpty()) {
                            // JWT 토큰 페이로드 파싱 및 관리자 플래그 기포팅 저장
                            val isAdmin = checkIsAdminFromJwt(token)

                            val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("access_token", token)
                                putBoolean("is_admin_user", isAdmin)

                                // 체크박스 상태에 따른 아이디 저장 로직
                                if (isAutoLoginChecked) {
                                    putString("saved_id", email)
                                } else {
                                    remove("saved_id")
                                }
                                apply() // 비동기로 데이터 저장
                            }

                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // 토큰 필드가 비어있거나 없는 경우
                            Toast.makeText(this@LoginActivity, "인증 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 로그인 실패 (비밀번호 틀림, 존재하지 않는 계정 등) : 400, 401, 404 등 HTTP 에러 응답 처리
                        val errorBody = response.errorBody()?.string() ?: ""
                        val errorMessage = when {
                            errorBody.contains("not found") -> "존재하지 않는 계정입니다."
                            errorBody.contains("password") -> "비밀번호가 올바르지 않습니다."
                            else -> "로그인 정보가 올바르지 않습니다."
                        }
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 화면이 다시 보일 때 저장된 아이디 불러오기
    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
        val savedId = sharedPref.getString("saved_id", null)

        if (savedId != null) {
            binding.etLoginId.setText(savedId)
            isAutoLoginChecked = true
            updateCheckboxUI()
        }
    }

    // 체크박스 아이콘 토글 헬퍼 함수
    private fun updateCheckboxUI() {
        if (isAutoLoginChecked) {
            binding.tvAutoLogin.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.checkbox_on_background, 0, 0, 0)
        } else {
            binding.tvAutoLogin.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.checkbox_off_background, 0, 0, 0)
        }
    }

    // JWT 토큰의 Payload 구역을 디코딩하여 관리자 조건 여부를 판별하는 함수
    private fun checkIsAdminFromJwt(token: String): Boolean {
        try {
            // JWT 구조는 "Header.Payload.Signature" 순으로 점(.)으로 쪼개져 있습니다.
            val parts = token.split(".")
            if (parts.size < 2) return false

            // 두 번째 항목인 Payload 구역 추출
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            val jsonString = String(decodedBytes, Charsets.UTF_8)

            // JSON 내부 파싱 처리
            val jsonObject = JSONObject(jsonString)

            // 가설 A: 백엔드가 'role' 이라는 클레임 이름을 심어두었을 경우
            if (jsonObject.has("role")) {
                val role = jsonObject.getString("role")
                return role.equals("ADMIN", ignoreCase = true)
            }

            // 가설 B: 클레임 없이 'sub' 혹은 'email' 필드만 있을 경우 계정 아이디 규칙 패턴으로 판별
            val email = when {
                jsonObject.has("email") -> jsonObject.getString("email")
                jsonObject.has("sub") -> jsonObject.getString("sub")
                else -> ""
            }

            return email.startsWith("admin") || email.contains("admin@")
        } catch (e: Exception) {
            Log.e("JWT_PARSE_ERROR", "토큰 분석 중 에러 발생: ${e.message}")
            return false
        }
    }
}