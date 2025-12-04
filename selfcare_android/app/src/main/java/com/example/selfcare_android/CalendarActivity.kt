package com.example.selfcare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import android.widget.AutoCompleteTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthYearText: AutoCompleteTextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private val days = mutableListOf<CalendarDay>()

    private var diaryList: List<DiaryEntry> = emptyList()

    private lateinit var gestureDetector: GestureDetectorCompat

    private val monthYearList = mutableListOf<Pair<Int, Int>>() // (year, month)
    private var currentMonthIndex = 0

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

    private lateinit var stepSensorManager: StepSensorManager
    private val REQUEST_CODE_ACTIVITY_RECOGNITION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        requestNotificationPermission()

        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val isPushEnabled = prefs.getBoolean("push_info", true)

        val scheduler = AlarmScheduler(this)

        if (isPushEnabled) {
            val hour = prefs.getInt("notification_hour", 19)
            val minute = prefs.getInt("notification_minute", 0)
            scheduler.setDailyAlarm(hour, minute)
        } else {
            scheduler.cancelDailyAlarm()
        }

        checkAndRequestPermissions()
        stepSensorManager = StepSensorManager(this)

        generateMonthYearList()
        setupViews()
        setupCalendar()
        setupBottomNavigation()
        setupGestureDetector()
        updateCalendar()
        setCustomStatusBar()
    }

    override fun onResume() {
        super.onResume()
        loadDiaryData()
        updateCalendar()
        stepSensorManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        stepSensorManager.stopListening()
    }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                stepSensorManager.startListening()
            } else {
                Toast.makeText(this, "歩数を記録するには権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDiaryData() {
        val appData = JsonDataManager.load(this)
        diaryList = appData.diaries

        if (::calendarAdapter.isInitialized) {
            calendarAdapter.updateDiaries(diaryList)
        }
    }

    /** 月のリストを生成（今月から過去12ヶ月分） */
    private fun generateMonthYearList() {
        val today = Calendar.getInstance()

        for (i in 0..11) {
            val cal = Calendar.getInstance().apply {
                time = today.time
                add(Calendar.MONTH, -i)
            }
            monthYearList.add(Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
        }
    }

    private fun setupViews() {
        monthYearText = findViewById(R.id.monthYearText)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        setupMonthYearSpinner()

        // 矢印ボタンでの月移動
        findViewById<ImageView>(R.id.prevMonthButton).setOnClickListener {
            if (currentMonthIndex < monthYearList.size - 1) {
                currentMonthIndex++
                updateFromSpinner()
            }
        }

        findViewById<ImageView>(R.id.nextMonthButton).setOnClickListener {
            if (currentMonthIndex > 0) {
                currentMonthIndex--
                updateFromSpinner()
            }
        }
    }

    /** プルダウンの設定 */
    private fun setupMonthYearSpinner() {
        val monthLabels = monthYearList.map { (year, month) ->
            val monthStr = if (month + 1 < 10) {
                "${year}年　${month + 1}月"  // 1桁の月の前に全角スペースを追加
            } else {
                "${year}年 ${month + 1}月"
            }
            monthStr
        }

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_dropdown_item,
            monthLabels
        )

        monthYearText.setAdapter(adapter)

        // AutoCompleteTextView では onItemClickListener を使用する
        monthYearText.setOnItemClickListener { parent, view, position, id ->
            currentMonthIndex = position
            updateFromSpinner()
        }

        // 初期値（今月）を表示
        monthYearText.setText(monthLabels[currentMonthIndex], false)
    }

    /** スピナーの選択から更新 */
    private fun updateFromSpinner() {
        val (year, month) = monthYearList[currentMonthIndex]
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)

        val monthLabels = monthYearList.map { (y, m) ->
            if (m + 1 < 10) {
                "${y}年　${m + 1}月"  // 1桁の月の前に全角スペースを追加
            } else {
                "${y}年 ${m + 1}月"
            }
        }
        monthYearText.setText(monthLabels[currentMonthIndex], false)

        updateCalendar()
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
                            if (currentMonthIndex < monthYearList.size - 1) {
                                currentMonthIndex++
                                updateFromSpinner()
                            }
                        } else {
                            // 左スワイプ → 次月
                            if (currentMonthIndex > 0) {
                                currentMonthIndex--
                                updateFromSpinner()
                            }
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

    private fun setupCalendar() {
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(days, diaryList) { day ->
            if (!isFutureDate(day)) {
                openDayDetail(day)
            }
        }
        calendarRecyclerView.adapter = calendarAdapter
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
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        days.clear()

        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val firstDayOfMonth = Calendar.getInstance()
        firstDayOfMonth.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val prevMonth = Calendar.getInstance()
        prevMonth.set(currentYear, currentMonth, 1)
        prevMonth.add(Calendar.MONTH, -1)
        val maxDayInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            val day = maxDayInPrevMonth - firstDayOfWeek + i + 1
            days.add(CalendarDay(day, false, currentYear, currentMonth - 1))
        }

        for (day in 1..maxDayInMonth) {
            days.add(CalendarDay(day, true, currentYear, currentMonth))
        }

        val remainingDays = 42 - days.size
        for (day in 1..remainingDays) {
            days.add(CalendarDay(day, false, currentYear, currentMonth + 1))
        }

        if (::calendarAdapter.isInitialized) {
            calendarAdapter.updateDiaries(diaryList)
            calendarAdapter.notifyDataSetChanged()
        }
    }

    private fun openDayDetail(day: CalendarDay) {
        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", day.year, day.month + 1, day.day)

        val today = Calendar.getInstance()
        val isToday = (day.year == today.get(Calendar.YEAR) &&
                day.month == today.get(Calendar.MONTH) &&
                day.day == today.get(Calendar.DAY_OF_MONTH))

        val targetEntry = diaryList.find { it.date == dateString }
        val hasContent = targetEntry != null && targetEntry.diaryContent.isNotEmpty()

        val intent = if (isToday) {
            if (hasContent) {
                Intent(this, DiaryDetailActivity::class.java)
            } else {
                Intent(this, DiaryInputActivity::class.java)
            }
        } else {
            Intent(this, DiaryDetailActivity::class.java)
        }

        intent.putExtra("year", day.year)
        intent.putExtra("month", day.month)
        intent.putExtra("day", day.day)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.setGroupCheckable(0, true, true)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_calendar -> {
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
    private var diaries: List<DiaryEntry>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

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

        val dateString = String.format(Locale.getDefault(), "%04d/%02d/%02d", day.year, day.month + 1, day.day)
        val diaryEntry = diaries.find { it.date == dateString }

        if (diaryEntry != null && diaryEntry.diaryContent?.isNotEmpty() == true) {
            holder.emotionIcon.visibility = View.VISIBLE

            val score = diaryEntry.emotionScore.toDoubleOrNull() ?: 0.0
            val iconRes = when {
                score >= 0.6 -> R.drawable.emoji_very_happy
                score >= 0.2 -> R.drawable.emoji_happy
                score >= -0.2 -> R.drawable.emoji_neutral
                score >= -0.6 -> R.drawable.emoji_sad
                else -> R.drawable.emoji_very_sad
            }
            holder.emotionIcon.setImageResource(iconRes)

        } else {
            holder.emotionIcon.visibility = View.INVISIBLE
        }
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

            if ((position + 1) % spanCount == 0 && position < state.itemCount - spanCount) {
                val left = parent.paddingLeft.toFloat()
                val right = (parent.width - parent.paddingRight).toFloat()
                val bottom = child.bottom.toFloat()

                c.drawLine(left, bottom, right, bottom, paint)
            }
        }
    }
}