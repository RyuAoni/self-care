package com.example.selfcare_android

import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class EmotionAnalysisActivity : AppCompatActivity() {

    private lateinit var emotionChart: LineChart
    private lateinit var stepChart: BarChart
    private lateinit var dateRangeTextView: TextView

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emotion_analysis)

        emotionChart = findViewById(R.id.emotionChart)
        stepChart = findViewById(R.id.stepChart)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)

        // 初期表示
        startDate.set(2025, Calendar.JANUARY, 5)
        endDate.set(2025, Calendar.JANUARY, 11)
        updateDateRangeText()
        setupEmotionChart()
        setupStepChart()

        // 日付範囲テキストをクリックで変更
        dateRangeTextView.setOnClickListener {
            showStartDatePicker()
        }
    }

    /** 開始日を選択 */
    private fun showStartDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            startDate.set(year, month, day)
            showEndDatePicker()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** 終了日を選択 */
    private fun showEndDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            endDate.set(year, month, day)
            updateDateRangeText()
            updateCharts()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** 日付範囲表示を更新 */
    private fun updateDateRangeText() {
        val format = SimpleDateFormat("M月d日", Locale.JAPAN)
        dateRangeTextView.text =
            "${format.format(startDate.time)}〜${format.format(endDate.time)}"
    }

    /** グラフ更新 */
    private fun updateCharts() {
        setupEmotionChart()
        setupStepChart()
    }

    /** 感情スコアグラフ */
    private fun setupEmotionChart() {
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
        emotionChart.axisLeft.setDrawGridLines(true)
        emotionChart.invalidate()
    }

    /** 歩数グラフ */
    private fun setupStepChart() {
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
