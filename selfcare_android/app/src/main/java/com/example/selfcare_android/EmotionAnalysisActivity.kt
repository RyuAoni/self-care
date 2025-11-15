package com.example.selfcare_android

import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class EmotionAnalysisActivity : AppCompatActivity() {

    private lateinit var emotionChart: LineChart
    private lateinit var stepChart: BarChart
    private lateinit var weekSpinner: Spinner

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()
    private val weekRanges = mutableListOf<Pair<Calendar, Calendar>>()
    private var currentWeekIndex = 0 // 現在選択されている週のインデックス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emotion_analysis)

        emotionChart = findViewById(R.id.emotionChart)
        stepChart = findViewById(R.id.stepChart)
        weekSpinner = findViewById(R.id.weekSpinner)

        // 週の範囲を生成（過去8週間分）
        generateWeekRanges()
        setupWeekSpinner()
        setupEmotionChart()
        setupStepChart()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
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
        // statsを選択状態にする
        bottomNav.selectedItemId = R.id.nav_stats
    }

    /** 週の範囲を生成 */
    private fun generateWeekRanges() {
        val today = Calendar.getInstance()

        // 今日を含む週の日曜日を計算
        val currentWeekStart = Calendar.getInstance().apply {
            time = today.time
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        // 過去8週間分の範囲を生成
        for (i in 0..7) {
            val weekStart = Calendar.getInstance().apply {
                time = currentWeekStart.time
                add(Calendar.WEEK_OF_YEAR, -i)
            }
            val weekEnd = Calendar.getInstance().apply {
                time = weekStart.time
                add(Calendar.DAY_OF_YEAR, 6)
            }
            weekRanges.add(Pair(weekStart, weekEnd))
        }

        // 最初の週（今週）を初期値に設定
        currentWeekIndex = 0
        startDate.time = weekRanges[currentWeekIndex].first.time
        endDate.time = weekRanges[currentWeekIndex].second.time
    }

    /** 週選択スピナーの設定 */
    private fun setupWeekSpinner() {
        val format = SimpleDateFormat("M月d日", Locale.JAPAN)
        val weekLabels = weekRanges.map { (start, end) ->
            "${format.format(start.time)}〜${format.format(end.time)}"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            weekLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        weekSpinner.adapter = adapter
        weekSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentWeekIndex = position
                val selectedWeek = weekRanges[position]
                startDate.time = selectedWeek.first.time
                endDate.time = selectedWeek.second.time
                updateCharts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** グラフ更新 */
    private fun updateCharts() {
        setupEmotionChart()
        setupStepChart()
    }

    /** 感情スコアグラフ */
    private fun setupEmotionChart() {
        // TODO: 実際のデータベースからデータを取得
        val entries = listOf(
            Entry(0f, 3f),
            Entry(1f, 1f),
            Entry(2f, 2f),
            Entry(3f, 2f),
            Entry(4f, 5f),
            Entry(5f, 4f),
            Entry(6f, 3f)
        )

        val dataSet = LineDataSet(entries, "感情スコア").apply {
            color = resources.getColor(android.R.color.black, null)
            setCircleColor(resources.getColor(android.R.color.black, null))
            lineWidth = 2f
            circleRadius = 5f
            setDrawValues(false)
        }

        val data = LineData(dataSet)
        emotionChart.data = data
        emotionChart.description.isEnabled = false
        emotionChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
        }
        emotionChart.axisRight.isEnabled = false
        emotionChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = 0f
            axisMaximum = 5f
        }
        emotionChart.invalidate()
    }

    /** 歩数グラフ */
    private fun setupStepChart() {
        // TODO: 実際のデータベースからデータを取得
        val entries = listOf(
            BarEntry(0f, 1600f),
            BarEntry(1f, 4500f),
            BarEntry(2f, 2700f),
            BarEntry(3f, 3500f),
            BarEntry(4f, 1500f),
            BarEntry(5f, 3600f),
            BarEntry(6f, 4200f)
        )

        val dataSet = BarDataSet(entries, "歩数").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        }

        val data = BarData(dataSet)
        stepChart.data = data
        stepChart.description.isEnabled = false
        stepChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
        }
        stepChart.axisRight.isEnabled = false
        stepChart.axisLeft.setDrawGridLines(true)
        stepChart.invalidate()
    }
}