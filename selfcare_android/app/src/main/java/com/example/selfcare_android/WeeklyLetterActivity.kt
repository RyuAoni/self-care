package com.example.selfcare_android

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
            WeeklyLetter("11/03~11/09", "11/03~11/9の\n週次お手紙", "○○さん、\n1週間お疲れ様でした！"),
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
        findViewById<ImageView>(R.id.navStats).setOnClickListener {
            // TODO: 統計画面へ遷移
            Toast.makeText(this, "統計画面", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navCalendar).setOnClickListener {
            // TODO: カレンダー画面へ遷移
            Toast.makeText(this, "カレンダー画面", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
            finish()
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