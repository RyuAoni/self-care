package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.gson.GsonBuilder
import java.util.Calendar

class SettingsDetailActivity : AppCompatActivity() {

    // 設定値を保持
    private lateinit var spinnerGender: AutoCompleteTextView
    private lateinit var spinnerYear: AutoCompleteTextView
    private lateinit var spinnerMonth: AutoCompleteTextView
    private lateinit var spinnerDay: AutoCompleteTextView
    private lateinit var editOccupation: EditText
    private lateinit var editHobby: EditText
    private lateinit var editFavorite: EditText
    private lateinit var pushInfoSwitch: MaterialSwitch
    private lateinit var demoModeSwitch: MaterialSwitch
    private lateinit var btnCheckData: Button
    private lateinit var bottomNavigation: BottomNavigationView

    // 通知時刻設定用
    private lateinit var notificationTimeLayout: LinearLayout
    private lateinit var spinnerNotificationHour: AutoCompleteTextView
    private lateinit var spinnerNotificationMinute: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_detail)

        // ビューの初期化
        initViews()

        // トップバー設定
        setupTopBar()

        // Spinnerの設定
        setupSpinners()

        // 通知時刻Spinnerの設定
        setupNotificationTimeSpinners()

        // ボトムナビゲーション設定
        setupBottomNavigation()

        // 保存されているデータを読み込み
        loadUserData()

        setupPushInfoSwitch()

        setupDemoSwitch()

        setupDebugButton()

        setupKeyboardListener()
        setCustomStatusBar()
    }

    override fun onResume() {
        super.onResume()
        // 画面に戻ってきたときにボトムナビゲーションを表示
        if (::bottomNavigation.isInitialized) {
            bottomNavigation.visibility = View.VISIBLE
        }
    }

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > 250) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE
            }
        }
    }

    private fun initViews() {
        spinnerGender = findViewById(R.id.spinnerGender)
        spinnerYear = findViewById(R.id.spinnerYear)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        spinnerDay = findViewById(R.id.spinnerDay)
        editOccupation = findViewById(R.id.editOccupation)
        editHobby = findViewById(R.id.editHobby)
        editFavorite = findViewById(R.id.editFavorite)
        pushInfoSwitch = findViewById(R.id.pushInfoSwitch)
        demoModeSwitch = findViewById(R.id.demoModeSwitch)
        btnCheckData = findViewById(R.id.btnCheckData)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // 通知時刻設定用
        notificationTimeLayout = findViewById(R.id.notificationTimeLayout)
        spinnerNotificationHour = findViewById(R.id.spinnerNotificationHour)
        spinnerNotificationMinute = findViewById(R.id.spinnerNotificationMinute)
    }

    private fun setupDebugButton() {
        btnCheckData.setOnClickListener {
            showJsonDialog()
        }
    }

    private fun showJsonDialog() {
        // 1. データをロード
        val appData = JsonDataManager.load(this)

        // 2. 見やすく整形 (Pretty Print)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(appData)

        // 3. ダイアログを表示
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_debug_json, null)
        val tvJson = dialogView.findViewById<TextView>(R.id.tvJsonContent)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseDebug)

        tvJson.text = jsonString

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDemoSwitch() {
        // プロフィールとは別の "AppConfig" に保存する
        val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)

        // 保存された状態を反映
        demoModeSwitch.isChecked = prefs.getBoolean("demo_mode", false)

        // 切り替え時の処理
        demoModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("demo_mode", isChecked).apply()
            val status = if (isChecked) "ON" else "OFF"
            Toast.makeText(this, "デモモードを ${status} にしました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNotificationTimeSpinners() {
        // 時間 (0〜23)
        val hours = (0..23).map { String.format("%02d", it) }
        val hourAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, hours)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationHour.setAdapter(hourAdapter)

        // 分 (00, 15, 30, 45)
        val minutes = listOf("00", "15", "30", "45")
        val minuteAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, minutes)
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationMinute.setAdapter(minuteAdapter)

        // 保存されている時刻を読み込み (デフォルト: 21:00)
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val savedHour = prefs.getInt("notification_hour", 19)
        val savedMinute = prefs.getInt("notification_minute", 0)

        // 時（"02" 形式に変換）
        val hourString = String.format("%02d", savedHour)
        spinnerNotificationHour.setText(hourString, false)

        // 分（"00", "15", "30", "45" 形式に変換）
        val minuteString = String.format("%02d", savedMinute)

        // リストにない場合は "00" を入れる
        val finalMinute = if (minutes.contains(minuteString)) minuteString else "00"
        spinnerNotificationMinute.setText(finalMinute, false)
    }

    private fun setupPushInfoSwitch() {
        // UserProfile に保存する
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)

        // デフォルト ON(何も保存されていない場合 true)
        val isEnabled = prefs.getBoolean("push_info", true)
        pushInfoSwitch.isChecked = isEnabled

        // 通知時刻レイアウトの表示/非表示を設定
        updateNotificationTimeVisibility(isEnabled)

        // 切り替えイベント
        pushInfoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("push_info", isChecked).apply()

            // 通知時刻レイアウトの表示/非表示を切り替え
            updateNotificationTimeVisibility(isChecked)

            val scheduler = AlarmScheduler(this)

            if (isChecked) {
                // 設定されている時刻で通知をセット
                val hour = spinnerNotificationHour.text.toString().toInt()
                val minute = spinnerNotificationMinute.text.toString().toInt()

                // 時刻も保存
                prefs.edit().apply {
                    putInt("notification_hour", hour)
                    putInt("notification_minute", minute)
                    apply()
                }

                scheduler.setDailyAlarm(hour, minute)
                Toast.makeText(this, "通知を ON に設定しました", Toast.LENGTH_SHORT).show()
            } else {
                scheduler.cancelDailyAlarm()
                Toast.makeText(this, "通知設定を OFF にしました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNotificationTimeVisibility(isVisible: Boolean) {
        notificationTimeLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun setupTopBar() {
        // 閉じるボタン
        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        // 保存ボタン
        findViewById<TextView>(R.id.buttonSave).setOnClickListener {
            saveUserData()
            overridePendingTransition(0, 0)
        }
    }

    private fun setupSpinners() {
        // 性別 AutoCompleteTextView
        val genderOptions = arrayOf("選択してください", "男性", "女性", "その他", "回答しない")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        spinnerGender.setAdapter(genderAdapter)

        // 年（1900年〜現在）
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf("-")
        for (year in currentYear downTo 1900) {
            years.add(year.toString())
        }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)
        spinnerYear.setAdapter(yearAdapter)

        // 月（1〜12）
        val months = mutableListOf("-")
        for (month in 1..12) {
            months.add(month.toString())
        }
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
        spinnerMonth.setAdapter(monthAdapter)

        // 日（1〜31）
        val days = mutableListOf("-")
        for (day in 1..31) {
            days.add(day.toString())
        }
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days)
        spinnerDay.setAdapter(dayAdapter)

        // 年・月が変更されたら日の選択肢を調整
        spinnerYear.setOnItemClickListener { _, _, _, _ ->
            updateDaySpinner()
        }

        spinnerMonth.setOnItemClickListener { _, _, _, _ ->
            updateDaySpinner()
        }
    }


    private fun updateDaySpinner() {
        val yearStr = spinnerYear.text.toString()
        val monthStr = spinnerMonth.text.toString()

        // 「-」 の場合は処理しない
        if (yearStr == "-" || monthStr == "-") {
            return
        }

        val year = yearStr.toIntOrNull() ?: return
        val month = monthStr.toIntOrNull() ?: return

        val previousDay = spinnerDay.text.toString()

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val newDays = mutableListOf("-")
        for (d in 1..maxDay) {
            newDays.add(d.toString())
        }

        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, newDays)
        spinnerDay.setAdapter(dayAdapter)

        if (previousDay in newDays) {
            spinnerDay.setText(previousDay, false)
        } else {
            spinnerDay.setText("-", false)
        }
    }

    private fun setupBottomNavigation() {
        // 初期選択を解除する
        bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNavigation.menu.size()) {
            bottomNavigation.menu.getItem(i).isChecked = false
        }
        bottomNavigation.menu.setGroupCheckable(0, true, true)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
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
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)

        // 性別
        val gender = prefs.getString("gender", "")
        if (!gender.isNullOrEmpty()) {
            spinnerGender.setText(gender, false)
        } else {
            spinnerGender.setText("選択してください", false)
        }

        // 生年月日
        val birthday = prefs.getString("birthday", "")
        if (!birthday.isNullOrEmpty()) {
            val parts = birthday.split("/")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]

                spinnerYear.setText(year, false)
                spinnerMonth.setText(month.toInt().toString(), false)

                // 月に応じて日数を更新
                updateDaySpinner()

                spinnerDay.post {
                    spinnerDay.setText(day.toInt().toString(), false)
                }
            }
        } else {
            spinnerYear.setText("-", false)
            spinnerMonth.setText("-", false)
            spinnerDay.setText("-", false)
        }

        editOccupation.setText(prefs.getString("occupation", ""))
        editHobby.setText(prefs.getString("hobby", ""))
        editFavorite.setText(prefs.getString("favorite", ""))
    }

    private fun saveUserData() {
        val gender = spinnerGender.text.toString()
        val year = spinnerYear.text.toString()
        val month = spinnerMonth.text.toString()
        val day = spinnerDay.text.toString()

        val birthday = if (
            year != "-" &&
            month != "-" &&
            day != "-"
        ) {
            "%s/%02d/%02d".format(year, month.toInt(), day.toInt())
        } else {
            ""
        }

        val occupation = editOccupation.text.toString().trim()
        val hobby = editHobby.text.toString().trim()
        val favorite = editFavorite.text.toString().trim()

        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        prefs.edit().apply {
            putString("gender", if (gender == "選択してください") "" else gender)
            putString("birthday", birthday)
            putString("occupation", occupation)
            putString("hobby", hobby)
            putString("favorite", favorite)

            // 通知 ON の時は時刻も保存する
            if (pushInfoSwitch.isChecked) {
                val hour = spinnerNotificationHour.text.toString().toInt()
                val minute = spinnerNotificationMinute.text.toString().toInt()
                putInt("notification_hour", hour)
                putInt("notification_minute", minute)

                AlarmScheduler(this@SettingsDetailActivity).setDailyAlarm(hour, minute)
            }

            apply()
        }

        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
        finish()
    }
}