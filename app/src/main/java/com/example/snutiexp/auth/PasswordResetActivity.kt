package com.example.snutiexp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivityPasswordResetBinding
import com.example.snutiexp.model.PasswordResetRequest
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PasswordResetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordResetBinding

    private var userEmail: String? = null
    private var verificationCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordResetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이메일 인증 화면에서 넘겨준 이메일과 인증 번호 받기
        userEmail = intent.getStringExtra("EMAIL")
        verificationCode = intent.getStringExtra("CODE")

        // 전달받은 이메일이 있다면 EditText에 채워넣고 수정 불가 처리 (사용자가 임의 변경 방지)
        if (!userEmail.isNullOrEmpty()) {
            binding.etSignupEmail.setText(userEmail)
            binding.etSignupEmail.isEnabled = false
        }

        // [변경 완료] 버튼 클릭 리스너
        binding.btnSignupSubmit.setOnClickListener {
            val email = binding.etSignupEmail.text.toString().trim()
            val newPassword = binding.etSignupPw.text.toString().trim()
            val passwordConfirm = binding.etSignupPwConfirm.text.toString().trim()

            // 1. 빈 칸 체크
            if (email.isEmpty() || newPassword.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 새 비밀번호 일치 여부 체크
            if (newPassword != passwordConfirm) {
                Toast.makeText(this, "새 비밀번호가 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                binding.etSignupPwConfirm.requestFocus()
                return@setOnClickListener
            }

            // 3. 인증 번호 확인
            if (verificationCode.isNullOrEmpty()) {
                Toast.makeText(this, "인증 정보가 유효하지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            // 4. 서버로 최종 비밀번호 리셋 요청 전송 (PasswordResetRequest 활용)
            val request = PasswordResetRequest(
                email = email,
                code = verificationCode!!,
                newPassword = newPassword
            )

            RetrofitClient.authService.resetPassword(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PasswordResetActivity, "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()

                        // 비밀번호 변경 완료 후 로그인 화면으로 이동 (스택 클리어)
                        val intent = Intent(this@PasswordResetActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        // 서버 에러 가공
                        val errorBody = response.errorBody()?.string() ?: ""
                        val userFriendlyMessage = when {
                            errorBody.contains("size must be between 8") -> "비밀번호는 최소 8자 이상이어야 합니다."
                            else -> "비밀번호 변경에 실패했습니다. (오류 코드: ${response.code()})"
                        }
                        Toast.makeText(this@PasswordResetActivity, userFriendlyMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@PasswordResetActivity, "서버 연결 상태가 좋지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}