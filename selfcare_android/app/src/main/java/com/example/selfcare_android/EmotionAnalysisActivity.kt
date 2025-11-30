package com.example.selfcare_android

import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class EmotionAnalysisActivity : AppCompatActivity() {

    private lateinit var emotionChart: LineChart
    private lateinit var stepChart: BarChart
    private lateinit var weekSpinner: Spinner
//
//    private var startDate: Calendar = Calendar.getInstance()
//    private var endDate: Calendar = Calendar.getInstance()
    private val weekRanges = mutableListOf<Pair<Calendar, Calendar>>()
    private var currentWeekIndex = 0 // 現在選択されている週のインデックス

    // アプリ全体のデータ
    private lateinit var appData: AppData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emotion_analysis)

        emotionChart = findViewById(R.id.emotionChart)
        stepChart = findViewById(R.id.stepChart)
        weekSpinner = findViewById(R.id.weekSpinner)

        // データをロード
        appData = JsonDataManager.load(this)

        // 週の範囲を生成（過去8週間分）
        generateWeekRanges()
        setupWeekSpinner()
//        setupEmotionChart()
//        setupStepChart()
        setupBottomNavigation()
    }

    // 画面に戻ってきた時もデータを最新にする
    override fun onResume() {
        super.onResume()
        appData = JsonDataManager.load(this)
        updateCharts()
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
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
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
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            weekRanges.add(Pair(weekStart, weekEnd))
        }

//        // 最初の週（今週）を初期値に設定
//        currentWeekIndex = 0
//        startDate.time = weekRanges[currentWeekIndex].first.time
//        endDate.time = weekRanges[currentWeekIndex].second.time
    }

    /** 週選択スピナーの設定 */
    private fun setupWeekSpinner() {
        val format = SimpleDateFormat("M月d日", Locale.JAPAN)
        val weekLabels = weekRanges.map { (start, end) ->
            "${format.format(start.time)}〜${format.format(end.time)}"
        }

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            weekLabels
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        weekSpinner.adapter = adapter
        weekSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentWeekIndex = position
//                val selectedWeek = weekRanges[position]
//                startDate.time = selectedWeek.first.time
//                endDate.time = selectedWeek.second.time
                updateCharts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** グラフ更新 */
    private fun updateCharts() {
        // 選択された週の範囲を取得
        val (start, end) = weekRanges[currentWeekIndex]

        // 1週間分の日付リスト（日〜土）を作成
        val weekDates = getDatesInWeek(start)

        // 日記データをフィルタリングして、日ごとのデータを抽出
        val emotionEntries = ArrayList<Entry>()
        val stepEntries = ArrayList<BarEntry>()

        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 0(日曜)〜6(土曜) のループ
        for (i in weekDates.indices) {
            val targetDateStr = sdf.format(weekDates[i])

            // その日の日記データを検索
            val diary = appData.diaries.find { it.date == targetDateStr }

            if (diary != null) {
                // 感情スコア (String -> Float)
                // -1.0〜1.0 の範囲を 0〜5 などのグラフ用数値に変換しても良いが、
                // ここではとりあえずそのままの値を使用し、Y軸の範囲調整で見やすくする
                val emotion = diary.emotionScore.toFloatOrNull() ?: 0f
                emotionEntries.add(Entry(i.toFloat(), emotion))

                // 歩数 (String -> Float)
                val steps = diary.stepCount.toFloatOrNull() ?: 0f
                stepEntries.add(BarEntry(i.toFloat(), steps))
            } else {
                // データがない日は 0 を入れる（またはエントリーを追加しない）
                // emotionEntries.add(Entry(i.toFloat(), 0f)) // 線をつなげたい場合は0を入れる
                stepEntries.add(BarEntry(i.toFloat(), 0f))
            }
        }

        setupEmotionChart(emotionEntries)
        setupStepChart(stepEntries)
//        setupEmotionChart()
//        setupStepChart()
    }

    // 指定した開始日から7日間のDateリストを返す
    private fun getDatesInWeek(startDate: Calendar): List<Date> {
        val dates = mutableListOf<Date>()
        val cal = startDate.clone() as Calendar
        for (i in 0..6) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    /** 感情スコアグラフ */
    private fun setupEmotionChart(entries: List<Entry>) {
//        // TODO: 実際のデータベースからデータを取得
//        val entries = listOf(
//            Entry(0f, 3f),
//            Entry(1f, 1f),
//            Entry(2f, 2f),
//            Entry(3f, 2f),
//            Entry(4f, 5f),
//            Entry(5f, 4f),
//            Entry(6f, 3f)
//        )

//        val dataSet = LineDataSet(entries, "感情スコア").apply {
//            color = resources.getColor(android.R.color.black, null)
//            setCircleColor(resources.getColor(android.R.color.black, null))
//            lineWidth = 2f
//            circleRadius = 5f
//            setDrawValues(false)
//        }
        val dataSet = LineDataSet(entries, "感情スコア (-1.0:悲 〜 1.0:喜)").apply {
            color = resources.getColor(android.R.color.black, null)
            setCircleColor(resources.getColor(android.R.color.black, null))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
        }

        val data = LineData(dataSet)
        emotionChart.data = data
        emotionChart.description.isEnabled = false
//        emotionChart.xAxis.apply {
//            position = XAxis.XAxisPosition.BOTTOM
//            granularity = 1f
//            setDrawGridLines(true)
//            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
//        }
        // X軸の設定
        emotionChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
            axisMinimum = -0.5f // 端の余白
            axisMaximum = 6.5f
        }
        emotionChart.axisRight.isEnabled = false
//        emotionChart.axisLeft.apply {
//            setDrawGridLines(true)
//            axisMinimum = 0f
//            axisMaximum = 5f
//        }
        emotionChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = -1.2f
            axisMaximum = 1.2f
            granularity = 0.5f
        }
        emotionChart.invalidate()
    }

    /** 歩数グラフ */
    private fun setupStepChart(entries: List<BarEntry>) {
//        // TODO: 実際のデータベースからデータを取得
//        val entries = listOf(
//            BarEntry(0f, 1600f),
//            BarEntry(1f, 4500f),
//            BarEntry(2f, 2700f),
//            BarEntry(3f, 3500f),
//            BarEntry(4f, 1500f),
//            BarEntry(5f, 3600f),
//            BarEntry(6f, 4200f)
//        )

//        val dataSet = BarDataSet(entries, "歩数").apply {
//            colors = ColorTemplate.MATERIAL_COLORS.toList()
//            valueTextSize = 12f
//        }
        val dataSet = BarDataSet(entries, "歩数").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
        }

        val data = BarData(dataSet)
        data.barWidth = 0.6f
        stepChart.data = data
        stepChart.description.isEnabled = false
//        stepChart.xAxis.apply {
//            position = XAxis.XAxisPosition.BOTTOM
//            granularity = 1f
//            setDrawGridLines(true)
//            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
//        }
        stepChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            valueFormatter = IndexAxisValueFormatter(listOf("日", "月", "火", "水", "木", "金", "土"))
            axisMinimum = -0.5f
            axisMaximum = 6.5f
        }
        stepChart.axisRight.isEnabled = false
//        stepChart.axisLeft.setDrawGridLines(true)
        stepChart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = 0f // 歩数は0から
        }
        stepChart.invalidate()
    }
}