package com.example.selfcare_android

// --- 必要なimport文 ---
// SharedPreferences関連（MODE_PRIVATEなど）は不要になります
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout // 誕生日Spinner用
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
// ----------------------

class SettingsDetailActivity : AppCompatActivity() {

    // DataRepository のインスタンスを準備
    private val repository by lazy { DataRepository(applicationContext) }

    // --- UI要素を定義 ---
    // (lateinit var を使うと findViewById が1回で済みます)
    private lateinit var detailType: String
    private lateinit var editText: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var layoutBirthdaySpinners: LinearLayout // 年月日Spinnerの親レイアウト
    private lateinit var spinnerYear: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerDay: Spinner

    // --- 他のビュー（XMLにあれば） ---
    private lateinit var editEmail: EditText // (古いコードにあったもの)
    private lateinit var editHobby: EditText // (古いコードにあったもの)
    private lateinit var editFavorite: EditText // (古いコードにあったもの)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_detail)

        // どの項目を開いているか取得
        detailType = intent.getStringExtra("DETAIL_TYPE") ?: ""

        // ビューの初期化
        initViews()

        // トップバー設定 (保存・閉じるボタンの処理)
        setupTopBar()

        // Spinnerの項目設定（例: 1950年〜2025年を設定）
        // ※この関数は既に存在すると仮定
        setupSpinners()

        // ボトムナビゲーション設定
        // ※この関数は既に存在すると仮定
        setupBottomNavigation()

        // 保存されているデータを読み込み (新しいロジック)
        loadUserData()
    }

    /**
     * UI要素のIDを関連付ける
     * (※ここのIDをご自身のXMLファイルに合わせてください)
     */
    private fun initViews() {
        // XMLのIDと一致させてください
        editText = findViewById(R.id.edit_text)
        spinnerGender = findViewById(R.id.spinnerGender)

        // ※誕生日のSpinner 3つを囲む親レイアウト（LinearLayoutなど）のID
        layoutBirthdaySpinners = findViewById(R.id.layout_birthday_spinners)

        spinnerYear = findViewById(R.id.spinnerYear)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        spinnerDay = findViewById(R.id.spinnerDay)
        editEmail = findViewById(R.id.editEmail)
        editHobby = findViewById(R.id.editHobby)
        editFavorite = findViewById(R.id.editFavorite)
    }

    /**
     * トップバー（閉じる・保存）のクリック処理を設定
     */
    private fun setupTopBar() {
        // 閉じるボタン
        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
            finish()
        }

        // 保存ボタン (R.id.buttonSave)
        findViewById<TextView>(R.id.buttonSave).setOnClickListener {
            // 保存処理は非同期（launch）で行う
            lifecycleScope.launch {
                saveUserData() // 保存処理（下記で定義）
                finish()       // 保存が終わったら画面を閉じる
            }
        }
    }

    /**
     * データ読み込み (SharedPreferences -> DataRepository に変更)
     */
    private fun loadUserData() {
        // 最初にすべての関連UIを非表示にする
        editText.visibility = View.GONE
        spinnerGender.visibility = View.GONE
        layoutBirthdaySpinners.visibility = View.GONE

        lifecycleScope.launch {
            // 1. DataRepositoryから設定をロード
            val settings = repository.loadData().settings

            // 2. detailTypeに応じてUIを表示し、データをセット
            when (detailType) {
                "NAME" -> {
                    editText.visibility = View.VISIBLE
                    editText.setText(settings?.name ?: "")
                }
                "GENDER" -> {
                    spinnerGender.visibility = View.VISIBLE
                    val savedGender = settings?.gender
                    val genderPosition = when (savedGender) {
                        "男性" -> 1
                        "女性" -> 2
                        "その他" -> 3
                        "回答しない" -> 4
                        else -> 0 // 未選択
                    }
                    spinnerGender.setSelection(genderPosition)
                }
                "BIRTHDAY" -> {
                    layoutBirthdaySpinners.visibility = View.VISIBLE
                    val savedBirthday = settings?.birthday // 例: "2000/1/15"

                    if (!savedBirthday.isNullOrEmpty()) {
                        val parts = savedBirthday.split("/")
                        if (parts.size == 3) {
                            val year = parts[0]
                            val month = parts[1] // "1" や "12"
                            val day = parts[2]   // "1" や "31"

                            // SpinnerのAdapterから値を探してセット
                            setSpinnerSelection(spinnerYear, year)
                            setSpinnerSelection(spinnerMonth, month)

                            // ※もし日(Day)Spinnerの中身が月(Month)によって変わる場合、
                            // setupSpinners()側での対応が必要です。
                            // ここでは単純に値をセットしに行きます。
                            setSpinnerSelection(spinnerDay, day)
                        }
                    }
                }
                "HOBBY" -> {
                    editText.visibility = View.VISIBLE
                    editText.setText(settings?.hobby ?: "")
                }
                "FAVORITE" -> {
                    editText.visibility = View.VISIBLE
                    editText.setText(settings?.favorite ?: "")
                }
                // "EMAIL" など、古いコードにあって今はない項目は無視
            }
        }
    }

    /**
     * データ保存処理 (suspend関数として分離)
     */
    private suspend fun saveUserData() {
        // 1. まず現在の全データをロード
        val currentData = repository.loadData()
        // 2. 現在の設定をロード (なければ新しい設定を作る)
        val currentSettings = currentData.settings ?: UserSettings()

        // 3. detailTypeに応じて、"どのUIから"データを取得するか変える
        val newSettings = when (detailType) {
            "NAME" -> {
                currentSettings.copy(name = editText.text.toString())
            }
            "GENDER" -> {
                // "未選択" が選ばれていないかチェック（任意）
                val selectedGender = spinnerGender.selectedItem.toString()
                currentSettings.copy(gender = selectedGender)
            }
            "BIRTHDAY" -> {
                val year = spinnerYear.selectedItem.toString()
                val month = spinnerMonth.selectedItem.toString()
                val day = spinnerDay.selectedItem.toString()
                currentSettings.copy(birthday = "$year/$month/$day")
            }
            "HOBBY" -> {
                currentSettings.copy(hobby = editText.text.toString())
            }
            "FAVORITE" -> {
                currentSettings.copy(favorite = editText.text.toString())
            }
            else -> currentSettings // 不明な場合はそのまま
        }

        // 4. 新しい設定をセットした全データを保存
        repository.saveData(currentData.copy(settings = newSettings))
    }

    /**
     * Spinnerの値をテキストで検索してセットするヘルパー関数
     */
    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        if (value == null) return
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            // "1" と "1"、 "12" と "12" を正しく比較する
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                return // 見つかったら終了
            }
        }
        // 見つからなかった場合は 0 番目（"年"など）のまま
    }

    // --- setupSpinners() や setupBottomNavigation() はここ ---
    // (既にファイル内に存在すると仮定)
    private fun setupSpinners() {
        // ... (Spinnerに項目をセットするコード) ...
    }

    private fun setupBottomNavigation() {
        // ... (ボトムナビのコード) ...
    }
}