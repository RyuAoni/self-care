package com.example.selfcare_android

//画面遷移(Intent)とフッター(BottomNavigationView)のためにインポート
import android.content.Intent
import android.net.Uri
import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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

    // 画像表示用
    private lateinit var imageContainer: CardView
    private lateinit var ivSelectedImage: ImageView
    private lateinit var btnRemoveImage: ImageButton // ★追加: 削除ボタン

    // 【追加】ConversationDataを受け取るための変数
    private var receivedConversations: List<ConversationData> = listOf()
    private var targetYear: Int = 0
    private var targetMonth: Int = 0
    private var targetDay: Int = 0

    // 状態管理
    private var isEditing: Boolean = false
    private var selectedImageUri: Uri? = null // 選んだ画像のURI
    private var savedImagePath: String? = null // 保存後のファイルパス

    // 歩数管理マネージャー
    private lateinit var stepSensorManager: StepSensorManager

    // ★追加: SupabaseのSentiment関数のURL
    // [あなたのプロジェクトID] の部分を書き換えてください
    private val SUPABASE_SENTIMENT_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/sentiment"
    private val SUPABASE_DIARY_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/diary"

    // 通信クライアント
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // フォトピッカーの準備
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // 画像が選ばれたとき
            selectedImageUri = uri
            ivSelectedImage.setImageURI(uri)
            imageContainer.visibility = View.VISIBLE
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

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

        // ★画面が開いたら、会話履歴をもとに日記を自動生成する
        if (receivedConversations.isNotEmpty()) {
            generateDiarySummary()
        } else {
            etDiaryContent.hint = "会話履歴がありません。今日はどんな一日でしたか？"
            // 生成しない場合は最初から編集可能にする
            enableEditMode(true)
        }
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

        // 画像表示用
        imageContainer = findViewById(R.id.imageContainer)
        ivSelectedImage = findViewById(R.id.ivSelectedImage)
        btnRemoveImage = findViewById(R.id.btnRemoveImage) // ★追加
    }

    // 各ボタンのクリック処理
    private fun setupListeners() {
        btnClose.setOnClickListener { finish() } // 閉じる

        // 画像追加ボタン: フォトピッカーを起動
        btnImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // ★追加: 画像削除ボタンの動作
        btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            ivSelectedImage.setImageURI(null)
            imageContainer.visibility = View.GONE
        }

        // 編集ボタン: モード切り替え
        btnEdit.setOnClickListener {
            enableEditMode(!isEditing)
        }

        // 保存ボタン
        btnSave.setOnClickListener { analyzeAndSaveDiary() }

//        btnImage.setOnClickListener {
//            Toast.makeText(this, "画像を追加", Toast.LENGTH_SHORT).show()
//        }
//
//        btnEdit.setOnClickListener {
//            Toast.makeText(this, "編集モード", Toast.LENGTH_SHORT).show()
//        }

