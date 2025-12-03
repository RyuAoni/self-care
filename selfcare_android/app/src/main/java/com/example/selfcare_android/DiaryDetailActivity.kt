package com.example.selfcare_android

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
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
    private lateinit var tvDateTitle: TextView
    private lateinit var imageCardView: CardView
    private lateinit var diaryImageView: ImageView

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
        setupDemoFeatures()
    }

    private fun setupViews() {
        tvDiaryContent = findViewById(R.id.diaryContentRecyclerView)
        chatRecyclerView = findViewById(R.id.chatHistoryRecyclerView)
        tvStepCount = findViewById(R.id.stepCountText)
        imgEmotion = findViewById(R.id.emotionIcon)
        tvDateTitle = findViewById(R.id.dateTitle)
        imageCardView = findViewById(R.id.imageCardView)
        diaryImageView = findViewById(R.id.diaryImageView)
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
        chatRecyclerView.isNestedScrollingEnabled = false
    }

    private fun setupDemoFeatures() {
        if (isDemoMode) {
            tvDateTitle.setOnClickListener {
                showDatePickerDialog()
            }
            tvDateTitle.setTextColor(android.graphics.Color.parseColor("#FF5722"))

            tvStepCount.setOnClickListener {
                showStepEditDialog()
            }
            tvStepCount.setTextColor(android.graphics.Color.parseColor("#FF5722"))
        }
    }

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

    private fun moveDiaryDate(newYear: Int, newMonth: Int, newDay: Int) {
        val oldDateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)
        val newDateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", newYear, newMonth + 1, newDay)

        val appData = JsonDataManager.load(this)
        val targetEntry = appData.diaries.find { it.date == oldDateString }

        if (targetEntry != null) {
            val updatedEntry = targetEntry.copy(date = newDateString)
            val index = appData.diaries.indexOf(targetEntry)
            if (index != -1) {
                appData.diaries[index] = updatedEntry
                JsonDataManager.save(this, appData)

                year = newYear
                month = newMonth
                day = newDay
                updateDateTitle(year, month, day)
                loadDiaryData(year, month, day)

                Toast.makeText(this, "日付を移動しました（デモ）", Toast.LENGTH_SHORT).show()
            }
        } else {
            year = newYear
            month = newMonth
            day = newDay
            updateDateTitle(year, month, day)
            loadDiaryData(year, month, day)
        }
    }

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

                loadDiaryData(year, month, day)
                Toast.makeText(this, "歩数を変更しました（デモ）", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "データがないため変更できません", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDiaryData(year: Int, month: Int, day: Int) {
        val targetDate = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)
        val appData = JsonDataManager.load(this)
        val entry = appData.diaries.find { it.date == targetDate }

        if (entry != null) {
            // 日記本文の表示
            tvDiaryContent.text = entry.diaryContent
            tvDiaryContent.setTextColor(android.graphics.Color.parseColor("#000000"))

            // 歩数の表示
            tvStepCount.text = "${entry.stepCount} 歩"

            // 感情アイコンの表示
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

            // 画像の表示
            if (!entry.imagePath.isNullOrEmpty()) {
                val imageFile = File(entry.imagePath)
                if (imageFile.exists()) {
                    imageCardView.visibility = View.VISIBLE
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    diaryImageView.setImageBitmap(bitmap)
                } else {
                    imageCardView.visibility = View.GONE
                }
            } else {
                imageCardView.visibility = View.GONE
            }

            // 会話履歴の表示
            val messages = entry.conversations.map { conversation ->
                conversation.toMessage()
            }
            messageAdapter.setMessages(messages)

        } else {
            // データがない場合
            tvDiaryContent.text = "この日の日記はまだありません"
            tvDiaryContent.setTextColor(android.graphics.Color.parseColor("#999999"))
            tvStepCount.text = "- 歩"
            imgEmotion.visibility = View.INVISIBLE
            imageCardView.visibility = View.GONE
            messageAdapter.setMessages(emptyList())
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
        isDemoMode = prefs.getBoolean("demo_mode", false)

        loadDiaryData(year, month, day)
        setupDemoFeatures()
    }
}