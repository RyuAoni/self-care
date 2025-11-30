package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson // 【追加】JSONライブラリをインポート
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.LinkedList
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class DiaryInputActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = LinkedList<Message>()
    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    // ★追加: Supabaseのチャット関数のURL
    // 必ず自分のSupabaseのURLに書き換えてください！
    private val SUPABASE_CHAT_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/chat"

    // ★追加: 通信クライアント (タイムアウトを少し長めに設定)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
                text = "AI日記アシスタントです。今日はどんな一日でしたか？",
                type = MESSAGE_TYPE_AI
            )
            messageAdapter.addMessage(initialMessage)
            saveConversationToStorage()
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
                // ★追加: ユーザーのメッセージを保存
                saveConversationToStorage()

                // 2. ★変更: SupabaseのAIと会話する
                handleAiResponse(text)
            }
        }

        // ボトムナビゲーション設定
        setupBottomNavigation()
    }

    // AIの応答を処理するメソッド (API実装の置き換え場所)
    private fun handleAiResponse(userText: String) {
        // 通信中は「入力中...」などを出しても良いですが、今回はシンプルに非同期で実行

        // CoroutineScopeでバックグラウンド処理を開始
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 送信データの作成 (修正: Gsonだけで完結させる)
                // ★重要: Geminiは「会話の開始はユーザーから」というルールがあるため、
                // messageListの先頭にあるAIの挨拶メッセージなどは除外して送ります。
                val validMessages = messageList.dropWhile { it.type == MESSAGE_TYPE_AI }

                val messagesToSend = validMessages.map { msg ->
                    val role = if (msg.type == MESSAGE_TYPE_USER) "user" else "model"
                    mapOf(
                        "role" to role,
                        "content" to msg.text
                    )
                }

                // 送信データ全体を作る
                val requestData = mapOf(
                    "messages" to messagesToSend
                )

                // Gsonを使ってJSON文字列に変換
                val jsonString = Gson().toJson(requestData)

                val requestBody = jsonString
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                // 2. リクエストの作成
                val request = Request.Builder()
                    .url(SUPABASE_CHAT_URL)
                    .post(requestBody)
                    .build()

                // 3. API呼び出し実行
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")

                    val aiText = jsonResponse.optString("message", "（応答なし）")

                    // 4. メインスレッドに戻って画面を更新
                    withContext(Dispatchers.Main) {
                        val aiMessage = Message(text = aiText, type = MESSAGE_TYPE_AI)
                        messageAdapter.addMessage(aiMessage)
                        findViewById<RecyclerView>(R.id.chat_history_view)
                            .scrollToPosition(messageAdapter.itemCount - 1)

                        // AIのメッセージも保存
                        saveConversationToStorage()
                    }
                } else {
                    val errorMsg = "エラー: ${response.code}"
                    Log.e("DiaryInput", "API Error: $errorMsg")
                    // エラーの詳細をログに出す
                    Log.e("DiaryInput", "Error Body: ${response.body?.string()}")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DiaryInputActivity, "AIの応答に失敗しました ($errorMsg)", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DiaryInputActivity,
                        "通信エラーが発生しました",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

//    private fun loadExistingDiary() {
//        val prefs = getSharedPreferences("DiaryData", MODE_PRIVATE)
//        val key = "${year}_${month}_${day}"
//
//        // 保存されたメッセージ数を取得
//        val messageCount = prefs.getInt("${key}_count", 0)
//
//        // メッセージを読み込む
//        for (i in 0 until messageCount) {
//            val text = prefs.getString("${key}_msg_${i}_text", "") ?: ""
//            val type = prefs.getInt("${key}_msg_${i}_type", MESSAGE_TYPE_USER)
//            if (text.isNotEmpty()) {
//                messageList.add(Message(text = text, type = type))
//            }
//        }
//
//        if (messageList.isNotEmpty()) {
//            messageAdapter.notifyDataSetChanged()
//        }
//    }
    private fun loadExistingDiary() {
        // 今日の日付文字列を作成 (保存時と同じフォーマット)
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)

        val appData = JsonDataManager.load(this)
        val entry = appData.diaries.find { it.date == dateString }

        if (entry != null) {
            // 保存されている会話データを読み込む
            messageList.clear()
            entry.conversations.forEach { convData ->
                messageList.add(convData.toMessage())
            }
            messageAdapter.notifyDataSetChanged()
        }
    }

    private fun saveConversationToStorage() {
        // 非同期（バックグラウンド）で保存処理を行う
        CoroutineScope(Dispatchers.IO).launch {
            val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)

            // 現在のリストを保存用データ型に変換
            val conversationsToSave = messageList.map { it.toConversationData() }

            val appData = JsonDataManager.load(this@DiaryInputActivity)
            val existingEntryIndex = appData.diaries.indexOfFirst { it.date == dateString }

            if (existingEntryIndex != -1) {
                // 既に日記データがある場合は、会話部分だけ更新する
                val oldEntry = appData.diaries[existingEntryIndex]
                val updatedEntry = oldEntry.copy(conversations = conversationsToSave)
                appData.diaries[existingEntryIndex] = updatedEntry
            } else {
                // データがない場合は新規作成
                val newEntry = DiaryEntry(
                    id = UUID.randomUUID().toString(),
                    date = dateString,
                    conversations = conversationsToSave,
                    stepCount = "0",     // 仮の値
                    diaryContent = "",   // まだ生成していないので空
                    emotionScore = "0.0",
                    positiveScore = "0.0",
                    negativeScore = "0.0"
                )
                appData.diaries.add(newEntry)
            }

            // ファイルに書き込み
            JsonDataManager.save(this@DiaryInputActivity, appData)
        }
    }

    private fun generateDiary() {
        val conversationsForTransfer: List<ConversationData> = messageList.map { it.toConversationData() }
        val gson = Gson()
        val conversationsJson = gson.toJson(conversationsForTransfer)
//        val prefs = getSharedPreferences("DiaryData", MODE_PRIVATE)
//        val editor = prefs.edit()
//        val key = "${year}_${month}_${day}"

        // 3. DiaryGenerateActivity に遷移
        val intent = Intent(this, DiaryGenerateActivity::class.java).apply {
            // 日付情報を渡す
            putExtra("year", year)
            putExtra("month", month)
            putExtra("day", day)
            // 会話履歴のJSON文字列を渡す
            putExtra("EXTRA_CONVERSATIONS_JSON", conversationsJson)
        }

        // メッセージ数を保存
//        editor.putInt("${key}_count", messageList.size)
//
//        // 各メッセージを保存
//        messageList.forEachIndexed { index, message ->
//            editor.putString("${key}_msg_${index}_text", message.text)
//            editor.putInt("${key}_msg_${index}_type", message.type)
//        }
//
//        editor.apply()
//
//        // DiaryGenerateActivity に遷移
//        val intent = Intent(this, DiaryGenerateActivity::class.java)
//        intent.putExtra("year", year)
//        intent.putExtra("month", month)
//        intent.putExtra("day", day)
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
