package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import java.util.Locale
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class WeeklyLetterActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeeklyLetterAdapter

    private val SUPABASE_WEEKLY_LETTER_URL = "https://gvgntdierpbmygmkrtgy.supabase.co/functions/v1/weekly-letter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "WeeklyLetterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_letter)
        Log.d(TAG, "onCreate: WeeklyLetterActivity started")

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
        }

        setupRecyclerView()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Starting setup")
        recyclerView = findViewById(R.id.letterRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val appData = JsonDataManager.load(this)
        Log.d(TAG, "setupRecyclerView: Loaded data - ${appData.weeklyLetters.size} letters, ${appData.diaries.size} diaries")

        val savedLetters = appData.weeklyLetters
        val diaries = appData.diaries

        val weeks = if (diaries.isNotEmpty()) {
            getWeeksFromDiaries(diaries)
        } else {
            getWeeksFromDiaries(emptyList())
        }
        Log.d(TAG, "setupRecyclerView: Generated ${weeks.size} weeks: $weeks")

        val displayList = weeks.map { weekRange ->
            val found = savedLetters.find { it.period == weekRange }
            if (found != null) {
                Log.d(TAG, "setupRecyclerView: Found existing letter for $weekRange")
                WeeklyLetterDisplayItem(weekRange, found.title, found.content, true)
            } else {
                Log.d(TAG, "setupRecyclerView: No letter found for $weekRange")
                WeeklyLetterDisplayItem(weekRange, "${weekRange}の\n週次お手紙", "タップして手紙を作成する", false)
            }
        }

        adapter = WeeklyLetterAdapter(displayList) { item ->
            Log.d(TAG, "Item clicked: ${item.period}, isGenerated: ${item.isGenerated}")
            if (item.isGenerated) {
                showLetterDialog(item.title, item.content)
            } else {
                showGenerateConfirmDialog(item.period)
            }
        }
        recyclerView.adapter = adapter
        Log.d(TAG, "setupRecyclerView: RecyclerView setup complete with ${displayList.size} items")
    }

    private fun getWeeksFromDiaries(diaries: List<DiaryEntry>): List<String> {
        Log.d(TAG, "getWeeksFromDiaries: Processing ${diaries.size} diaries")
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val periodFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY

        val uniqueWeeks = mutableSetOf<String>()

        val datesToCheck = diaries.mapNotNull {
            try {
                val date = sdf.parse(it.date)
                Log.d(TAG, "getWeeksFromDiaries: Parsed date ${it.date} -> $date")
                date
            } catch (e: Exception) {
                Log.e(TAG, "getWeeksFromDiaries: Failed to parse date ${it.date}", e)
                null
            }
        }.toMutableList()

        if (datesToCheck.isEmpty()) {
            Log.d(TAG, "getWeeksFromDiaries: No valid dates, adding today")
            datesToCheck.add(Date())
        }

        for (date in datesToCheck) {
            calendar.time = date
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val startStr = periodFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val endStr = periodFormat.format(calendar.time)

            val weekRange = "$startStr~$endStr"
            uniqueWeeks.add(weekRange)
            Log.d(TAG, "getWeeksFromDiaries: Added week range: $weekRange")
        }

        val result = uniqueWeeks.sortedDescending()
        Log.d(TAG, "getWeeksFromDiaries: Returning ${result.size} unique weeks")
        return result
    }

    private fun showGenerateConfirmDialog(period: String) {
        Log.d(TAG, "showGenerateConfirmDialog: Showing dialog for $period")
        MaterialAlertDialogBuilder(this)
            .setTitle("手紙を受け取りますか？")
            .setMessage("$period の日記をもとに、AIからのお手紙を作成します。")
            .setNegativeButton("キャンセル") { _, _ ->
                Log.d(TAG, "showGenerateConfirmDialog: User cancelled")
            }
            .setPositiveButton("作成する") { _, _ ->
                Log.d(TAG, "showGenerateConfirmDialog: User confirmed, starting generation")
                generateWeeklyLetter(period)
            }
            .show()
    }

    private fun generateWeeklyLetter(period: String) {
        Log.d(TAG, "generateWeeklyLetter: Starting generation for $period")
        Toast.makeText(this, "お手紙を書いています...少々お待ちください", Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appData = JsonDataManager.load(this@WeeklyLetterActivity)
                val targetDiaries = filterDiariesByPeriod(appData.diaries, period)
                Log.d(TAG, "generateWeeklyLetter: Found ${targetDiaries.size} diaries for $period")

                if (targetDiaries.isEmpty()) {
                    Log.w(TAG, "generateWeeklyLetter: No diaries found for $period")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WeeklyLetterActivity, "この期間の日記がないため作成できません", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val diariesPayload = targetDiaries.map {
                    mapOf("date" to it.date, "content" to it.diaryContent)
                }

                val payload = mapOf(
                    "diaries" to diariesPayload,
                    "weekRange" to period
                )

                val jsonBody = Gson().toJson(payload)
                Log.d(TAG, "generateWeeklyLetter: Sending request to $SUPABASE_WEEKLY_LETTER_URL")
                Log.d(TAG, "generateWeeklyLetter: Payload: $jsonBody")

                val request = Request.Builder()
                    .url(SUPABASE_WEEKLY_LETTER_URL)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                Log.d(TAG, "generateWeeklyLetter: Response code: ${response.code}")

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    Log.d(TAG, "generateWeeklyLetter: Response body: $responseBody")

                    val json = JSONObject(responseBody)
                    val letterContent = json.optString("letter", "手紙を受け取れませんでした。")
                    Log.d(TAG, "generateWeeklyLetter: Letter content length: ${letterContent.length}")

                    val newLetter = WeeklyLetterData(
                        id = UUID.randomUUID().toString(),
                        period = period,
                        title = "${period}の\n週次お手紙",
                        content = letterContent
                    )

                    appData.weeklyLetters.add(newLetter)
                    JsonDataManager.save(this@WeeklyLetterActivity, appData)
                    Log.d(TAG, "generateWeeklyLetter: Letter saved successfully")

                    withContext(Dispatchers.Main) {
                        setupRecyclerView()
                        showLetterDialog(newLetter.title, newLetter.content)
                        Log.d(TAG, "generateWeeklyLetter: UI updated")
                    }
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "generateWeeklyLetter: Request failed with code ${response.code}")
                    Log.e(TAG, "generateWeeklyLetter: Error body: $errorBody")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WeeklyLetterActivity, "作成に失敗しました: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "generateWeeklyLetter: Exception occurred", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WeeklyLetterActivity, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterDiariesByPeriod(allDiaries: List<DiaryEntry>, period: String): List<DiaryEntry> {
        Log.d(TAG, "filterDiariesByPeriod: Filtering ${allDiaries.size} diaries for period $period")
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val parts = period.split("~")
            if (parts.size != 2) {
                Log.e(TAG, "filterDiariesByPeriod: Invalid period format: $period")
                return emptyList()
            }

            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

            val startDate = sdf.parse("$currentYear/${parts[0]}")
            val endDate = sdf.parse("$currentYear/${parts[1]}")

            if (startDate == null || endDate == null) {
                Log.e(TAG, "filterDiariesByPeriod: Failed to parse dates")
                return emptyList()
            }

            Log.d(TAG, "filterDiariesByPeriod: Period range: $startDate to $endDate")

            val filtered = allDiaries.filter { diary ->
                val diaryDate = sdf.parse(diary.date)
                val inRange = diaryDate != null && !diaryDate.before(startDate) && !diaryDate.after(endDate)
                Log.d(TAG, "filterDiariesByPeriod: Diary ${diary.date} - in range: $inRange")
                inRange
            }

            Log.d(TAG, "filterDiariesByPeriod: Filtered ${filtered.size} diaries")
            return filtered
        } catch (e: Exception) {
            Log.e(TAG, "filterDiariesByPeriod: Exception occurred", e)
            return emptyList()
        }
    }

    private fun showLetterDialog(title: String, content: String) {
        Log.d(TAG, "showLetterDialog: Showing letter - $title")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_weekly_letter, null)

        dialogView.findViewById<TextView>(R.id.letterTitle).text = title
        dialogView.findViewById<TextView>(R.id.letterContent).text = content

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("閉じる") { _, _ ->
                Log.d(TAG, "showLetterDialog: Dialog closed")
            }
            .show()
    }

    private fun setupBottomNavigation() {
        Log.d(TAG, "setupBottomNavigation: Setting up navigation")
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)

        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "setupBottomNavigation: Item selected - ${item.itemId}")
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

