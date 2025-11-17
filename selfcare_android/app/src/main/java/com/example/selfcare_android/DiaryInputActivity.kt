package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.LinkedList

class DiaryInputActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = LinkedList<Message>()
    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_input)

        // 日付情報を取得
        year = intent.getIntExtra("year", 0)
        month = intent.getIntExtra("month", 0)
        day = intent.getIntExtra("day", 0)

        // UIコンポーネントの取得
        val recyclerView: RecyclerView = findViewById(R.id.chat_history_view)
        val inputField: EditText = findViewById(R.id.message_input_field)
        val sendButton: ImageButton = findViewById(R.id.image_button_send)
        val saveButton: Button = findViewById(R.id.saveButton)
        val closeButton: ImageButton = findViewById(R.id.closeButton)
        val dateTitleText: TextView = findViewById(R.id.dateTitleText)

        // 日付をヘッダーに表示
        dateTitleText.text = "${month + 1}月${day}日の日記"

        // 閉じるボタン
        closeButton.setOnClickListener {
            finish()
        }

        // 保存ボタン
        saveButton.setOnClickListener {
            generateDiary()
        }

        // --- RecyclerViewの設定 ---
        messageAdapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // リストを下端に固定
        }
        recyclerView.adapter = messageAdapter

        // 既存の日記を読み込む
        loadExistingDiary()

        // --- 初期メッセージ（AIからの導入メッセージ） ---
        if (messageList.isEmpty()) {
            val initialMessage = Message(
                text = "AI日記アシスタントです。今日はどんな一日でしたか？会話形式で教えてください。",
                type = MESSAGE_TYPE_AI
            )
            messageAdapter.addMessage(initialMessage)
        }

        // --- 送信ボタンのクリック処理（文字送信機能のコア） ---
        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()

            if (text.isNotEmpty()) {
                // 1. ユーザーメッセージをリストに追加
                val userMessage = Message(text = text, type = MESSAGE_TYPE_USER)
                messageAdapter.addMessage(userMessage)
                recyclerView.scrollToPosition(messageAdapter.itemCount - 1) // 最新へスクロール

                // 2. 入力フィールドをクリア
                inputField.setText("")

                // 3. (APIが未実装のため) ダミーのAI応答を生成
                handleAiResponse(text)
            }
        }

        // ボトムナビゲーション設定
        setupBottomNavigation()
    }

    // AIの応答を処理するメソッド (API実装の置き換え場所)
    private fun handleAiResponse(userText: String) {
        // --- 将来的にAPIをコールする場所 ---

        // 現時点では、即座にダミーのAIメッセージを返す
        val aiResponseText = "なるほど、${userText}。それは楽しそうですね。詳しく聞かせてください。"

        // ダミーメッセージをリストに追加
        val aiMessage = Message(text = aiResponseText, type = MESSAGE_TYPE_AI)
        messageAdapter.addMessage(aiMessage)

        // 最新のメッセージにスクロール
        findViewById<RecyclerView>(R.id.chat_history_view).scrollToPosition(messageAdapter.itemCount - 1)
    }

    private fun loadExistingDiary() {
        val prefs = getSharedPreferences("DiaryData", MODE_PRIVATE)
        val key = "${year}_${month}_${day}"

        // 保存されたメッセージ数を取得
        val messageCount = prefs.getInt("${key}_count", 0)

        // メッセージを読み込む
        for (i in 0 until messageCount) {
            val text = prefs.getString("${key}_msg_${i}_text", "") ?: ""
            val type = prefs.getInt("${key}_msg_${i}_type", MESSAGE_TYPE_USER)
            if (text.isNotEmpty()) {
                messageList.add(Message(text = text, type = type))
            }
        }

        if (messageList.isNotEmpty()) {
            messageAdapter.notifyDataSetChanged()
        }
    }

    private fun generateDiary() {
        val prefs = getSharedPreferences("DiaryData", MODE_PRIVATE)
        val editor = prefs.edit()
        val key = "${year}_${month}_${day}"

        // メッセージ数を保存
        editor.putInt("${key}_count", messageList.size)

        // 各メッセージを保存
        messageList.forEachIndexed { index, message ->
            editor.putString("${key}_msg_${index}_text", message.text)
            editor.putInt("${key}_msg_${index}_type", message.type)
        }

        editor.apply()

        // DiaryGenerateActivity に遷移
        val intent = Intent(this, DiaryGenerateActivity::class.java)
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
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
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
}
