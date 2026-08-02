package com.example.snutiexp.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivityEmailVerificationBinding
import com.example.snutiexp.model.PasswordResetVerifyRequest
import com.example.snutiexp.model.VerifyEmailCodeRequest
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private var verificationPurpose: String? = null // "SIGN_UP" 또는 "PASSWORD_RESET"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 어떤 목적(회원가입 vs 비밀번호 리셋)으로 진입했는지 확인
        verificationPurpose = intent.getStringExtra("PURPOSE")
        val userEmail = intent.getStringExtra("EMAIL")

        // 전달받은 이메일이 있다면 EditText에 미리 채워넣기
        if (!userEmail.isNullOrEmpty()) {
            binding.etEmailInput.setText(userEmail)
            // 회원가입 시나리오 등 이미 입력된 이메일이라면 수정 못 하게 막을 수도 있음
            binding.etEmailInput.isEnabled = false
        }

        // "인증 메일 다시 전송" 텍스트에 밑줄 추가
        binding.btnResendCode.paintFlags = binding.btnResendCode.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // 0. 상단 뒤로가기 버튼 클릭 리스너 (@+id/btn_back)
        // 인증하지 않고 뒤로 갈 때 앱이 알아서 처리하도록 설정
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 1. [인증하기] 버튼 클릭 리스너 (이메일로 인증 코드 전송 요청)
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버 인증 메일 전송 API 호출 (필요에 따라 이메일 바디나 쿼리를 받는 구조로 확장 가능)
            sendEmailCode(email)
        }

        // 2. [인증 메일 다시 전송] 텍스트 클릭 리스너
        binding.btnResendCode.setOnClickListener {
            val email = binding.etEmailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "먼저 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendEmailCode(email)
        }

        // 3. 하단 [확인] 버튼 클릭 리스너 (입력한 인증번호 검증 요청)
        binding.btnVerifySubmit.setOnClickListener {
            val code = binding.etCodeInput.text.toString().trim()

            if (code.length != 6) {
                Toast.makeText(this, "인증번호 6자리를 올바르게 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyEmailCode(code)
        }
    }

    // 서버로 인증 코드 전송 요청 함수
    private fun sendEmailCode(email: String) {
        val call = if (verificationPurpose == "PASSWORD_RESET") {
            // 비밀번호 리셋용 전송 API (필요한 Request 형태에 맞게 조정)
            RetrofitClient.authService.sendPasswordResetCode(com.example.snutiexp.model.PasswordResetSendRequest(email))
        } else {
            // 회원가입용 토큰 기반 이메일 전송 API
            RetrofitClient.authService.sendEmailVerification()
        }

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EmailVerificationActivity, "인증 코드가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EmailVerificationActivity, "인증 코드 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@EmailVerificationActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 서버로 인증 코드 검증 요청 함수
    private fun verifyEmailCode(code: String) {
        if (verificationPurpose == "PASSWORD_RESET") {
            // 비밀번호 리셋 검증 로직
            val email = binding.etEmailInput.text.toString().trim()
            val request = PasswordResetVerifyRequest(email = email, code = code)
            RetrofitClient.authService.verifyPasswordResetCode(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailVerificationActivity, "인증 성공! 새 비밀번호 입력 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                        // 비밀번호 리셋 화면으로 이메일과 인증 코드 전달하며 이동
                        val intent = Intent(this@EmailVerificationActivity, PasswordResetActivity::class.java).apply {
                            putExtra("EMAIL", email)
                            putExtra("CODE", code)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EmailVerificationActivity, "인증번호가 일치하지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@EmailVerificationActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // 회원가입 인증 검증 로직
            val request = VerifyEmailCodeRequest(code = code)
            RetrofitClient.authService.verifyEmailCode(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailVerificationActivity, "이메일 인증이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        // 인증 실패 시 강제 퇴장시키지 않고 문구만 출력하여 재시도 가능하게 함
                        Toast.makeText(this@EmailVerificationActivity, "인증번호가 올바르지 않습니다. 다시 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@EmailVerificationActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}