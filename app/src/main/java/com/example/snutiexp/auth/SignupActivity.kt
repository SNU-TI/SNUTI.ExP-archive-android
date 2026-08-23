package com.example.snutiexp.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivitySignupBinding
import com.example.snutiexp.model.AuthResponse
import com.example.snutiexp.model.SignupRequest
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private var verifiedEmail: String? = null
    private var verificationCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // [핵심] 이메일 인증 화면(EmailVerificationActivity)에서 넘겨준 이메일과 인증 번호 받기
        verifiedEmail = intent.getStringExtra("EMAIL")
        verificationCode = intent.getStringExtra("CODE")

        // 전달받은 이메일이 있다면 EditText에 채워넣고 수정 불가 처리 (사용자 임의 변경 방지)
        if (!verifiedEmail.isNullOrEmpty()) {
            binding.etSignupEmail.setText(verifiedEmail)
            binding.etSignupEmail.isEnabled = false
        }

        // [뒤로가기 / 로그인으로 돌아가기] 버튼 클릭 시 팝업창 띄우기
        binding.btnBackToLogin.setOnClickListener {
            showCancelSignupDialog()
        }

        // 하드웨어 뒤로가기 버튼을 눌렀을 때도 동일하게 팝업창이 뜨도록 처리
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelSignupDialog()
            }
        })

        // [회원가입] 실행 버튼
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etSignupEmail.text.toString().trim()
            val password = binding.etSignupEmail.text.toString().trim()
            val passwordConfirm = binding.etSignupPwConfirm.text.toString().trim()

            // [자체 검증 1] 빈 칸 체크
            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [자체 검증 2] 비밀번호 일치 체크 ★
            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 서로 일치하지 않습니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show()
                // 비밀번호 확인 칸을 강조하거나 비워줄 수도 있습니다.
                binding.etSignupPwConfirm.requestFocus()
                return@setOnClickListener
            }

            // [자체 검증 3] 인증 정보 유효성 체크
            if (verifiedEmail.isNullOrEmpty() || verificationCode.isNullOrEmpty()) {
                Toast.makeText(this, "이메일 인증 정보가 유효하지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            // 서버에 회원가입 요청 전송 (서버 규격에 맞춰 email과 password 전송)
            val signupRequest = SignupRequest(email = email, password = password)
            RetrofitClient.authService.signUp(signupRequest).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val accessToken = authResponse?.accessToken

                        if (!accessToken.isNullOrEmpty()) {
                            // 회원가입 직후 발급받은 토큰을 AuthInterceptor가 사용하는 'token_prefs'에 저장
                            val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
                            sharedPref.edit().putString("access_token", accessToken).apply()

                            Toast.makeText(this@SignupActivity, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()

                            // 최종 회원가입 종료 후 로그인 화면으로 이동 및 백스택 클리어
                            val intent = Intent(this@SignupActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@SignupActivity, "회원가입은 성공했으나 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // [서버 에러 가공]
                        val errorBody = response.errorBody()?.string() ?: ""

                        val userFriendlyMessage = when {
                            // 비밀번호 길이 에러 (서버 응답 메시지 기반)
                            errorBody.contains("size must be between 8") -> "비밀번호는 최소 8자 이상이어야 합니다."

                            // 이메일 중복 에러 (예상 문구)
                            errorBody.contains("already exists") || errorBody.contains("duplicate") -> "이미 가입된 이메일 주소입니다."

                            // 이메일 형식 에러
                            errorBody.contains("email") -> "올바른 이메일 형식을 입력해주세요."

                            // 그 외 알 수 없는 에러
                            else -> "회원가입에 실패했습니다. (오류 코드: ${response.code()})"
                        }

                        Toast.makeText(this@SignupActivity, userFriendlyMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@SignupActivity, "서버 연결 상태가 좋지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 회원가입 취소 확인 팝업창 함수
    private fun showCancelSignupDialog() {
        AlertDialog.Builder(this)
            .setTitle("회원가입 취소")
            .setMessage("뒤로 가시면 입력 중인 내용이 사라집니다. 회원가입을 취소하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
                val token = sharedPref.getString("access_token", null)

                if (!token.isNullOrEmpty()) {
                    deleteAccountAndFinish()
                } else {
                    finish()
                }
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    // '로그인으로 돌아가기'를 눌렀을 때 계정이 존재하면 삭제하고 나가는 함수
    private fun deleteAccountAndFinish() {
        RetrofitClient.authService.deleteAccount().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().remove("access_token").apply()
                Toast.makeText(this@SignupActivity, "회원가입이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                finish() // 통신 실패 시에도 일단 화면은 닫아줌
            }
        })
    }
}