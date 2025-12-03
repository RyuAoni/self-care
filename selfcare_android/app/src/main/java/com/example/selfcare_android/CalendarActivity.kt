package com.example.selfcare_android

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*
import kotlin.math.abs
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private val days = mutableListOf<CalendarDay>()

    // ★追加: 読み込んだ日記データを保持するリスト
    private var diaryList: List<DiaryEntry> = emptyList()

    private lateinit var gestureDetector: GestureDetectorCompat

    private val requestPermission =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            // granted が false なら通知は絶対に届かない
        }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // ★追加: 歩数センサー管理
    private lateinit var stepSensorManager: StepSensorManager
    private val REQUEST_CODE_ACTIVITY_RECOGNITION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        requestNotificationPermission()
        val scheduler = AlarmScheduler(this)
        // 毎日指定時刻に通知（例: 19:30）
        scheduler.setDailyAlarm(19, 0)

        // ★追加: 権限チェックとリクエスト
        checkAndRequestPermissions()

        // ★追加: センサー初期化
        stepSensorManager = StepSensorManager(this)

        setupViews()
        setupCalendar()
        setupBottomNavigation()
        setupGestureDetector()
        updateCalendar()
    }

    // ★追加: 画面が表示されるたびにデータを再読み込みする
    override fun onResume() {
        super.onResume()
        loadDiaryData()
        updateCalendar()
        // ★追加: 計測開始
        stepSensorManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        // ★追加: 計測一時停止
        stepSensorManager.stopListening()
    }

    // ★追加: 権限リクエストのロジック
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    REQUEST_CODE_ACTIVITY_RECOGNITION
                )
            }
        }
    }

    // ★追加: 権限リクエスト結果の受け取り
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 許可されたら計測開始
                stepSensorManager.startListening()
            } else {
                Toast.makeText(this, "歩数を記録するには権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ★追加: JSONファイルから日記データを読み込む処理
    private fun loadDiaryData() {
        val appData = JsonDataManager.load(this)
        diaryList = appData.diaries

        // アダプターに最新データを渡す
        if (::calendarAdapter.isInitialized) {
            calendarAdapter.updateDiaries(diaryList)
        }
    }

    private fun setupViews() {
        monthYearText = findViewById(R.id.monthYearText)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        // 年月テキストをクリックで月選択ダイアログを表示
        monthYearText.setOnClickListener {
            showMonthYearPicker()
        }

        findViewById<ImageView>(R.id.prevMonthButton).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<ImageView>(R.id.nextMonthButton).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // 右スワイプ → 前月
                            calendar.add(Calendar.MONTH, -1)
                            updateCalendar()
                        } else {
                            // 左スワイプ → 次月
                            calendar.add(Calendar.MONTH, 1)
                            updateCalendar()
                        }
                        return true
                    }
                }
                return false
            }
        })

        calendarRecyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun showMonthYearPicker() {
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // DatePickerDialogを使用して年月を選択
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                updateCalendar()
            },
            currentYear,
            currentMonth,
            1
        )

        // 日付選択を非表示にして月と年のみ選択可能に
        try {
            val dayPicker = datePickerDialog.datePicker
            dayPicker.findViewById<View>(
                resources.getIdentifier("day", "id", "android")
            )?.visibility = View.GONE
        } catch (e: Exception) {
            // フォールバック: 日付ピッカーの非表示に失敗した場合
        }

        datePickerDialog.show()
    }

    private fun setupCalendar() {
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(days, diaryList) { day ->
            // 今日以前の日付のみクリック可能
            if (!isFutureDate(day)) {
                openDayDetail(day)
            }
        }
        calendarRecyclerView.adapter = calendarAdapter

        // 横線の罫線を追加
        calendarRecyclerView.addItemDecoration(CalendarItemDecoration())
    }

    private fun isFutureDate(day: CalendarDay): Boolean {
        val today = Calendar.getInstance()
        val targetDate = Calendar.getInstance()
        targetDate.set(day.year, day.month, day.day, 0, 0, 0)
        targetDate.set(Calendar.MILLISECOND, 0)

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return targetDate.after(today)
    }

    private fun updateCalendar() {
        // 月と年を表示（例: 2025年 1月）
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        monthYearText.text = "${year}年 ${month}月"

        // カレンダーのデータを生成
        days.clear()

        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 月の最初の日
        val firstDayOfMonth = Calendar.getInstance()
        firstDayOfMonth.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 日曜日=0

        // 月の日数
        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 前月の日付を追加（グレーアウト）
        val prevMonth = Calendar.getInstance()
        prevMonth.set(currentYear, currentMonth, 1)
        prevMonth.add(Calendar.MONTH, -1)
        val maxDayInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            val day = maxDayInPrevMonth - firstDayOfWeek + i + 1
            days.add(CalendarDay(day, false, currentYear, currentMonth - 1))
        }

        // 当月の日付を追加
        for (day in 1..maxDayInMonth) {
            days.add(CalendarDay(day, true, currentYear, currentMonth))
        }

        // 次月の日付を追加（6週分になるように）
        val remainingDays = 42 - days.size // 6週 × 7日 = 42
        for (day in 1..remainingDays) {
            days.add(CalendarDay(day, false, currentYear, currentMonth + 1))
        }

        // アダプターにデータ変更を通知
        if (::calendarAdapter.isInitialized) {
            // 日記データも最新であることを保証
            calendarAdapter.updateDiaries(diaryList)
            calendarAdapter.notifyDataSetChanged()
        }
    }

