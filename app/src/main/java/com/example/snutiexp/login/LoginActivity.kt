package com.example.snutiexp.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivityLoginBinding
import com.example.snutiexp.main.MainActivity
import com.example.snutiexp.signup.AuthService
import com.example.snutiexp.signup.SignupActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Retrofit 설정 (회원가입과 동일한 주소)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://43.201.109.122:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authService = retrofit.create(AuthService::class.java)

        // 2. [회원가입하러 가기] 버튼 기능 (이미 구현된 부분)
        binding.btnGoSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // 3. [로그인] 버튼 기능 추가
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
            authService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                        // 성공 시 메인 화면으로 이동
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // 로그인 화면은 닫아줍니다.
                    } else {
                        // 로그인 실패 (비밀번호 틀림, 존재하지 않는 계정 등)
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
}