package com.example.selfcare_android

/**
 * 簡易的な感情分析を行うオブジェクト
 * 文章内のポジティブ・ネガティブな単語をカウントしてスコア化します。
 */
object EmotionAnalyzer {

    // ポジティブな単語リスト（必要に応じて追加してください）
    private val positiveWords = listOf(
        "楽しい", "嬉しい", "幸せ", "最高", "好き", "愛", "感謝", "元気",
        "成功", "美しい", "素晴らしい", "笑顔", "安心", "希望", "良い",
        "おいしい", "ラッキー", "感動", "わくわく", "達成", "ポジティブ"
    )

    // ネガティブな単語リスト（必要に応じて追加してください）
    private val negativeWords = listOf(
        "悲しい", "辛い", "苦しい", "最悪", "嫌", "憎", "不安", "疲れた",
        "失敗", "怒り", "後悔", "絶望", "悪い", "痛い", "孤独",
        "憂鬱", "イライラ", "無理", "しんどい", "ネガティブ", "ごめん"
    )

    /**
     * テキストを分析して -1.0(最悪) 〜 1.0(最高) のスコアを返す
     */
    fun analyze(text: String): Double {
        var score = 0.0

        // 分析用に一応正規化（空白削除など）
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return 0.0

        // ポジティブ単語のカウント
        var posCount = 0
        positiveWords.forEach { word ->
            // 単純な文字列検索 (contains)
            if (normalizedText.contains(word)) {
                posCount++
                // 同じ単語が複数回出てくる場合の考慮をするなら Regex 等が必要だが、
                // 今回は簡易的に「含まれているか」で加点
                // もし「とても楽しい」などで加点を増やしたい場合はロジックを追加
            }
        }

        // ネガティブ単語のカウント
        var negCount = 0
        negativeWords.forEach { word ->
            if (normalizedText.contains(word)) {
                negCount++
            }
        }

        // スコア計算: (ポジティブ - ネガティブ)
        // 1単語あたり 0.2 ポイント変動させてみる
        val rawScore = (posCount - negCount) * 0.2

        // 範囲を -1.0 〜 1.0 に制限する (clamp)
        score = rawScore.coerceIn(-1.0, 1.0)

        return score
    }
}