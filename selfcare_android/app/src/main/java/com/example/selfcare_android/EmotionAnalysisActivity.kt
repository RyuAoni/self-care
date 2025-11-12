// main/java/com/example/selfcare_android/EmotionAnalysisActivity.kt

package com.example.selfcare_android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.selfcare_android.databinding.ActivityEmotionAnalysisBinding

/**
 * 感情分析画面 (EmotionAnalysisActivity)
 * 日次・週次の感情推移と歩数をグラフで表示し、心身の相関関係への気づきを促す。
 */
class EmotionAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmotionAnalysisBinding
    private var isWeeklyView = true
    private var isEmotionScoreView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmotionAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAnalysisData()

        // 期間切り替えボタンのリスナーを設定 (1週間/1ヶ月)
        binding.togglePeriodButton.setOnClickListener {
            isWeeklyView = !isWeeklyView
            binding.togglePeriodButton.text = if (isWeeklyView) "期間: 1週間" else "期間: 1ヶ月"
            loadAnalysisData()
            Toast.makeText(this, "表示期間を${if (isWeeklyView) "1週間" else "1ヶ月"}に切り替えました", Toast.LENGTH_SHORT).show()
        }

        // スコア表示切り替えボタンを設定 (感情/総合)
        binding.toggleCompositeScoreButton.setOnClickListener {
            isEmotionScoreView = !isEmotionScoreView
            binding.toggleCompositeScoreButton.text = if (isEmotionScoreView) "スコア: 感情" else "スコア: 総合"
            loadAnalysisData()
            Toast.makeText(this, "表示スコアを${if (isEmotionScoreView) "感情スコア" else "総合スコア"}に切り替えました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAnalysisData() {
        // TODO: DBから感情スコアと歩数データを取得し、グラフビューにデータを渡すロジック
        // 現在のisWeeklyViewとisEmotionScoreViewに基づいてデータをロード・描画する。
    }
}