//        // 保存ボタンで分析＆保存を開始
//        btnSave.setOnClickListener { analyzeAndSaveDiary() }
//
//        // フッターのクリック処理
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                // カレンダー
//                R.id.nav_calendar -> {
//                    val intent = Intent(this, DiaryInputActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                // 統計
//                R.id.nav_stats -> {
//                    val intent = Intent(this, WeeklyLetterActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                // プロフィール
//                R.id.nav_profile -> {
//                    val intent = Intent(this, SettingsActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                else -> false
//            }
//        }
        setupBottomNavigation()

    }

    // 編集モードの切り替え処理
    private fun enableEditMode(enable: Boolean) {
        isEditing = enable
        etDiaryContent.isEnabled = enable

        if (enable) {
            etDiaryContent.requestFocus()
            // キーボードを表示する処理を入れても良い
            Toast.makeText(this, "編集できます", Toast.LENGTH_SHORT).show()
            // アイコンの色を変えるなどして状態を示すと親切
            btnEdit.setColorFilter(android.graphics.Color.parseColor("#8B4513")) // 茶色に
        } else {
            btnEdit.clearColorFilter() // 元の色に
        }
    }

    // ★追加: Supabaseの日記生成APIを呼び出す
    private fun generateDiarySummary() {
        etDiaryContent.setText("日記を生成中...")
        etDiaryContent.isEnabled = false // 生成中は編集不可にする

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 送信データの作成
                // サーバーは { "messages": [...], "date": "..." } を期待
                val messagesPayload = receivedConversations.map {
                    mapOf("role" to it.commentator, "content" to it.text)
                }

                val dateString = "${targetYear}年${targetMonth + 1}月${targetDay}日"

                val requestData = mapOf(
                    "messages" to messagesPayload,
                    "date" to dateString
                )

                val jsonString = Gson().toJson(requestData)
                val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(SUPABASE_DIARY_URL)
                    .post(requestBody)
                    .build()

                // 2. API呼び出し
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val diaryText = json.optString("diary", "")

                    // 3. UI更新
                    withContext(Dispatchers.Main) {
                        if (diaryText.isNotEmpty()) {
                            etDiaryContent.setText(diaryText)
                        } else {
                            etDiaryContent.setText("")
                            etDiaryContent.hint = "日記の生成に失敗しました"
                        }
//                        etDiaryContent.isEnabled = true
                        // 生成完了後は「閲覧モード」にする（編集ボタンで編集可）
                        enableEditMode(false)
                    }
                } else {
//                    val errorMsg = "生成エラー: ${response.code}"
//                    Log.e("DiaryGenerate", errorMsg)
//                    withContext(Dispatchers.Main) {
//                        etDiaryContent.setText("")
//                        etDiaryContent.hint = "日記の生成に失敗しました (サーバーエラー)"
//                        etDiaryContent.isEnabled = true
//                        Toast.makeText(this@DiaryGenerateActivity, errorMsg, Toast.LENGTH_SHORT).show()
//                    }
                    withContext(Dispatchers.Main) {
                        etDiaryContent.setText("")
                        etDiaryContent.hint = "日記の生成に失敗しました"
                        enableEditMode(true) // 失敗時は手動入力を促す
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    etDiaryContent.setText("")
                    etDiaryContent.hint = "通信エラーが発生しました"
                    etDiaryContent.isEnabled = true
                    Toast.makeText(this@DiaryGenerateActivity, "通信エラー", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ★変更: サーバーで感情分析をしてから保存する処理
    private fun analyzeAndSaveDiary() {
        val content = etDiaryContent.text.toString()
//        if (content.isEmpty()) {
//            Toast.makeText(this, "内容を入力してください", Toast.LENGTH_SHORT).show()
//            return
//        }
        if (content.isEmpty() || content == "日記を生成中...") {
            Toast.makeText(this, "内容を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        // UIをローディング状態にする
        btnSave.isEnabled = false
//        btnSave.text = "分析中..."
        btnSave.text = "保存中..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
//                // 1. Supabaseへ分析リクエスト
//                val jsonBody = JSONObject().apply {
//                    put("text", content)
//                }
                // 1. 画像があれば内部ストレージにコピーして保存
                if (selectedImageUri != null) {
                    savedImagePath = copyImageToInternalStorage(selectedImageUri!!)
                } else {
                    savedImagePath = null // 画像が削除されていたらnullにする
                }

                // 2. 感情分析
//                val requestData = mapOf("text" to content)
//                val jsonString = Gson().toJson(requestData)
////                val requestBody = jsonBody.toString()
////                    .toRequestBody("application/json; charset=utf-8".toMediaType())
//                val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
//
//                val request = Request.Builder()
//                    .url(SUPABASE_SENTIMENT_URL)
//                    .post(requestBody)
//                    .build()
//
//                val response = client.newCall(request).execute()

                // デフォルト値（失敗時用）
                var emotionScore = "0.0"
                var positiveScore = "0.0"
                var negativeScore = "0.0"

//                if (response.isSuccessful) {
//                    val responseBody = response.body?.string()
//                    val json = JSONObject(responseBody ?: "{}")
//
//                    if (json.optBoolean("success")) {
//                        val normalized = json.getJSONObject("normalized")
//                        val pos = normalized.optDouble("positive", 0.0)
//                        val neg = normalized.optDouble("negative", 0.0)
//
//                        // スコアを保存用にセット
//                        positiveScore = pos.toString()
//                        negativeScore = neg.toString()
//
//                        // 感情スコア (-1.0 〜 1.0) を算出
//                        // シンプルに (ポジティブ - ネガティブ) で計算
//                        emotionScore = (pos - neg).toString()
//                    }
//                } else {
//                    Log.e("DiaryGenerate", "Analysis failed: ${response.code}")
//                }
                try {
                    val requestData = mapOf("text" to content)
                    val jsonString = Gson().toJson(requestData)
                    val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = Request.Builder()
                        .url(SUPABASE_SENTIMENT_URL)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        if (json.optBoolean("success")) {
                            val normalized = json.getJSONObject("normalized")
                            val pos = normalized.optDouble("positive", 0.0)
                            val neg = normalized.optDouble("negative", 0.0)
                            positiveScore = pos.toString()
                            negativeScore = neg.toString()
                            emotionScore = (pos - neg).toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 分析エラーでも保存は続行する
                    Log.e("DiaryGenerate", "Sentiment analysis failed: ${e.message}")
                }

                // 2. データの保存処理 (メインスレッドで実行しなくてOKだが、完了後のUI操作はメインで)
                saveDiaryData(content, emotionScore, positiveScore, negativeScore, savedImagePath)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DiaryGenerateActivity, "保存しました！", Toast.LENGTH_SHORT).show()
//                    btnSave.text = "保存"
//                    btnSave.isEnabled = true

                    // 完了したら画面を閉じる、またはカレンダーへ遷移
                    // finish()
                    val intent = Intent(this@DiaryGenerateActivity, CalendarActivity::class.java)
                    // 戻るスタックをクリアしてカレンダーへ
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish() // 現在のアクティビティを終了
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@DiaryGenerateActivity, "分析エラー: 保存のみ行います", Toast.LENGTH_SHORT).show()
//                    // エラーでも保存はする
//                    saveDiaryData(content, "0.0", "0.0", "0.0")
//                    btnSave.text = "保存"
//                    btnSave.isEnabled = true
////                    finish()
//
//                    val intent = Intent(this@DiaryGenerateActivity, CalendarActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//                    startActivity(intent)
                    Toast.makeText(this@DiaryGenerateActivity, "保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSave.text = "保存"
                    btnSave.isEnabled = true
                }
            }
        }
    }

    // 画像をアプリ専用領域にコピーする関数
    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            // 一意なファイル名を生成
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            file.absolutePath // 保存されたパスを返す
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 内部保存処理
    private fun saveDiaryData(content: String, emotion: String, pos: String, neg: String, imagePath: String?) {
        val currentSteps = stepSensorManager.currentSteps.toString()

        val dateString = if (targetYear != 0) {
            String.format(Locale.getDefault(), "%04d/%02d/%02d", targetYear, targetMonth + 1, targetDay)
        } else {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        }

        // データをロード
        val appData = JsonDataManager.load(this)

        // ★修正ポイント: 既存のデータを探す
        val existingIndex = appData.diaries.indexOfFirst { it.date == dateString }

        // 新しいデータオブジェクトを作成
        val newDiary = DiaryEntry(
            id = if (existingIndex != -1) appData.diaries[existingIndex].id else UUID.randomUUID().toString(), // IDは引き継ぐ
            date = dateString,
            conversations = receivedConversations,
            stepCount = currentSteps,
            diaryContent = content,
            imagePath = imagePath, // ★画像パスを保存
            emotionScore = emotion,
            positiveScore = pos,
            negativeScore = neg
        )

        if (existingIndex != -1) {
            // ★データがある場合は「上書き（更新）」
            appData.diaries[existingIndex] = newDiary
        } else {
            // ★データがない場合は「追加」
            appData.diaries.add(newDiary)
        }

        JsonDataManager.save(this, appData)
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
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
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
