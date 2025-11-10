package com.example.selfcare_android

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.LinkedList

class DiaryInputActivity : AppCompatActivity() {

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = LinkedList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_input)

        // UIコンポーネントの取得
        val recyclerView: RecyclerView = findViewById(R.id.chat_history_view)
        val inputField: EditText = findViewById(R.id.message_input_field)
        val sendButton: ImageButton = findViewById(R.id.image_button_send)
        // ※ activity_diary_input.xml の送信ボタンにIDを付けていないため、
        // 仮にR.id.image_button_sendとします。IDが異なる場合は修正が必要です。

        // --- RecyclerViewの設定 ---
        messageAdapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // リストを下端に固定
        }
        recyclerView.adapter = messageAdapter

        // --- 初期メッセージ（AIからの導入メッセージ） ---
        val initialMessage = Message(
            text = "AI日記アシスタントです。今日はどんな一日でしたか？会話形式で教えてください。",
            type = MESSAGE_TYPE_AI
        )
        messageAdapter.addMessage(initialMessage)

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
}