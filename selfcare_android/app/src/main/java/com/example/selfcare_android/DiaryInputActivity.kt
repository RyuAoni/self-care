package com.example.selfcare_android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
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

    // UI部品
    private lateinit var inputField: EditText
    private lateinit var bottomNavigation: BottomNavigationView

    private val SUPABASE_CHAT_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/chat"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val voiceInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultData = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = resultData?.get(0)

            if (!spokenText.isNullOrEmpty()) {
                val currentText = inputField.text.toString()
                if (currentText.isNotEmpty()) {
                    inputField.setText("$currentText $spokenText")
                } else {
                    inputField.setText(spokenText)
                }
                inputField.setSelection(inputField.text.length)
            }
        }
    }

    private val requestMicrophonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startVoiceInput()
        } else {
            Toast.makeText(this, "音声入力にはマイク権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_input)

        year = intent.getIntExtra("year", 0)
        month = intent.getIntExtra("month", 0)
        day = intent.getIntExtra("day", 0)

        val recyclerView: RecyclerView = findViewById(R.id.chat_history_view)
        inputField = findViewById(R.id.message_input_field)
        val sendButton: ImageButton = findViewById(R.id.image_button_send)
        val voiceInputButton: ImageButton = findViewById(R.id.voiceInputButton)
        val saveButton: Button = findViewById(R.id.saveButton)
        val closeButton: ImageButton = findViewById(R.id.closeButton)
        val dateTitleText: TextView = findViewById(R.id.dateTitleText)

        dateTitleText.text = "${month + 1}月${day}日の日記"

        closeButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        saveButton.setOnClickListener {
            generateDiary()
        }

        messageAdapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = messageAdapter

        loadExistingDiary()

        if (messageList.isEmpty()) {
            val initialMessage = Message(
                text = "AI日記アシスタントです。今日はどんな一日でしたか？",
                type = MESSAGE_TYPE_AI
            )
            messageAdapter.addMessage(initialMessage)
            saveConversationToStorage()
        }

        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()

            if (text.isNotEmpty()) {
                val userMessage = Message(text = text, type = MESSAGE_TYPE_USER)
                messageAdapter.addMessage(userMessage)
                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)

                inputField.setText("")
                saveConversationToStorage()

                handleAiResponse(text)
            }
        }

        voiceInputButton.setOnClickListener {
            checkPermissionAndStartVoiceInput()
        }

        setupBottomNavigation()
        setupKeyboardListener()
        setCustomStatusBar()
    }

    override fun onResume() {
        super.onResume()
        // 画面に戻ってきたときにボトムナビゲーションを表示
        if (::bottomNavigation.isInitialized) {
            bottomNavigation.visibility = View.VISIBLE
        }
    }

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > 250) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE
            }
        }
    }

    private fun checkPermissionAndStartVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "お話しください")
            }
            voiceInputLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "このデバイスでは音声入力がサポートされていません", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAiResponse(userText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val validMessages = messageList.dropWhile { it.type == MESSAGE_TYPE_AI }

                val messagesToSend = validMessages.map { msg ->
                    val role = if (msg.type == MESSAGE_TYPE_USER) "user" else "model"
                    mapOf(
                        "role" to role,
                        "content" to msg.text
                    )
                }

                val requestData = mapOf(
                    "messages" to messagesToSend
                )

                val jsonString = Gson().toJson(requestData)

                val requestBody = jsonString
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(SUPABASE_CHAT_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")

                    val aiText = jsonResponse.optString("message", "（応答なし）")

                    withContext(Dispatchers.Main) {
                        val aiMessage = Message(text = aiText, type = MESSAGE_TYPE_AI)
                        messageAdapter.addMessage(aiMessage)
                        findViewById<RecyclerView>(R.id.chat_history_view)
                            .scrollToPosition(messageAdapter.itemCount - 1)

                        saveConversationToStorage()
                    }
                } else {
                    val errorMsg = "エラー: ${response.code}"
                    Log.e("DiaryInput", "API Error: $errorMsg")
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

    private fun loadExistingDiary() {
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)

        val appData = JsonDataManager.load(this)
        val entry = appData.diaries.find { it.date == dateString }

        if (entry != null) {
            messageList.clear()
            entry.conversations.forEach { convData ->
                messageList.add(convData.toMessage())
            }
            messageAdapter.notifyDataSetChanged()
        }
    }

    private fun saveConversationToStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, day)

            val conversationsToSave = messageList.map { it.toConversationData() }

            val appData = JsonDataManager.load(this@DiaryInputActivity)
            val existingEntryIndex = appData.diaries.indexOfFirst { it.date == dateString }

            if (existingEntryIndex != -1) {
                val oldEntry = appData.diaries[existingEntryIndex]
                val updatedEntry = oldEntry.copy(conversations = conversationsToSave)
                appData.diaries[existingEntryIndex] = updatedEntry
            } else {
                val newEntry = DiaryEntry(
                    id = UUID.randomUUID().toString(),
                    date = dateString,
                    conversations = conversationsToSave,
                    stepCount = "0",
                    diaryContent = "",
                    emotionScore = "0.0",
                    positiveScore = "0.0",
                    negativeScore = "0.0"
                )
                appData.diaries.add(newEntry)
            }

            JsonDataManager.save(this@DiaryInputActivity, appData)
        }
    }

    private fun generateDiary() {
        val conversationsForTransfer: List<ConversationData> = messageList.map { it.toConversationData() }
        val gson = Gson()
        val conversationsJson = gson.toJson(conversationsForTransfer)

        val intent = Intent(this, DiaryGenerateActivity::class.java).apply {
            putExtra("year", year)
            putExtra("month", month)
            putExtra("day", day)
            putExtra("EXTRA_CONVERSATIONS_JSON", conversationsJson)
        }

        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNavigation.menu.size()) {
            bottomNavigation.menu.getItem(i).isChecked = false
        }
        bottomNavigation.menu.setGroupCheckable(0, true, true)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }
}