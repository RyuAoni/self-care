package com.example.selfcare_android // 修正点 1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DiaryGenerateActivity : AppCompatActivity() { // 修正点 2

    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etDiaryContent: EditText
    private lateinit var btnImage: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnSave: Button
    private lateinit var btnStats: ImageButton
    private lateinit var btnCalendar: ImageButton
    private lateinit var btnProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_generate) // 修正点 3

        // ビューの初期化
        initViews()

        // イベントリスナーの設定
        setupListeners()
    }

    private fun initViews() {
        // IDは activity_diary_generate.xml と一致しているため、ここは修正不要です
        btnClose = findViewById(R.id.btnClose)
        tvTitle = findViewById(R.id.tvTitle)
        etDiaryContent = findViewById(R.id.etDiaryContent)
        btnImage = findViewById(R.id.btnImage)
        btnEdit = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)
        btnStats = findViewById(R.id.btnStats)
        btnCalendar = findViewById(R.id.btnCalendar)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setupListeners() {
        // 閉じるボタン
        btnClose.setOnClickListener {
            finish()
        }

        // 画像ボタン
        btnImage.setOnClickListener {
            Toast.makeText(this, "画像を追加", Toast.LENGTH_SHORT).show()
            // 画像選択の処理を追加
        }

        // 編集ボタン
        btnEdit.setOnClickListener {
            Toast.makeText(this, "編集モード", Toast.LENGTH_SHORT).show()
            // 編集モードの処理を追加
        }

        // 保存ボタン
        btnSave.setOnClickListener {
            saveDiary()
        }

        // 統計ボタン
        btnStats.setOnClickListener {
            Toast.makeText(this, "統計画面", Toast.LENGTH_SHORT).show()
            // 統計画面への遷移処理
        }

        // カレンダーボタン
        btnCalendar.setOnClickListener {
            Toast.makeText(this, "カレンダー画面", Toast.LENGTH_SHORT).show()
            // カレンダー画面への遷移処理
        }

        // プロフィールボタン
        btnProfile.setOnClickListener {
            Toast.makeText(this, "プロフィール画面", Toast.LENGTH_SHORT).show()
            // プロフィール画面への遷移処理
        }
    }

    private fun saveDiary() {
        val content = etDiaryContent.text.toString()

        if (content.isNotEmpty()) {
            // 保存
            Toast.makeText(this, "日記を保存しました", Toast.LENGTH_SHORT).show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish()
            }, 2000) // 2000ミリ秒 = 2秒
        } else {
            Toast.makeText(this, "内容を入力してください", Toast.LENGTH_SHORT).show()
        }
    }
}