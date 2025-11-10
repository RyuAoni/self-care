package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // activity_main.xml を読み込む
        setContentView(R.layout.activity_main)

        // 「日記入力画面へ」ボタン（ID: button_start_diary）を取得
        val startDiaryButton: Button = findViewById(R.id.button_start_diary)

        // ボタンがクリックされたときの処理を設定
        startDiaryButton.setOnClickListener {
            // DiaryInputActivityに遷移するためのIntentを作成し、起動
            val intent = Intent(this, DiaryInputActivity::class.java)
            startActivity(intent)
        }
    }
}