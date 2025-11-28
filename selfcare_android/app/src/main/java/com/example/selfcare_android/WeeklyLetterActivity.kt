package com.example.selfcare_android

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.view.LayoutInflater
import com.google.android.material.bottomnavigation.BottomNavigationView


class WeeklyLetterActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeeklyLetterAdapter

    // ★書き換え必要: SupabaseのURL
    private val SUPABASE_WEEKLY_LETTER_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/weekly-letter"

    // 通信クライアント
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // 生成に時間がかかるため長めに
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_letter)

        // 戻るボタン
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // RecyclerViewの設定
        setupRecyclerView()

        // ボトムナビゲーションの設定
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.letterRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 1. 過去4週間分くらいの「週の範囲」リストを作る
        val weeks = generatePastWeeks(4)

        // 2. 保存済みの手紙データをロード
        val appData = JsonDataManager.load(this)
        val savedLetters = appData.weeklyLetters

        // 3. 表示用データに変換
        // (保存済みならその内容を、なければ「未生成」として扱う)
        val displayList = weeks.map { weekRange ->
            val found = savedLetters.find { it.period == weekRange }
            if (found != null) {
                // すでに手紙がある場合
                WeeklyLetterDisplayItem(weekRange, found.title, found.content, true)
            } else {
                // 手紙がまだない場合
                WeeklyLetterDisplayItem(weekRange, "${weekRange}の\n週次お手紙", "タップして手紙を作成する", false)
            }
        }

        adapter = WeeklyLetterAdapter(displayList) { item ->
            if (item.isGenerated) {
                // 既にある場合はダイアログで表示
                showLetterDialog(item.title, item.content)
            } else {
                // まだない場合は生成確認ダイアログを表示
                showGenerateConfirmDialog(item.period)
            }
        }
        recyclerView.adapter = adapter
    }

    // 直近n週間分の期間文字列リストを生成 (例: "11/03~11/09")
    private fun generatePastWeeks(weeksCount: Int): List<String> {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        // 週の始まりを日曜日に設定
        calendar.firstDayOfWeek = Calendar.SUNDAY
        // 今日が含まれる週の日曜日へ移動
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())

        for (i in 0 until weeksCount) {
            // 週の終わりの土曜日
            val endCal = calendar.clone() as Calendar
            endCal.add(Calendar.DAY_OF_MONTH, 6)

            val startStr = sdf.format(calendar.time)
            val endStr = sdf.format(endCal.time)

            list.add("$startStr~$endStr")

            // 1週間前に戻る
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return list
    }

    // 生成確認ダイアログ
    private fun showGenerateConfirmDialog(period: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("手紙を受け取りますか？")
            .setMessage("$period の日記をもとに、AIからのお手紙を作成します。")
            .setNegativeButton("キャンセル", null)
            .setPositiveButton("作成する") { _, _ ->
                generateWeeklyLetter(period)
            }
            .show()
    }

    // AI手紙生成処理
    private fun generateWeeklyLetter(period: String) {
        // ローディング表示などを入れるとなお良い
        Toast.makeText(this, "お手紙を書いています...少々お待ちください", Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 対象期間の日記データを抽出
                val appData = JsonDataManager.load(this@WeeklyLetterActivity)
                val targetDiaries = filterDiariesByPeriod(appData.diaries, period)

                if (targetDiaries.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WeeklyLetterActivity, "この期間の日記がないため作成できません", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 2. 送信データの作成
                // サーバーは { "diaries": [...], "weekRange": "..." } を期待している
                val diariesPayload = targetDiaries.map {
                    mapOf("date" to it.date, "content" to it.diaryContent)
                }

                val jsonBody = JSONObject().apply {
                    put("diaries", Gson().toJsonTree(diariesPayload))
                    put("weekRange", period)
                }

                val request = Request.Builder()
                    .url(SUPABASE_WEEKLY_LETTER_URL)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                // 3. API呼び出し
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val letterContent = json.optString("letter", "手紙を受け取れませんでした。")

                    // 4. データを保存
                    val newLetter = WeeklyLetterData(
                        id = UUID.randomUUID().toString(),
                        period = period,
                        title = "${period}の\n週次お手紙",
                        content = letterContent
                    )
                    appData.weeklyLetters.add(newLetter)
                    JsonDataManager.save(this@WeeklyLetterActivity, appData)

                    // 5. 画面更新
                    withContext(Dispatchers.Main) {
                        setupRecyclerView() // リストを再描画
                        showLetterDialog(newLetter.title, newLetter.content) // 完成した手紙を表示
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WeeklyLetterActivity, "作成に失敗しました: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WeeklyLetterActivity, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 期間文字列 ("11/03~11/09") に含まれる日記を抽出するヘルパー
    private fun filterDiariesByPeriod(allDiaries: List<DiaryEntry>, period: String): List<DiaryEntry> {
        try {
            // 現在の年を補完してパース (期間文字列には年がないため)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val parts = period.split("~")
            if (parts.size != 2) return emptyList()

            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

            // 期間の開始日と終了日をDate型に変換
            val startDate = sdf.parse("$currentYear/${parts[0]}")
            val endDate = sdf.parse("$currentYear/${parts[1]}")

            if (startDate == null || endDate == null) return emptyList()

            // 終了日の時刻を23:59:59にするなどの調整が必要だが、簡易的に日付比較
            // 日記の日付フォーマット "yyyy/MM/dd" をパースして比較
            return allDiaries.filter { diary ->
                val diaryDate = sdf.parse(diary.date)
                diaryDate != null && !diaryDate.before(startDate) && !diaryDate.after(endDate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun showLetterDialog(title: String, content: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_weekly_letter, null)

        dialogView.findViewById<TextView>(R.id.letterTitle).text = title
        dialogView.findViewById<TextView>(R.id.letterContent).text = content

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("閉じる", null)
            .show()
    }

    private fun setupBottomNavigation() {
        findViewById<ImageView>(R.id.navStats).setOnClickListener {
            // TODO: 統計画面へ遷移
        }
        findViewById<ImageView>(R.id.navCalendar).setOnClickListener {
            // TODO: カレンダー画面へ遷移
        }
        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
            finish()
        }
    }

//    private fun setupBottomNavigation() {
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
//        // 初期選択を解除する
//        bottomNav.menu.setGroupCheckable(0, true, false)
//        for (i in 0 until bottomNav.menu.size()) {
//            bottomNav.menu.getItem(i).isChecked = false
//        }
//        bottomNav.menu.setGroupCheckable(0, true, true)
//
//
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_stats -> {
//                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                R.id.nav_calendar -> {
//                    val intent = Intent(this, CalendarActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                R.id.nav_profile -> {
//                    val intent = Intent(this, SettingsActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                else -> false
//            }
//        }
//    }
}

// リスト表示用のデータクラス
data class WeeklyLetterDisplayItem(
    val period: String,
    val title: String,
    val content: String,
    val isGenerated: Boolean
)

// アダプター
class WeeklyLetterAdapter(
    private val letters: List<WeeklyLetterDisplayItem>,
    private val onItemClick: (WeeklyLetterDisplayItem) -> Unit
) : RecyclerView.Adapter<WeeklyLetterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val periodText: TextView = view.findViewById(R.id.periodText)
        val titleText: TextView = view.findViewById(R.id.letterTitle) // レイアウトに合わせてID確認が必要
        val contentText: TextView = view.findViewById(R.id.letterContentPreview) // レイアウトに合わせてID確認が必要
        val cardView: CardView = view.findViewById(R.id.letterCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_letter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val letter = letters[position]
        holder.periodText.text = letter.period
        holder.cardView.setOnClickListener {
            onItemClick(letter)
        }

        // 生成済みかどうかで見た目を変える（例：未生成なら薄くする等）
        if (!letter.isGenerated) {
            holder.cardView.alpha = 0.6f
        } else {
            holder.cardView.alpha = 1.0f
        }
    }

    override fun getItemCount() = letters.size
}