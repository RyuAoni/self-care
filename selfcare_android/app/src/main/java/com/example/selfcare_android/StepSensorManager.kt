package com.example.selfcare_android

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 歩数センサーを管理するクラス
 */
class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // 現在の歩数を保持する変数
    var currentSteps: Int = 0
        private set

    // データ保存用
    private val prefs = context.getSharedPreferences("step_data", Context.MODE_PRIVATE)

    init {
        if (stepSensor == null) {
            Log.e("StepSensorManager", "このデバイスには歩数センサーが搭載されていません。")
        }
    }

    /**
     * センサーの計測を開始する
     */
    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * センサーの計測を停止する
     */
    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            // 端末起動時からの累計歩数
            val rawSteps = event.values[0].toInt()

            // 今日の歩数を計算して更新
            updateDailySteps(rawSteps)
        }
    }

    private fun updateDailySteps(rawSteps: Int) {
        // 今日の日付文字列 (例: "20251111")
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        // 保存されている「最後に計測した日付」と「その日の開始歩数」を取得
        val lastDate = prefs.getString("last_date", "")
        var startSteps = prefs.getInt("start_steps", -1)

        if (lastDate != today) {
            // 日付が変わった（または初めての計測）場合
            // 今の累計歩数を「今日の開始歩数」として保存
            startSteps = rawSteps
            prefs.edit()
                .putString("last_date", today)
                .putInt("start_steps", startSteps)
                .apply()
        } else if (startSteps == -1) {
            // 同じ日だが開始歩数が未保存の場合（念のため）
            startSteps = rawSteps
            prefs.edit().putInt("start_steps", startSteps).apply()
        } else if (rawSteps < startSteps) {
            // 端末が再起動されて rawSteps がリセットされた場合の対策
            // 開始歩数を0リセットして、ここからの歩数をカウントする
            startSteps = 0
            prefs.edit().putInt("start_steps", 0).apply()
        }

        // 今日の歩数 = 現在の累計 - 今日の開始歩数
        currentSteps = rawSteps - startSteps

        // 負の値にならないように補正
        if (currentSteps < 0) currentSteps = 0

        Log.d("StepSensorManager", "今日の歩数: $currentSteps (累計: $rawSteps, 開始: $startSteps)")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 精度の変更は今回は無視
    }
}