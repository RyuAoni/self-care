package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    // 1. DataRepository のインスタンスを準備
    private val repository by lazy { DataRepository(applicationContext) }

    // --- UI要素を定義 ---
    // (XMLのIDに合わせて調整してください)
    private lateinit var tvName: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvBirthday: TextView
    private lateinit var tvHobby: TextView
    private lateinit var tvFavorite: TextView
    // private lateinit var tvEmail: TextView // (もしあれば)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // ボタンのクリックリスナー設定
        setupClickListeners()

        // ボトムナビゲーションの設定
        setupBottomNavigation()
    }

//    private fun setupMenuButtons() {
//        // 週次お手紙ボタン
//        findViewById<CardView>(R.id.weeklyLetterButton).setOnClickListener {
//            // TODO: 週次お手紙画面へ遷移
//            startActivity(Intent(this, WeeklyLetterActivity::class.java))
//        }
//
//        // アルバムボタン
//        findViewById<CardView>(R.id.albumButton).setOnClickListener {
//            // TODO: アルバム画面へ遷移
//            startActivity(Intent(this, AlbumActivity::class.java))
//        }
//
//        // 設定ボタン（現在の画面なので何もしない or 設定詳細へ）
//        findViewById<CardView>(R.id.settingsButton).setOnClickListener {
//            // TODO: 設定詳細画面へ遷移
//            startActivity(Intent(this, SettingsDetailActivity::class.java))
//        }
//    }

//    private fun setupBottomNavigation() {
//        // 統計アイコン
//        findViewById<ImageView>(R.id.navStats).setOnClickListener {
//            // TODO: 統計画面へ遷移
//            // startActivity(Intent(this, StatsActivity::class.java))
//        }
//
//        // カレンダーアイコン
//        findViewById<ImageView>(R.id.navCalendar).setOnClickListener {
//            // TODO: カレンダー画面へ遷移
//            // startActivity(Intent(this, CalendarActivity::class.java))
//        }
//
//        // プロフィールアイコン
//        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
//            // TODO: プロフィール画面へ遷移
//            // startActivity(Intent(this, ProfileActivity::class.java))
//        }
//    }

    override fun onResume() {
        super.onResume()
        // プロフィール情報を更新
        loadUserSettings()
    }

//    private fun loadProfileData() {
//        // SharedPreferencesからプロフィール情報を読み込み
//        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
//        val userName = sharedPreferences.getString("user_name", "はるさめべーこん")
//
//        // 名前を表示
//        findViewById<android.widget.TextView>(R.id.nameText).text = "名前 $userName"
//
//        // TODO: プロフィール画像の読み込み
//        // val profileImageUri = sharedPreferences.getString("profile_image_uri", null)
//        // profileImageUri?.let {
//        //     findViewById<ImageView>(R.id.profileImage).setImageURI(Uri.parse(it))
//        // }
//    }
    private fun initViews() {
        tvName = findViewById(R.id.nameText) // 例: 名前のTextView
        tvGender = findViewById(R.id.spinnerGender) // 例: 性別のTextView
        tvBirthday = findViewById(R.id.tv_birthday) // 例: 誕生日のTextView
        tvHobby = findViewById(R.id.tv_hobby) // 例: 趣味のTextView
        tvFavorite = findViewById(R.id.tv_favorite) // 例: 好きなもののTextView
    }

    /**
     * 3. データ読み込み (DataRepository を使うように変更)
     */
    private fun loadUserSettings() {
        lifecycleScope.launch {
            val settings = repository.loadData().settings

            // 読み込んだデータを各TextViewにセット
            // ( ?: "未設定" は、データがnullの場合に "未設定" と表示する処理)
            tvName.text = settings?.name ?: "未設定"
            tvGender.text = settings?.gender ?: "未設定"
            tvBirthday.text = settings?.birthday ?: "未設定"
            tvHobby.text = settings?.hobby ?: "未設定"
            tvFavorite.text = settings?.favorite ?: "未設定"
        }
    }

    /**
     * 4. 各項目がクリックされた時の処理
     * (SettingsDetailActivityへ "DETAIL_TYPE" を渡して起動)
     */
    private fun setupClickListeners() {
        // XMLのIDに合わせて調整してください
        findViewById<LinearLayout>(R.id.layout_name).setOnClickListener {
            startDetailActivity("NAME")
        }
        findViewById<LinearLayout>(R.id.layout_gender).setOnClickListener {
            startDetailActivity("GENDER")
        }
        findViewById<LinearLayout>(R.id.layout_birthday).setOnClickListener {
            startDetailActivity("BIRTHDAY")
        }
        findViewById<LinearLayout>(R.id.layout_hobby).setOnClickListener {
            startDetailActivity("HOBBY")
        }
        findViewById<LinearLayout>(R.id.layout_favorite).setOnClickListener {
            startDetailActivity("FAVORITE")
        }

        // 戻るボタン (もしあれば)
        findViewById<ImageView>(R.id.buttonBack)?.setOnClickListener {
            finish()
        }
    }

    /**
     * SettingsDetailActivity を起動するヘルパー関数
     */
    private fun startDetailActivity(detailType: String) {
        val intent = Intent(this, SettingsDetailActivity::class.java)
        intent.putExtra("DETAIL_TYPE", detailType)
        startActivity(intent)
    }

    /**
     * ボトムナビゲーションの設定 (既存のコード)
     */
    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_mypage // マイページを選択状態に

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_graph -> {
                    startActivity(Intent(this, MainActivity::class.java)) // グラフ画面へ
                    true
                }
                R.id.navigation_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java)) // カレンダー画面へ
                    true
                }
                R.id.navigation_mypage -> {
                    // すでにマイページ（設定画面）にいるので何もしない
                    true
                }
                else -> false
            }
        }
    }
}