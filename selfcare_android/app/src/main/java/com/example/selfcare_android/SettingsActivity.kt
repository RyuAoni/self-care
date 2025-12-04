package com.example.selfcare_android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
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

        // 名前をタップして編集できるように設定
        findViewById<android.widget.TextView>(R.id.nameText).setOnClickListener {
            showEditNameDialog()
        }

        // プロフィール画像をタップして編集できるように設定
        findViewById<ImageView>(R.id.profileImage).setOnClickListener {
            showEditProfileImageDialog()
        }

        val window = window
        val skyBlue = Color.parseColor("#87CEEB")

        // ステータスバーをこの画面だけスカイブルーにする
        window.statusBarColor = skyBlue
    }

    private fun setupMenuButtons() {
        // 週次お手紙ボタン
        findViewById<CardView>(R.id.weeklyLetterButton).setOnClickListener {
            startActivity(Intent(this, WeeklyLetterActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // アルバムボタン
        findViewById<CardView>(R.id.albumButton).setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // 設定ボタン（現在の画面なので何もしない or 設定詳細へ）
        findViewById<CardView>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsDetailActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
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

    private fun showEditNameDialog() {
        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val currentName = sharedPreferences.getString("user_name", "はるさめべーこん")

        // EditTextを作成
        val editText = EditText(this)
        editText.setText(currentName)
        editText.setPadding(50, 40, 50, 40)

        // ダイアログを作成
        AlertDialog.Builder(this)
            .setTitle("名前を編集")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // SharedPreferencesに保存
                    sharedPreferences.edit().apply {
                        putString("user_name", newName)
                        apply()
                    }
                    // 表示を更新
                    loadProfileData()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showEditProfileImageDialog() {
        // TODO: プロフィール画像の編集機能を実装
        AlertDialog.Builder(this)
            .setTitle("プロフィール画像")
            .setMessage("プロフィール画像の変更機能は今後実装予定です")
            .setPositiveButton("OK", null)
            .show()
    }
}