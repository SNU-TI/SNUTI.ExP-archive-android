package com.example.snutiexp.signup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivitySignupBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Retrofit 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("http://43.201.109.122:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authService = retrofit.create(AuthService::class.java)

        // 2. [로그인으로 돌아가기] 버튼
        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        // 3. [회원가입] 실행 버튼
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etSignupEmail.text.toString()
            val password = binding.etSignupPw.text.toString()
            val passwordConfirm = binding.etSignupPwConfirm.text.toString()

            // [자체 검증 1] 빈 칸 체크
            if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [자체 검증 2] 비밀번호 일치 체크 ★ (요청하신 부분)
            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 서로 일치하지 않습니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show()
                // 비밀번호 확인 칸을 강조하거나 비워줄 수도 있습니다.
                binding.etSignupPwConfirm.requestFocus()
                return@setOnClickListener
            }

            // 4. 서버에 회원가입 요청 전송
            val signupRequest = SignupRequest(email, password)
            authService.signUp(signupRequest).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@SignupActivity, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                        finish() // 로그인 화면으로 이동
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

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    Toast.makeText(this@SignupActivity, "서버 연결 상태가 좋지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}