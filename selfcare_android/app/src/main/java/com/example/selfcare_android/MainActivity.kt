package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

//仮のホーム画面
// メイン画面（トップ画面）
class MainActivity : AppCompatActivity() {

    private lateinit var btnGoToGenerate: Button // 日記作成ボタン

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()       // UI初期化
        setupListeners()  // ボタンの動作設定
    }

    // UI部品をIDと関連付け
    private fun initViews() {
        btnGoToGenerate = findViewById(R.id.btnGoToGenerate)
    }

    // ボタンのクリック処理
    private fun setupListeners() {
        btnGoToGenerate.setOnClickListener {
            // 日記生成画面へ遷移
            val intent = Intent(this, DiaryGenerateActivity::class.java)
            startActivity(intent)
        }
    }
}
