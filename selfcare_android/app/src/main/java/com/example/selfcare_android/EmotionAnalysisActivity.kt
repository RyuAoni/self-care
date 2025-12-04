package com.example.selfcare_android

import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class EmotionAnalysisActivity : AppCompatActivity() {

    private lateinit var emotionChart: LineChart
    private lateinit var stepChart: BarChart
    private lateinit var weekSpinner: AutoCompleteTextView

    private val weekRanges = mutableListOf<Pair<Calendar, Calendar>>()
    private var currentWeekIndex = 0

    private lateinit var appData: AppData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emotion_analysis)

        emotionChart = findViewById(R.id.emotionChart)
        stepChart = findViewById(R.id.stepChart)
        weekSpinner = findViewById(R.id.weekSpinner)

        appData = JsonDataManager.load(this)

        generateWeekRanges()
        setupWeekSpinner()

        setupBottomNavigation()
        setCustomStatusBar()
    }

    override fun onResume() {
        super.onResume()
        appData = JsonDataManager.load(this)
        updateCharts()
    }

    /** 週の範囲を生成 */
    private fun generateWeekRanges() {
        val today = Calendar.getInstance()

        val currentWeekStart = Calendar.getInstance().apply {
            time = today.time
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0..7) {
            val weekStart = Calendar.getInstance().apply {
                time = currentWeekStart.time
                add(Calendar.WEEK_OF_YEAR, -i)
            }
            val weekEnd = Calendar.getInstance().apply {
                time = weekStart.time
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            weekRanges.add(Pair(weekStart, weekEnd))
        }
    }

    /** プルダウンの設定 */
    private fun setupWeekSpinner() {
        val format = SimpleDateFormat("M月d日", Locale.JAPAN)
        val weekLabels = weekRanges.map { (start, end) ->
            val startStr = format.format(start.time)
            val endStr = format.format(end.time)

            // 月と日をそれぞれ2桁に揃える
            val formattedStart = formatDateWithPadding(start)
            val formattedEnd = formatDateWithPadding(end)

            "${formattedStart}〜${formattedEnd}"
        }

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_dropdown_item,
            weekLabels
        )

        weekSpinner.setAdapter(adapter)

        /** ★ AutoCompleteTextView では onItemClickListener を使用する */
        weekSpinner.setOnItemClickListener { parent, view, position, id ->
            currentWeekIndex = position
            updateCharts()
        }

        /** 初期値（今週）を表示 */
        weekSpinner.setText(weekLabels[currentWeekIndex], false)
    }

    /** 月と日を2桁に揃えてフォーマット */
    private fun formatDateWithPadding(cal: Calendar): String {
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val monthStr = if (month < 10) "  ${month}" else "${month}"
        val dayStr = if (day < 10) "  ${day}" else "${day}"

        return "${monthStr}月${dayStr}日"
    }

    /** グラフの更新 */
    private fun updateCharts() {
        val (start, _) = weekRanges[currentWeekIndex]
        val weekDates = getDatesInWeek(start)

        val emotionEntries = ArrayList<Entry>()
        val stepEntries = ArrayList<BarEntry>()

        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        for (i in weekDates.indices) {
            val targetDateStr = sdf.format(weekDates[i])
            val diary = appData.diaries.find { it.date == targetDateStr }

            if (diary != null) {
                val emotion = diary.emotionScore.toFloatOrNull() ?: 0f
                emotionEntries.add(Entry(i.toFloat(), emotion))

                val steps = diary.stepCount.toFloatOrNull() ?: 0f
                stepEntries.add(BarEntry(i.toFloat(), steps))
            } else {
                stepEntries.add(BarEntry(i.toFloat(), 0f))
            }
        }

        setupEmotionChart(emotionEntries)
        setupStepChart(stepEntries)
    }

    private fun getDatesInWeek(startDate: Calendar): List<Date> {
        val dates = mutableListOf<Date>()
        val cal = startDate.clone() as Calendar
        for (i in 0..6) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    /** 感情スコアの折れ線グラフ */
    private fun setupEmotionChart(entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "感情スコア (-1.0〜1.0)").apply {
            color = resources.getColor(android.R.color.black, null)
            setCircleColor(resources.getColor(android.R.color.black, null))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
        }

        emotionChart.data = LineData(dataSet)
        emotionChart.description.isEnabled = false

        emotionChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
            axisMinimum = -0.5f
            axisMaximum = 6.5f
        }

        emotionChart.axisRight.isEnabled = false

        emotionChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = -1.2f
            axisMaximum = 1.2f
            granularity = 0.5f
        }

        emotionChart.invalidate()
    }

    /** 歩数の棒グラフ */
    private fun setupStepChart(entries: List<BarEntry>) {
        val dataSet = BarDataSet(entries, "歩数").apply {
            colors = listOf(
                android.graphics.Color.rgb(255, 99, 132),
                android.graphics.Color.rgb(255, 159, 64),
                android.graphics.Color.rgb(255, 205, 86),
                android.graphics.Color.rgb(75, 192, 192),
                android.graphics.Color.rgb(54, 162, 235),
                android.graphics.Color.rgb(153, 102, 255),
                android.graphics.Color.rgb(201, 203, 207)
            )
            valueTextSize = 10f
        }

        stepChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        stepChart.description.isEnabled = false

        stepChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
            axisMinimum = -0.5f
            axisMaximum = 6.5f
        }

        stepChart.axisRight.isEnabled = false

        stepChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = 0f
        }

        stepChart.invalidate()
    }

    /** BottomNavigation */
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> true
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_stats
    }
}