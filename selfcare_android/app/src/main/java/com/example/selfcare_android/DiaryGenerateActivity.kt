package com.example.selfcare_android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

//  日記生成画面
class DiaryGenerateActivity : AppCompatActivity() {

    // UI部品
    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etDiaryContent: EditText
    private lateinit var btnImage: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnSave: Button

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
    }

    // 日記保存処理
    private fun saveDiary() {
        val content = etDiaryContent.text.toString()

        if (content.isNotEmpty()) {
            Toast.makeText(this, "日記を保存しました", Toast.LENGTH_SHORT).show()
            //Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
        } else {
            Toast.makeText(this, "内容を入力してください", Toast.LENGTH_SHORT).show()
        }
    }
}