//    private fun openDayDetail(day: CalendarDay) {
//        val today = Calendar.getInstance()
//        val target = Calendar.getInstance().apply {
//            set(day.year, day.month, day.day, 0, 0, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        today.set(Calendar.HOUR_OF_DAY, 0)
//        today.set(Calendar.MINUTE, 0)
//        today.set(Calendar.SECOND, 0)
//        today.set(Calendar.MILLISECOND, 0)
//
//        // ★修正: 日記が存在するかチェック
//        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", day.year, day.month + 1, day.day)
//        val hasDiary = diaryList.any { it.date == dateString }
//
//        val intent = if (target.before(today)) {
//            // 昨日以前 → DiaryDetailActivity
//            Intent(this, DiaryDetailActivity::class.java)
//        } else {
//            // 日記がない場合
//            if (target.before(today)) {
//                // 過去の日付なら、入力画面を開くか詳細画面を開くか検討が必要。
//                // ここでは「過去の日記も書ける」ように入力画面へ遷移させます（または空の詳細画面）
//                Intent(this, DiaryInputActivity::class.java)
//            } else {
//                // 今日なら入力画面へ
//                Intent(this, DiaryInputActivity::class.java)
//            }
//        }
//
//        intent.putExtra("year", day.year)
//        intent.putExtra("month", day.month)
//        intent.putExtra("day", day.day)
//        startActivity(intent)
//    }

    private fun openDayDetail(day: CalendarDay) {
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", day.year, day.month + 1, day.day)

        // 今日の日付かどうか判定
        val today = Calendar.getInstance()
        val isToday = (day.year == today.get(Calendar.YEAR) &&
                day.month == today.get(Calendar.MONTH) &&
                day.day == today.get(Calendar.DAY_OF_MONTH))

        // その日の日記データが存在するかチェック
        val targetEntry = diaryList.find { it.date == dateString }

        // データが存在し、かつ日記の中身が空でない（＝生成＆保存済み）場合のみ「完了」とみなす
        val hasContent = targetEntry != null && targetEntry.diaryContent.isNotEmpty()

        val intent = if (isToday) {
            // 当日の場合
            if (hasContent) {
                // 日記があれば詳細画面
                Intent(this, DiaryDetailActivity::class.java)
            } else {
                // 日記がなければ入力画面
                Intent(this, DiaryInputActivity::class.java)
            }
        } else {
            // 過去の場合
            // 日記の有無に関わらず詳細画面を開く
            // (詳細画面側で「日記がありません」などの表示対応が必要)
            Intent(this, DiaryDetailActivity::class.java)
        }

        intent.putExtra("year", day.year)
        intent.putExtra("month", day.month)
        intent.putExtra("day", day.day)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 初期選択状態のクリア
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
        // calendarを選択状態にする
        bottomNav.selectedItemId = R.id.nav_calendar
    }
}

