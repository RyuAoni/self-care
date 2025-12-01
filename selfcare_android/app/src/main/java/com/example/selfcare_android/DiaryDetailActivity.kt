package com.example.selfcare_android

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
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
    private lateinit var tvDateTitle: TextView // 日付タイトル

    // アダプター
    private lateinit var messageAdapter: MessageAdapter

    // デモモード管理
    private var isDemoMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        // デモモード設定の読み込み
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        isDemoMode = prefs.getBoolean("demo_mode", false)

        year = intent.getIntExtra("year", 0)
        month = intent.getIntExtra("month", 0)
        day = intent.getIntExtra("day", 0)

        setupViews()
        setupTopBar(year, month, day)
        setupRecyclerView()
        setupBottomNavigation()
        loadDiaryData(year, month, day)
        setupDemoFeatures() // ★追加: デモ機能
    }

    private fun setupViews() {
        // ※レイアウトファイルのIDに合わせて変更してください
        // 元のコードで diaryContentRecyclerView となっていた場所は TextView であるべきです
        tvDiaryContent = findViewById(R.id.diaryContentRecyclerView)

        // 会話履歴表示用のRecyclerView (レイアウトに追加が必要です)
        chatRecyclerView = findViewById(R.id.chatHistoryRecyclerView) // ★ID要確認

        tvStepCount = findViewById(R.id.stepCountText) // ★ID要確認
        imgEmotion = findViewById(R.id.emotionIcon) // ★ID要確認
        tvDateTitle = findViewById(R.id.dateTitle)
    }

    private fun setupTopBar(year: Int, month: Int, day: Int) {
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
        updateDateTitle(year, month, day)
    }

    private fun updateDateTitle(year: Int, month: Int, day: Int) {
        tvDateTitle.text = "${month + 1}月${day}日の日記"
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(mutableListOf())
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter
    }

    // ★追加: デモモード用の機能
    private fun setupDemoFeatures() {
        if (isDemoMode) {
            // 1. 日付をタップして変更（日記データを別の日付に移動）
            tvDateTitle.setOnClickListener {
                showDatePickerDialog()
            }
            tvDateTitle.setTextColor(android.graphics.Color.parseColor("#FF5722")) // 色でわかるように

            // 2. 歩数をタップして変更
            tvStepCount.setOnClickListener {
                showStepEditDialog()
            }
            tvStepCount.setTextColor(android.graphics.Color.parseColor("#FF5722"))
        }
    }

    // 日付変更ダイアログ
    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, newYear, newMonth, newDay ->
                moveDiaryDate(newYear, newMonth, newDay)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // 日記の日付データを移動する処理
    private fun moveDiaryDate(newYear: Int, newMonth: Int, newDay: Int) {
        val oldDateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)
        val newDateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", newYear, newMonth + 1, newDay)

        val appData = JsonDataManager.load(this)
        val targetEntry = appData.diaries.find { it.date == oldDateString }

        if (targetEntry != null) {
            // 日付を書き換えた新しいデータを作成
            val updatedEntry = targetEntry.copy(date = newDateString)

            // リスト内のデータを入れ替え（または削除して追加）
            val index = appData.diaries.indexOf(targetEntry)
            if (index != -1) {
                appData.diaries[index] = updatedEntry
                JsonDataManager.save(this, appData)

                // 画面の変数を更新して再表示
                year = newYear
                month = newMonth
                day = newDay
                updateDateTitle(year, month, day)
                loadDiaryData(year, month, day)

                Toast.makeText(this, "日付を移動しました（デモ）", Toast.LENGTH_SHORT).show()
            }
        } else {
            // データがない場合でも、画面上の日付だけ更新（空の状態で移動）
            year = newYear
            month = newMonth
            day = newDay
            updateDateTitle(year, month, day)
            loadDiaryData(year, month, day)
        }
    }

    // 歩数変更ダイアログ
    private fun showStepEditDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "歩数を入力"

        AlertDialog.Builder(this)
            .setTitle("【デモ】歩数を変更")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newSteps = input.text.toString()
                if (newSteps.isNotEmpty()) {
                    updateDiarySteps(newSteps)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // 歩数データを更新する処理
    private fun updateDiarySteps(newSteps: String) {
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)
        val appData = JsonDataManager.load(this)
        val targetEntry = appData.diaries.find { it.date == dateString }

        if (targetEntry != null) {
            val updatedEntry = targetEntry.copy(stepCount = newSteps)
            val index = appData.diaries.indexOf(targetEntry)
            if (index != -1) {
                appData.diaries[index] = updatedEntry
                JsonDataManager.save(this, appData)

                // 画面更新
                loadDiaryData(year, month, day)
                Toast.makeText(this, "歩数を変更しました（デモ）", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "データがないため変更できません", Toast.LENGTH_SHORT).show()
        }
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
                    val intent = Intent(this, CalendarActivity::class.java)
                    // スタックをクリアしてカレンダーに戻る
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
//                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 設定が変わっている可能性があるのでデモモード再確認
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        isDemoMode = prefs.getBoolean("demo_mode", false)

        // 日記が更新されている可能性があるので再読み込み
        loadDiaryData(year, month, day)
        setupDemoFeatures() // デモ機能の再セットアップ
    }
}