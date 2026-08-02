package com.example.snutiexp.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivitySignupBinding
import com.example.snutiexp.auth.EmailVerificationActivity
import com.example.snutiexp.main.MainActivity
import com.example.snutiexp.model.AuthResponse
import com.example.snutiexp.model.SignupRequest
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    // 이메일 인증 화면 다녀오기 결과를 받기 위한 계약 (ActivityResultLauncher)
    private val emailVerifyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // [인증 성공 시] 최종 메인 화면으로 이동
            Toast.makeText(this, "이메일 인증이 완료되었습니다!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, com.example.snutiexp.main.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // [로그인으로 돌아가기] 버튼
        binding.btnBackToLogin.setOnClickListener {
            // ⭐ 인증 여부와 상관없이, 이미 계정 생성(토큰 발급)이 진행된 상태라면 뒤로 갈 때 계정 삭제 및 정리 수행
            val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
            val token = sharedPref.getString("access_token", null)

            if (!token.isNullOrEmpty()) {
                deleteAccountAndFinish()
            } else {
                finish()
            }
        }

        // [회원가입] 실행 버튼
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etSignupEmail.text.toString()
            val password = binding.etSignupPw.text.toString()
            val passwordConfirm = binding.etSignupPwConfirm.text.toString()

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

            // 서버에 회원가입 요청 전송
            val signupRequest = SignupRequest(email, password)
            RetrofitClient.authService.signUp(signupRequest).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val accessToken = authResponse?.accessToken

                        if (!accessToken.isNullOrEmpty()) {
                            // 💡 회원가입 직후 발급받은 토큰을 AuthInterceptor가 사용하는 'token_prefs'에 저장
                            val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
                            sharedPref.edit().putString("access_token", accessToken).apply()

                            // 🚀 [회원가입 성공 -> 이메일 인증 화면으로 이동]
                            val intent = Intent(this@SignupActivity, EmailVerificationActivity::class.java)
                            intent.putExtra("PURPOSE", "SIGN_UP")
                            intent.putExtra("EMAIL", email)
                            emailVerifyLauncher.launch(intent)
                        } else {
                            Toast.makeText(this@SignupActivity, "회원가입은 성공했으나 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // [서버 에러 가공] ★ (요청하신 한국어 변환 부분)
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