data class WeeklyLetterDisplayItem(
    val period: String,
    val title: String,
    val content: String,
    val isGenerated: Boolean
)

class WeeklyLetterAdapter(
    private val letters: List<WeeklyLetterDisplayItem>,
    private val onItemClick: (WeeklyLetterDisplayItem) -> Unit
) : RecyclerView.Adapter<WeeklyLetterAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "WeeklyLetterAdapter"
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val periodText: TextView = view.findViewById(R.id.periodText)
        val titleText: TextView = view.findViewById(R.id.letterTitle)
        val contentText: TextView = view.findViewById(R.id.letterContentPreview)
        val cardView: CardView = view.findViewById(R.id.letterCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_letter, parent, false)
        Log.d(TAG, "onCreateViewHolder: Created new ViewHolder")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val letter = letters[position]
        Log.d(TAG, "onBindViewHolder: Binding position $position - ${letter.period}, generated: ${letter.isGenerated}")

        holder.periodText.text = letter.period
        holder.titleText.text = letter.title
        holder.contentText.text = letter.content
        holder.cardView.setOnClickListener {
            Log.d(TAG, "onBindViewHolder: Card clicked at position $position")
            onItemClick(letter)
        }

        if (!letter.isGenerated) {
            holder.cardView.alpha = 0.6f
        } else {
            holder.cardView.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${letters.size} items")
        return letters.size
    }
}