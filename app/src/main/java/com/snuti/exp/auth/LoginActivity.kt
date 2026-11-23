package com.snuti.exp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.snuti.exp.databinding.ActivityLoginBinding
import com.snuti.exp.main.MainActivity
import com.snuti.exp.model.LoginRequest
import com.snuti.exp.model.LoginResponse
import com.snuti.exp.network.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private var isAutoLoginChecked = true
    private var loginCall: Call<LoginResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 토큰 존재 여부 확인부터 수행
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
        val token = sharedPref.getString("access_token", null)

        // 토큰이 존재하고 아직 만료되지 않았을 때만 자동 로그인으로 메인 진입
        if (!token.isNullOrEmpty() && !isTokenExpired(token)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 만료된 토큰이 남아있었다면 기존 세션 정보 정리
        if (!token.isNullOrEmpty()) {
            sharedPref.edit().clear().apply()
        }

        // 토큰이 없을 때만 UI 초기화 진행
        initLoginUI()
    }

    // JWT Payload의 exp 클레임으로 만료 여부 확인 (파싱 불가 시 보수적으로 만료 처리)
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true

            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING)
            val jsonObject = JSONObject(String(decodedBytes, Charsets.UTF_8))

            if (!jsonObject.has("exp")) return false

            val expSeconds = jsonObject.getLong("exp")
            val nowSeconds = System.currentTimeMillis() / 1000L
            // 만료 10초 전부터 만료로 간주해 경계선 오류 방지
            expSeconds <= (nowSeconds + 10L)
        } catch (e: Exception) {
            Log.e("JWT_EXP_ERROR", "토큰 만료 확인 중 오류: ${e.message}")
            true
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        val isInteractive = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = isInteractive
        binding.etLoginId.isEnabled = isInteractive
        binding.etLoginPw.isEnabled = isInteractive
        binding.tvFindPassword.isEnabled = isInteractive
        binding.btnGoSignup.isEnabled = isInteractive
    }

    private fun initLoginUI() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setLoadingState(false)

        // [비밀번호 찾기] 버튼 기능 추가
        binding.tvFindPassword.setOnClickListener {
            val intent = Intent(this, EmailVerificationActivity::class.java).apply {
                putExtra("PURPOSE", "PASSWORD_RESET") // 💡 핵심: 비밀번호 리셋 목적 플래그 전달
            }
            startActivity(intent)
        }

        // [회원가입하러 가기] 버튼 기능
        binding.btnGoSignup.setOnClickListener {
            val intent = Intent(this, EmailVerificationActivity::class.java).apply {
                putExtra("PURPOSE", "SIGN_UP") // 회원가입 목적 플래그 전달
            }
            startActivity(intent)
        }

        // [로그인] 버튼 기능
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginId.text.toString().trim()
            val password = binding.etLoginPw.text.toString()

            // 빈 칸 체크
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 이전 요청 취소 후 새 로그인 요청 시작 및 로딩 화면 표시
            loginCall?.cancel()
            setLoadingState(true)

            // 서버에 로그인 요청 전송
            val loginRequest = LoginRequest(email, password)
            loginCall = RetrofitClient.authService.login(loginRequest)
            loginCall?.enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (call.isCanceled || isFinishing || isDestroyed) return

                    // 중복 선언 제거 및 응답 상태 일원화
                    val token = if (response.isSuccessful) response.body()?.accessToken else null
                    if (response.isSuccessful && !token.isNullOrEmpty()) {
                        val isAdmin = checkIsAdminFromJwt(token)
                        val prefs = getSharedPreferences("token_prefs", MODE_PRIVATE)
                        with(prefs.edit()) {
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
                        // 로그인 실패 또는 토큰 누락 시 로딩 종료 및 안내 메시지 표시
                        setLoadingState(false)
                        if (response.isSuccessful) {
                            Toast.makeText(this@LoginActivity, "인증 토큰을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    if (call.isCanceled || isFinishing || isDestroyed) return // 취소되거나 종료된 화면의 콜백 무시

                    setLoadingState(false) // 네트워크 실패 시 로딩 종료
                    Log.e("LOGIN_ERROR", "로그인 요청 실패: ${t.message}")
                    Toast.makeText(this@LoginActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 화면 종료 시 진행 중인 로그인 요청 정리
    override fun onDestroy() {
        super.onDestroy()
        loginCall?.cancel()
        loginCall = null
    }

    // 화면이 다시 보일 때 저장된 아이디 불러오기
    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("token_prefs", MODE_PRIVATE)
        val savedId = sharedPref.getString("saved_id", null)

        if (savedId != null) {
            binding.etLoginId.setText(savedId)
            isAutoLoginChecked = true
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