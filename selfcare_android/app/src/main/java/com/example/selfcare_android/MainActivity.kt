package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnGoToGenerate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnGoToGenerate = findViewById<Button>(R.id.btnGoToGenerate)
    }

    private fun setupListeners() {
        // 日記生成画面への遷移
        btnGoToGenerate.setOnClickListener {
            val intent = Intent(this, DiaryGenerateActivity::class.java)
            startActivity(intent)
        }
    }
}