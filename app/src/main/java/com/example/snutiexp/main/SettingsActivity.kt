package com.example.snutiexp.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.R
import com.example.snutiexp.databinding.ActivitySettingsBinding
import com.example.snutiexp.auth.LoginActivity
import com.example.snutiexp.model.ChangePasswordRequest
import com.example.snutiexp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : AppCompatActivity() {

    // 액티비티이므로 프래그먼트의 _binding 패턴 대신 lateinit을 사용합니다.
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 뷰 바인딩 초기화
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. 뒤로가기 버튼 클릭 리스너
        binding.btnBack.setOnClickListener {
            // 액티비티를 종료하여 이전 화면(MainActivity)으로 돌아갑니다.
            finish()
        }

        // 3. 비밀번호 변경 클릭 리스너
        binding.groupPassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // 4. 로그아웃 클릭 리스너
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // 5. 계정 삭제 클릭 리스너
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    // 비밀번호 변경 다이얼로그 띄우기
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)

        val etCurrentPw = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_current_pw)
        val etNewPw = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_new_pw)
        val etNewPwConfirm = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_new_pw_confirm)

        AlertDialog.Builder(this)
            .setTitle("비밀번호 변경")
            .setView(dialogView)
            .setPositiveButton("변경") { _, _ ->
                val currentPw = etCurrentPw.text.toString().trim()
                val newPw = etNewPw.text.toString().trim()
                val newPwConfirm = etNewPwConfirm.text.toString().trim()

                if (currentPw.isEmpty() || newPw.isEmpty()) {
                    Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPw.length < 8) {
                    Toast.makeText(this, "새 비밀번호는 최소 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPw != newPwConfirm) {
                    Toast.makeText(this, "새 비밀번호가 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    etNewPwConfirm.requestFocus()
                    return@setPositiveButton
                }

                // 서버로 비밀번호 변경 요청 전송 (AuthInterceptor가 자동으로 토큰을 헤더에 붙여줌)
                val request = ChangePasswordRequest(currentPassword = currentPw, newPassword = newPw)
                RetrofitClient.authService.changePassword(request).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@SettingsActivity, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorBody = response.errorBody()?.string() ?: ""
                            val message = when {
                                errorBody.contains("password") || response.code() == 400 -> "현재 비밀번호가 일치하지 않거나 조건에 맞지 않습니다."
                                else -> "비밀번호 변경에 실패했습니다. (코드: ${response.code()})"
                            }
                            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(this@SettingsActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 로그아웃 확인 다이얼로그
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                performLogout()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 로그아웃 실제 수행 로직
    private fun performLogout() {
        // 휴대폰에 저장된 토큰 정보 삭제
        val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // 로그인 화면으로 이동 및 스택 클리어
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        Toast.makeText(this, "성공적으로 로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        // 현재 액티비티 종료
        finish()
    }

    // 계정 삭제 확인 다이얼로그
    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("계정 삭제")
            .setMessage("정말 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("탈퇴하기") { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 계정 삭제 실제 수행 로직
    private fun performDeleteAccount() {
        // 서버의 회원 탈퇴 엔드포인트 호출 (AuthInterceptor가 토큰을 자동으로 헤더에 담아줍니다)
        RetrofitClient.authService.deleteAccount().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // 기기에 저장된 토큰 정보 삭제
                    val sharedPref = getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()

                    Toast.makeText(this@SettingsActivity, "계정이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                    // 로그인 화면으로 이동 및 백스택 클리어
                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@SettingsActivity, "계정 삭제에 실패했습니다. (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}