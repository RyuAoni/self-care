package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class DiaryDetailActivity : AppCompatActivity() {

    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    // UI部品
    private lateinit var tvDiaryContent: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var tvStepCount: TextView
    private lateinit var imgEmotion: ImageView

    // アダプター
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        year = intent.getIntExtra("year", 0)
        month = intent.getIntExtra("month", 0)
        day = intent.getIntExtra("day", 0)

        setupViews()
        setupTopBar(year, month, day)
        setupRecyclerView()
        setupBottomNavigation()
        loadDiaryData(year, month, day)
    }

    private fun setupViews() {
        // ※レイアウトファイルのIDに合わせて変更してください
        // 元のコードで diaryContentRecyclerView となっていた場所は TextView であるべきです
        tvDiaryContent = findViewById(R.id.diaryContentRecyclerView)

        // 会話履歴表示用のRecyclerView (レイアウトに追加が必要です)
        chatRecyclerView = findViewById(R.id.chatHistoryRecyclerView) // ★ID要確認

        tvStepCount = findViewById(R.id.stepCountText) // ★ID要確認
        imgEmotion = findViewById(R.id.emotionIcon) // ★ID要確認
    }

    private fun setupTopBar(year: Int, month: Int, day: Int) {
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.dateTitle).text =
            "${month + 1}月${day}日の日記"
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(mutableListOf())
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter
    }

    private fun loadDiaryData(year: Int, month: Int, day: Int) {
        // 1. 保存時と同じ形式の日付文字列を作成 (例: "2025/11/11")
        val targetDate = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)

        // 2. JSONデータをロード
        val appData = JsonDataManager.load(this)

        // 3. 日付が一致する日記を探す
        val entry = appData.diaries.find { it.date == targetDate }

        if (entry != null) {
            // --- 日記本文の表示 ---
            tvDiaryContent.text = entry.diaryContent
            tvDiaryContent.setTextColor(android.graphics.Color.parseColor("#000000"))

            // --- 歩数の表示 ---
            tvStepCount.text = "${entry.stepCount} 歩"

            // --- 感情アイコンの表示 ---
            imgEmotion.visibility = View.VISIBLE
            val score = entry.emotionScore.toDoubleOrNull() ?: 0.0
            val iconRes = when {
                score >= 0.6 -> R.drawable.emoji_very_happy
                score >= 0.2 -> R.drawable.emoji_happy
                score >= -0.2 -> R.drawable.emoji_neutral
                score >= -0.6 -> R.drawable.emoji_sad
                else -> R.drawable.emoji_very_sad
            }
            imgEmotion.setImageResource(iconRes)

            // --- 会話履歴の表示 ---
            // ConversationData ("user"/"ai") を Message (0/1) に変換
            val messages = entry.conversations.map { conversation ->
                // AppModels.kt に定義した拡張関数を使用
                conversation.toMessage()
            }
            messageAdapter.setMessages(messages)

        } else {
            // データがない場合
            tvDiaryContent.text = "この日の日記はまだありません"
            tvDiaryContent.setTextColor(android.graphics.Color.parseColor("#999999"))
            tvStepCount.text = "- 歩"
            imgEmotion.visibility = View.INVISIBLE
            messageAdapter.setMessages(emptyList())
        }

//        // 日記テキストを結合して表示
//        val messageCount = prefs.getInt("${key}_count", 0)
//        val diaryText = StringBuilder()
//
//        for (i in 0 until messageCount) {
//            val text = prefs.getString("${key}_msg_${i}_text", "") ?: ""
//            val type = prefs.getInt("${key}_msg_${i}_type", MESSAGE_TYPE_USER)
//
//            // ユーザーのメッセージのみを表示
//            if (type == MESSAGE_TYPE_USER && text.isNotEmpty()) {
//                if (diaryText.isNotEmpty()) {
//                    diaryText.append("\n\n")
//                }
//                diaryText.append(text)
//            }
//        }
//
//        val diaryContentText = findViewById<TextView>(R.id.diaryContentRecyclerView)
//        if (diaryText.isEmpty()) {
//            diaryContentText.text = "この日の日記はまだありません"
//            diaryContentText.setTextColor(android.graphics.Color.parseColor("#999999"))
//        } else {
//            diaryContentText.text = diaryText.toString()
//            diaryContentText.setTextColor(android.graphics.Color.parseColor("#000000"))
//        }
//
//        // TODO: 歩数データの読み込み
//        // val stepCount = prefs.getInt("${key}_steps", 0)
//        // findViewById<TextView>(R.id.stepCountText).text = "${stepCount}歩"
//
//        // TODO: 感情データの読み込みと表示
//        // val emotion = prefs.getString("${key}_emotion", null)
//        // if (emotion != null) {
//        //     val emotionIcon = findViewById<ImageView>(R.id.emotionIcon)
//        //     emotionIcon.visibility = View.VISIBLE
//        //     loadEmotionIcon(emotion, emotionIcon)
//        // }
    }

//    private fun openEditScreen() {
//        val intent = Intent(this, DiaryInputActivity::class.java)
//        intent.putExtra("year", year)
//        intent.putExtra("month", month)
//        intent.putExtra("day", day)
//        startActivity(intent)
//    }

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
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
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