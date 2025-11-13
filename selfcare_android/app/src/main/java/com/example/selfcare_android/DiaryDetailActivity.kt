package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class DiaryDetailActivity : AppCompatActivity() {

    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        year = intent.getIntExtra("year", 0)
        month = intent.getIntExtra("month", 0)
        day = intent.getIntExtra("day", 0)

        setupTopBar(year, month, day)
        loadDiaryData(year, month, day)
        setupBottomNavigation()
    }

    private fun setupTopBar(year: Int, month: Int, day: Int) {
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.dateTitle).text =
            "${month + 1}月${day}日の日記"
    }

    private fun loadDiaryData(year: Int, month: Int, day: Int) {
        val prefs = getSharedPreferences("DiaryData", MODE_PRIVATE)
        val key = "${year}_${month}_${day}"

        // 日記テキストを結合して表示
        val messageCount = prefs.getInt("${key}_count", 0)
        val diaryText = StringBuilder()

        for (i in 0 until messageCount) {
            val text = prefs.getString("${key}_msg_${i}_text", "") ?: ""
            val type = prefs.getInt("${key}_msg_${i}_type", MESSAGE_TYPE_USER)

            // ユーザーのメッセージのみを表示
            if (type == MESSAGE_TYPE_USER && text.isNotEmpty()) {
                if (diaryText.isNotEmpty()) {
                    diaryText.append("\n\n")
                }
                diaryText.append(text)
            }
        }

        val diaryContentText = findViewById<TextView>(R.id.diaryContentRecyclerView)
        if (diaryText.isEmpty()) {
            diaryContentText.text = "この日の日記はまだありません"
            diaryContentText.setTextColor(android.graphics.Color.parseColor("#999999"))
        } else {
            diaryContentText.text = diaryText.toString()
            diaryContentText.setTextColor(android.graphics.Color.parseColor("#000000"))
        }

        // TODO: 歩数データの読み込み
        // val stepCount = prefs.getInt("${key}_steps", 0)
        // findViewById<TextView>(R.id.stepCountText).text = "${stepCount}歩"

        // TODO: 感情データの読み込みと表示
        // val emotion = prefs.getString("${key}_emotion", null)
        // if (emotion != null) {
        //     val emotionIcon = findViewById<ImageView>(R.id.emotionIcon)
        //     emotionIcon.visibility = View.VISIBLE
        //     loadEmotionIcon(emotion, emotionIcon)
        // }
    }

    private fun openEditScreen() {
        val intent = Intent(this, DiaryInputActivity::class.java)
        intent.putExtra("year", year)
        intent.putExtra("month", month)
        intent.putExtra("day", day)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // 初期選択を解除する
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    // TODO: 統計画面へ遷移
                    true
                }
                R.id.nav_calendar -> {
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 日記が更新されている可能性があるので再読み込み
        loadDiaryData(year, month, day)
    }
}