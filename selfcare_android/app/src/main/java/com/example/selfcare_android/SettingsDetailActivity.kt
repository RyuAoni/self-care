package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerYear: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerDay: Spinner
    private lateinit var editOccupation: EditText
    private lateinit var editHobby: EditText
    private lateinit var editFavorite: EditText
    private lateinit var demoModeSwitch: MaterialSwitch
    private lateinit var btnCheckData: Button
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_detail)

        // ビューの初期化
        initViews()

        // トップバー設定
        setupTopBar()

        // Spinnerの設定
        setupSpinners()

        // ボトムナビゲーション設定
        setupBottomNavigation()

        // 保存されているデータを読み込み
        loadUserData()

        setupDemoSwitch()

        setupDebugButton()

        setupKeyboardListener()
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
        demoModeSwitch = findViewById(R.id.demoModeSwitch)
        btnCheckData = findViewById(R.id.btnCheckData)
        bottomNavigation = findViewById(R.id.bottom_navigation)
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

    private fun setupTopBar() {
        // 閉じるボタン
        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
            finish()
        }

        // 保存ボタン
        findViewById<TextView>(R.id.buttonSave).setOnClickListener {
            saveUserData()
        }
    }

    private fun setupSpinners() {
        // 性別Spinner
        val genderOptions = arrayOf("選択してください", "男性", "女性", "その他", "回答しない")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        // 年Spinner(1900年〜現在年)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf("—")
        for (year in currentYear downTo 1900) {
            years.add(year.toString())
        }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter

        // 月Spinner(1〜12月)
        val months = mutableListOf("—")
        for (month in 1..12) {
            months.add(month.toString())
        }
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        // 日Spinner(1〜31日)
        val days = mutableListOf("—")
        for (day in 1..31) {
            days.add(day.toString())
        }
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = dayAdapter

        // 年・月が変更されたら日の選択肢を調整
        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDaySpinner()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDaySpinner()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDaySpinner() {
        val yearStr = spinnerYear.selectedItem.toString()
        val monthStr = spinnerMonth.selectedItem.toString()

        if (yearStr == "—" || monthStr == "—") {
            return
        }

        val year = yearStr.toIntOrNull() ?: return
        val month = monthStr.toIntOrNull() ?: return

        // その月の最終日を計算
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf("—")
        for (day in 1..maxDay) {
            days.add(day.toString())
        }

        val currentSelection = spinnerDay.selectedItemPosition
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = dayAdapter

        // 以前の選択を維持(範囲内であれば)
        if (currentSelection < days.size) {
            spinnerDay.setSelection(currentSelection)
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
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)

        // 性別
        val gender = prefs.getString("gender", "")
        val genderPosition = when (gender) {
            "男性" -> 1
            "女性" -> 2
            "その他" -> 3
            "回答しない" -> 4
            else -> 0
        }
        spinnerGender.setSelection(genderPosition)

        // 生年月日
        val birthday = prefs.getString("birthday", "")
        if (birthday?.isNotEmpty() == true) {
            val parts = birthday.split("/")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]

                // 年の選択
                val yearAdapter = spinnerYear.adapter
                for (i in 0 until yearAdapter.count) {
                    if (yearAdapter.getItem(i).toString() == year) {
                        spinnerYear.setSelection(i)
                        break
                    }
                }

                // 月の選択
                val monthAdapter = spinnerMonth.adapter
                for (i in 0 until monthAdapter.count) {
                    if (monthAdapter.getItem(i).toString() == month.toInt().toString()) {
                        spinnerMonth.setSelection(i)
                        break
                    }
                }

                // 日の選択
                spinnerDay.postDelayed({
                    val dayAdapter = spinnerDay.adapter
                    for (i in 0 until dayAdapter.count) {
                        if (dayAdapter.getItem(i).toString() == day.toInt().toString()) {
                            spinnerDay.setSelection(i)
                            break
                        }
                    }
                }, 100)
            }
        }

        // その他の項目
        editOccupation.setText(prefs.getString("occupation", ""))
        editHobby.setText(prefs.getString("hobby", ""))
        editFavorite.setText(prefs.getString("favorite", ""))
    }

    private fun saveUserData() {
        // 入力チェック
        val gender = spinnerGender.selectedItem.toString()
        val year = spinnerYear.selectedItem.toString()
        val month = spinnerMonth.selectedItem.toString()
        val day = spinnerDay.selectedItem.toString()

        val birthday = if (year != "—" && month != "—" && day != "—") {
            String.format("%s/%02d/%02d", year, month.toInt(), day.toInt())
        } else {
            ""
        }

        val occupation = editOccupation.text.toString().trim()
        val hobby = editHobby.text.toString().trim()
        val favorite = editFavorite.text.toString().trim()

        // 保存
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        prefs.edit().apply {
            putString("gender", if (gender == "選択してください") "" else gender)
            putString("birthday", birthday)
            putString("occupation", occupation)
            putString("hobby", hobby)
            putString("favorite", favorite)
            apply()
        }

        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
        finish()
    }
}