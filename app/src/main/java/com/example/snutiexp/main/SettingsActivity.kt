package com.example.snutiexp.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snutiexp.databinding.ActivitySettingsBinding
import com.example.snutiexp.login.LoginActivity
import com.example.snutiexp.network.RetrofitClient

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
            Toast.makeText(this, "비밀번호 변경 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        // 4. 로그아웃 클릭 리스너
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // 5. 계정 삭제 클릭 리스너
        binding.btnDeleteAccount.setOnClickListener {
            Toast.makeText(this, "계정 삭제 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
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
}