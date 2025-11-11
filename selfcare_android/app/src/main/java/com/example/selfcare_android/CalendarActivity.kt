package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private val days = mutableListOf<CalendarDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        setupViews()
        setupCalendar()
        setupBottomNavigation()
        updateCalendar()
    }

    private fun setupViews() {
        monthYearText = findViewById(R.id.monthYearText)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        findViewById<ImageView>(R.id.prevMonthButton).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<ImageView>(R.id.nextMonthButton).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun setupCalendar() {
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(days) { day ->
            if (day.isCurrentMonth) {
                openDayDetail(day)
            }
        }
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun updateCalendar() {
        // 月と年を表示
        val monthName = "${calendar.get(Calendar.MONTH) + 1}月"
        monthYearText.text = monthName

        // カレンダーのデータを生成
        days.clear()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // 月の最初の日
        val firstDayOfMonth = Calendar.getInstance()
        firstDayOfMonth.set(year, month, 1)
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 日曜日=0

        // 月の日数
        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 前月の日付を追加（グレーアウト）
        val prevMonth = Calendar.getInstance()
        prevMonth.set(year, month, 1)
        prevMonth.add(Calendar.MONTH, -1)
        val maxDayInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            val day = maxDayInPrevMonth - firstDayOfWeek + i + 1
            days.add(CalendarDay(day, false, year, month - 1))
        }

        // 当月の日付を追加
        for (day in 1..maxDayInMonth) {
            days.add(CalendarDay(day, true, year, month))
        }

        // 次月の日付を追加（6週分になるように）
        val remainingDays = 42 - days.size // 6週 × 7日 = 42
        for (day in 1..remainingDays) {
            days.add(CalendarDay(day, false, year, month + 1))
        }

        calendarAdapter.notifyDataSetChanged()
    }

    private fun openDayDetail(day: CalendarDay) {
        val intent = Intent(this, DiaryInputActivity::class.java)
        intent.putExtra("year", day.year)
        intent.putExtra("month", day.month)
        intent.putExtra("day", day.day)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        findViewById<ImageView>(R.id.navStats).setOnClickListener {
            Toast.makeText(this, "統計", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navCalendar).setOnClickListener {
            // 現在の画面なので何もしない
        }

        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
            finish()
        }
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
            day.isCurrentMonth) {
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