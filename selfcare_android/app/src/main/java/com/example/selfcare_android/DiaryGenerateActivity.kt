package com.example.selfcare_android

//画面遷移(Intent)とフッター(BottomNavigationView)のためにインポート
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

//  日記生成画面
class DiaryGenerateActivity : AppCompatActivity() {

    // UI部品
    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etDiaryContent: EditText
    private lateinit var btnImage: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnSave: Button
    private lateinit var bottomNav: BottomNavigationView //フッターの宣言

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_generate) // レイアウト適用

        initViews()       // UI初期化
        setupListeners()  // ボタン動作設定
    }

    // UI部品をIDと結びつける
    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        tvTitle = findViewById(R.id.tvTitle)
        etDiaryContent = findViewById(R.id.etDiaryContent)
        btnImage = findViewById(R.id.btnImage)
        btnEdit = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)

        // ★AI追加: フッターの初期化
        bottomNav = findViewById(R.id.bottom_navigation)
    }

    // 各ボタンのクリック処理
    private fun setupListeners() {
        btnClose.setOnClickListener { finish() } // 閉じる

        btnImage.setOnClickListener {
            Toast.makeText(this, "画像を追加", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            Toast.makeText(this, "編集モード", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { saveDiary() }

        // フッターのクリック処理
        // 初期選択を解除する
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // カレンダー
                R.id.nav_calendar -> {
                    // DiaryInputActivity への画面遷移
                    val intent = Intent(this, DiaryInputActivity::class.java)
                    startActivity(intent)
                    true
                }
                // 統計
                R.id.nav_stats -> {
                    Toast.makeText(this, "統計画面", Toast.LENGTH_SHORT).show()
                    true
                }
                // プロフィール
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // 日記保存処理
    private fun saveDiary() {
        val content = etDiaryContent.text.toString()

        Toast.makeText(this, "日記を保存しました", Toast.LENGTH_SHORT).show()

        // 保存処理が終わったあとにCalendarActivityへ移動
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
            finish() // 現在の画面を閉じる
        }, 1500) // トースト表示を少し見せるための遅延
    }
}