data class CalendarDay(
    val day: Int,
    val isCurrentMonth: Boolean,
    val year: Int,
    val month: Int
)

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private var diaries: List<DiaryEntry>, // ★追加: 日記データのリストを受け取る
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    // ★追加: データを更新するためのメソッド
    fun updateDiaries(newDiaries: List<DiaryEntry>) {
        this.diaries = newDiaries
    }

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayText: TextView = view.findViewById(R.id.dayText)
        val dayContainer: View = view.findViewById(R.id.dayContainer)
        val emotionIcon: ImageView = view.findViewById(R.id.emotionIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        holder.dayText.text = day.day.toString()
        holder.dayContainer.setOnClickListener {
            onDayClick(day)
        }

        if (day.isCurrentMonth) {
            holder.dayText.alpha = 1.0f
        } else {
            holder.dayText.alpha = 0.3f
        }

        // 今日の日付をハイライト
        val dayHighlight = holder.itemView.findViewById<FrameLayout>(R.id.dayHighlight)
        val today = Calendar.getInstance()
        val isToday = day.year == today.get(Calendar.YEAR) &&
                day.month == today.get(Calendar.MONTH) &&
                day.day == today.get(Calendar.DAY_OF_MONTH) &&
                day.isCurrentMonth

        if (isToday) {
            dayHighlight.setBackgroundResource(R.drawable.circle_highlight)
        } else {
            dayHighlight.background = null
        }

        // ★修正: JSONデータに基づいてアイコンを表示
        // カレンダーの日付を文字列 "yyyy/MM/dd" に変換して検索
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", day.year, day.month + 1, day.day)
        val diaryEntry = diaries.find { it.date == dateString }

        if (diaryEntry != null && diaryEntry.diaryContent?.isNotEmpty() == true) {
            holder.emotionIcon.visibility = View.VISIBLE

            // 感情スコアに基づいてアイコンを変更
            val score = diaryEntry.emotionScore.toDoubleOrNull() ?: 0.0
            val iconRes = when {
                score >= 0.6 -> R.drawable.emoji_very_happy
                score >= 0.2 -> R.drawable.emoji_happy
                score >= -0.2 -> R.drawable.emoji_neutral // 0.0 はここに含まれる
                score >= -0.6 -> R.drawable.emoji_sad
                else -> R.drawable.emoji_very_sad
            }
            holder.emotionIcon.setImageResource(iconRes)

        } else {
            holder.emotionIcon.visibility = View.INVISIBLE
        }

//        // 感情アイコンの表示（1日から昨日まで）
//        val targetDate = Calendar.getInstance().apply {
//            set(day.year, day.month, day.day, 0, 0, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        val todayDate = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        // 当月で、1日から昨日まで（今日以前で今日は除く）
//        if (day.isCurrentMonth &&
//            day.day >= 1 &&
//            targetDate.before(todayDate)) {
//
//            holder.emotionIcon.visibility = View.VISIBLE
//
//            // 日付によって異なるアイコンを表示（テスト用）
//            val iconRes = when (day.day % 5) {
//                0 -> R.drawable.emoji_very_happy
//                1 -> R.drawable.emoji_happy
//                2 -> R.drawable.emoji_neutral
//                3 -> R.drawable.emoji_sad
//                else -> R.drawable.emoji_very_sad
//            }
//            holder.emotionIcon.setImageResource(iconRes)
//        } else {
//            holder.emotionIcon.visibility = View.INVISIBLE
//        }
    }

    override fun getItemCount() = days.size
}

class CalendarItemDecoration : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        color = Color.parseColor("#000000")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = 7

        // 週の最後のアイテム（土曜日）の下にスペースを追加
        if ((position + 1) % spanCount == 0 && position < state.itemCount - 1) {
            outRect.bottom = 1
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val spanCount = 7
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            // 週の最後（土曜日の下）に横線を描画
            if ((position + 1) % spanCount == 0 && position < state.itemCount - spanCount) {
                val left = parent.paddingLeft.toFloat()
                val right = (parent.width - parent.paddingRight).toFloat()
                val bottom = child.bottom.toFloat()

                c.drawLine(left, bottom, right, bottom, paint)
            }
        }
    }
}