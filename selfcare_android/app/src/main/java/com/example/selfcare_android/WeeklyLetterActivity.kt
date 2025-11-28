package com.example.selfcare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WeeklyLetterActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeeklyLetterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_letter)

        // 戻るボタン
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // RecyclerViewの設定
        setupRecyclerView()

        // ボトムナビゲーションの設定
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.letterRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ダミーデータを作成
        val letters = listOf(
            WeeklyLetter("11/03~11/09", "11/03~11/9の\n週次お手紙", "一週間分の日記、拝見しました。\n" +
                    "\n" +
                    "穏やかな日もあれば、疲れのにじむ日もあり、少しだけ嬉しい出来事が混じっている日もありましたね。あなたが日々の小さな変化を丁寧に記録しているのがよく伝わってきました。\n" +
                    "\n" +
                    "仕事では問題の切り分けや予期せぬトラブルに追われた日もありましたが、そのたびに原因を着実に探し当てていたのが印象的でした。外から見れば些細に思える作業でも、積み重ねが確かな前進になっているように感じます。\n" +
                    "\n" +
                    "気分転換の散歩や買い物、食事の選択にもその時々の心の揺れが表れていて、人間らしいリズムが心地良かったです。疲れた日の静かな夜や、ふと笑える場面も、読む側として温かくなりました。\n" +
                    "\n" +
                    "この一週間があなたにとってどんな意味を持っていたのか、すべてを言葉にする必要はありません。ただ、あなたが感じたことや考えたことがそうして日記として形になり、誰かに届いているという事実そのものに価値があります。\n" +
                    "\n" +
                    "また何かを書きたくなったら、いつでも読ませてください。"),
            WeeklyLetter("11/10~11/16", "11/10~11/16の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("11/17~11/23", "11/17~11/23の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("11/24~11/30", "11/24~11/30の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("12/01~12/07", "12/01~12/07の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("12/08~12/14", "12/08~12/14の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("12/15~12/21", "12/15~12/21の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("12/22~12/28", "12/22~12/28の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
            WeeklyLetter("12/29~01/04", "12/29~01/04の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！")
        )

        adapter = WeeklyLetterAdapter(letters) { letter ->
            showLetterDialog(letter)
        }
        recyclerView.adapter = adapter
    }

    private fun showLetterDialog(letter: WeeklyLetter) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_weekly_letter, null)

        dialogView.findViewById<TextView>(R.id.letterTitle).text = letter.title
        dialogView.findViewById<TextView>(R.id.letterContent).text = letter.content

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("閉じる", null)
            .show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // 初期選択を解除する
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)


        bottomNav.setOnItemSelectedListener { item ->
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
}

data class WeeklyLetter(
    val period: String,
    val title: String,
    val content: String
)

class WeeklyLetterAdapter(
    private val letters: List<WeeklyLetter>,
    private val onItemClick: (WeeklyLetter) -> Unit
) : RecyclerView.Adapter<WeeklyLetterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val periodText: TextView = view.findViewById(R.id.periodText)
        val cardView: CardView = view.findViewById(R.id.letterCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_letter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val letter = letters[position]
        holder.periodText.text = letter.period
        holder.cardView.setOnClickListener {
            onItemClick(letter)
        }
    }

    override fun getItemCount() = letters.size
}