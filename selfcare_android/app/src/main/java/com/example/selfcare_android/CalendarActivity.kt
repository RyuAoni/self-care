package com.example.selfcare_android

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*
import kotlin.math.abs

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private val days = mutableListOf<CalendarDay>()
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        setupViews()
        setupCalendar()
        setupBottomNavigation()
        setupGestureDetector()
        updateCalendar()
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
        calendarAdapter = CalendarAdapter(days) { day ->
            // 当月かつ今日以前の日付のみクリック可能
            if (day.isCurrentMonth && !isFutureDate(day)) {
                openDayDetail(day)
            }
        }
        calendarRecyclerView.adapter = calendarAdapter
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

        calendarAdapter.notifyDataSetChanged()
    }

    private fun openDayDetail(day: CalendarDay) {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(day.year, day.month, day.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val intent = if (target.before(today)) {
            // 昨日以前 → DiaryDetailActivity
            Intent(this, DiaryDetailActivity::class.java)
        } else {
            // 今日 → DiaryInputActivity
            Intent(this, DiaryInputActivity::class.java)
        }

        intent.putExtra("year", day.year)
        intent.putExtra("month", day.month)
        intent.putExtra("day", day.day)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayText: TextView = view.findViewById(R.id.dayText)
        val dayContainer: View = view.findViewById(R.id.dayContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        holder.dayText.text = day.day.toString()

        if (day.isCurrentMonth) {
            holder.dayText.alpha = 1.0f
            holder.dayContainer.setOnClickListener {
                onDayClick(day)
            }
        } else {
            holder.dayText.alpha = 0.3f
            holder.dayContainer.setOnClickListener(null)
        }

        // 今日の日付をハイライト
        val today = Calendar.getInstance()
        if (day.year == today.get(Calendar.YEAR) &&
            day.month == today.get(Calendar.MONTH) &&
            day.day == today.get(Calendar.DAY_OF_MONTH) &&
            day.isCurrentMonth
        ) {
            holder.dayContainer.setBackgroundResource(R.drawable.circle_highlight)
        } else {
            holder.dayContainer.background = null
        }

        // TODO: 感情アイコンの表示（将来実装）
        // val emotionIcon = holder.itemView.findViewById<ImageView>(R.id.emotionIcon)
        // loadEmotionIcon(day, emotionIcon)
    }

    override fun getItemCount() = days.size
}