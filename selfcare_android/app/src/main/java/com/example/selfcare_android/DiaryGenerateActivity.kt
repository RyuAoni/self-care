package com.example.selfcare_android

//画面遷移(Intent)とフッター(BottomNavigationView)のためにインポート
import android.content.Intent
import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.gson.Gson // 【追加】JSON変換ライブラリ
import com.google.gson.reflect.TypeToken // 【追加】JSONリストの型指定
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

//  日記生成画面
class DiaryGenerateActivity : AppCompatActivity() {

    // UI部品
    private lateinit var btnClose: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etDiaryContent: EditText
    private lateinit var btnImage: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnSave: Button
    private lateinit var bottomNav: BottomNavigationView //フッターの宣言
    // 【追加】ConversationDataを受け取るための変数
    private var receivedConversations: List<ConversationData> = listOf()

    // 歩数管理マネージャー
    private lateinit var stepSensorManager: StepSensorManager
    private var targetYear: Int = 0
    private var targetMonth: Int = 0
    private var targetDay: Int = 0

    // ★追加: SupabaseのSentiment関数のURL
    // [あなたのプロジェクトID] の部分を書き換えてください
    private val SUPABASE_SENTIMENT_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/sentiment"

    // 通信クライアント
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_generate) // レイアウト適用

        // 【追加】Intentからデータを受け取る処理
        receiveDataFromIntent()

        initViews()       // UI初期化
        setupListeners()  // ボタン動作設定

        // タイトルに日付を表示（例: 11月11日）
        if (targetYear != 0) {
            tvTitle.text = "${targetMonth + 1}月${targetDay}日"
        }

        // 歩数センサーの初期化と計測開始
        stepSensorManager = StepSensorManager(this)
    }

    override fun onResume() {
        super.onResume()
        stepSensorManager.startListening() // 画面表示時に計測再開
    }

    override fun onPause() {
        super.onPause()
        stepSensorManager.stopListening() // 画面非表示時に計測停止（バッテリー節約）
    }

    // 【追加】IntentからJSONデータを受け取る関数
    private fun receiveDataFromIntent() {
        // 日付の取得
        targetYear = intent.getIntExtra("year", 0)
        targetMonth = intent.getIntExtra("month", 0)
        targetDay = intent.getIntExtra("day", 0)

        // IntentからJSON文字列を取得
        val conversationsJson = intent.getStringExtra("EXTRA_CONVERSATIONS_JSON")

        if (conversationsJson != null) {
            val gson = Gson()
            // List<ConversationData> の型を正確にGsonに伝えるために TypeToken を使用
            val listType = object : TypeToken<List<ConversationData>>() {}.type
            // JSON文字列をデータクラスのリストに変換して変数に格納
            receivedConversations = gson.fromJson(conversationsJson, listType)
        }
    }

    // UI部品をIDと結びつける
    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        tvTitle = findViewById(R.id.tvTitle)
        etDiaryContent = findViewById(R.id.etDiaryContent)
        btnImage = findViewById(R.id.btnImage)
        btnEdit = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)

        // ★AI追加: フッターの初期化
        bottomNav = findViewById(R.id.bottom_navigation)
    }

    // 各ボタンのクリック処理
    private fun setupListeners() {
        btnClose.setOnClickListener { finish() } // 閉じる

        btnImage.setOnClickListener {
            Toast.makeText(this, "画像を追加", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            Toast.makeText(this, "編集モード", Toast.LENGTH_SHORT).show()
        }

        // 保存ボタンで分析＆保存を開始
        btnSave.setOnClickListener { analyzeAndSaveDiary() }

        // フッターのクリック処理
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // カレンダー
                R.id.nav_calendar -> {
                    val intent = Intent(this, DiaryInputActivity::class.java)
                    startActivity(intent)
                    true
                }
                // 統計
                R.id.nav_stats -> {
                    Toast.makeText(this, "統計画面", Toast.LENGTH_SHORT).show()
                    true
                }
                // プロフィール
                R.id.nav_profile -> {
                    Toast.makeText(this, "プロフィール画面", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    // ★変更: サーバーで感情分析をしてから保存する処理
    private fun analyzeAndSaveDiary() {
        val content = etDiaryContent.text.toString()
        if (content.isEmpty()) {
            Toast.makeText(this, "内容を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        // UIをローディング状態にする
        btnSave.isEnabled = false
        btnSave.text = "分析中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Supabaseへ分析リクエスト
                val jsonBody = JSONObject().apply {
                    put("text", content)
                }
                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(SUPABASE_SENTIMENT_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                // デフォルト値（失敗時用）
                var emotionScore = "0.0"
                var positiveScore = "0.0"
                var negativeScore = "0.0"

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")

                    if (json.optBoolean("success")) {
                        val normalized = json.getJSONObject("normalized")
                        val pos = normalized.optDouble("positive", 0.0)
                        val neg = normalized.optDouble("negative", 0.0)

                        // スコアを保存用にセット
                        positiveScore = pos.toString()
                        negativeScore = neg.toString()

                        // 感情スコア (-1.0 〜 1.0) を算出
                        // シンプルに (ポジティブ - ネガティブ) で計算
                        emotionScore = (pos - neg).toString()
                    }
                } else {
                    Log.e("DiaryGenerate", "Analysis failed: ${response.code}")
                }

                // 2. データの保存処理 (メインスレッドで実行しなくてOKだが、完了後のUI操作はメインで)
                saveDiaryData(content, emotionScore, positiveScore, negativeScore)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DiaryGenerateActivity, "保存しました！", Toast.LENGTH_SHORT).show()
                    btnSave.text = "保存"
                    btnSave.isEnabled = true

                    // 完了したら画面を閉じる、またはカレンダーへ遷移
                    // finish()
                    val intent = Intent(this@DiaryGenerateActivity, CalendarActivity::class.java)
                    // 戻るスタックをクリアしてカレンダーへ
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DiaryGenerateActivity, "分析エラー: 保存のみ行います", Toast.LENGTH_SHORT).show()
                    // エラーでも保存はする
                    saveDiaryData(content, "0.0", "0.0", "0.0")
                    btnSave.text = "保存"
                    btnSave.isEnabled = true
                    finish()
                }
            }
        }
    }

    // 内部保存処理
    private fun saveDiaryData(content: String, emotion: String, pos: String, neg: String) {
        val currentSteps = stepSensorManager.currentSteps.toString()

        val dateString = if (targetYear != 0) {
            String.format(Locale.getDefault(), "%04d/%02d/%02d", targetYear, targetMonth + 1, targetDay)
        } else {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        }

        val newDiary = DiaryEntry(
            id = UUID.randomUUID().toString(),
            date = dateString,
            conversations = receivedConversations,
            stepCount = currentSteps,
            diaryContent = content,
            emotionScore = emotion, // ★サーバーから取得した値を保存
            positiveScore = pos,
            negativeScore = neg
        )

        val appData = JsonDataManager.load(this)
        appData.diaries.add(newDiary)
        JsonDataManager.save(this, appData)
    }
}
