package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // ボタンのクリックリスナー設定
        setupMenuButtons()

        // ボトムナビゲーションの設定
        setupBottomNavigation()
    }

    private fun setupMenuButtons() {
        // 週次お手紙ボタン
        findViewById<CardView>(R.id.weeklyLetterButton).setOnClickListener {
            // TODO: 週次お手紙画面へ遷移
            startActivity(Intent(this, WeeklyLetterActivity::class.java))
        }

        // アルバムボタン
        findViewById<CardView>(R.id.albumButton).setOnClickListener {
            // TODO: アルバム画面へ遷移
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        // 設定ボタン（現在の画面なので何もしない or 設定詳細へ）
        findViewById<CardView>(R.id.settingsButton).setOnClickListener {
            // TODO: 設定詳細画面へ遷移
            startActivity(Intent(this, SettingsDetailActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    true
                }
                else -> false
            }
        }
        // profileを選択状態にする
        bottomNav.selectedItemId = R.id.nav_profile
    }

    override fun onResume() {
        super.onResume()
        // プロフィール情報を更新
        loadProfileData()
    }

    private fun loadProfileData() {
        // SharedPreferencesからプロフィール情報を読み込み
        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_name", "はるさめべーこん")

        // 名前を表示
        findViewById<android.widget.TextView>(R.id.nameText).text = "名前 $userName"

        // TODO: プロフィール画像の読み込み
        // val profileImageUri = sharedPreferences.getString("profile_image_uri", null)
        // profileImageUri?.let {
        //     findViewById<ImageView>(R.id.profileImage).setImageURI(Uri.parse(it))
        // }
    }
}
