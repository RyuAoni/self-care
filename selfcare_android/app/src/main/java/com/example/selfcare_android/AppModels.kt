package com.example.selfcare_android

import com.google.gson.annotations.SerializedName
import com.example.selfcare_android.MESSAGE_TYPE_USER
import com.example.selfcare_android.MESSAGE_TYPE_AI

/**
 * アプリ全体のデータ構造
 * {
 * “日記”: [ ... ],
 * “お手紙”: ”...”
 * }
 */
data class AppData(
    @SerializedName("日記")
    val diaries: MutableList<DiaryEntry> = mutableListOf(),

    // ★変更: Stringではなく、手紙オブジェクトのリストにします
    @SerializedName("お手紙リスト")
    val weeklyLetters: MutableList<WeeklyLetterData> = mutableListOf()
)

/**
 * 日記1件あたりのデータ構造
 */
data class DiaryEntry(
    @SerializedName("id")
    val id: String,

    @SerializedName("日付")
    val date: String,

    @SerializedName("会話データ")
    val conversations: List<ConversationData>,

    @SerializedName("歩数")
    val stepCount: String,

    @SerializedName("日記")
    val diaryContent: String,

    @SerializedName("感情数値")
    val emotionScore: String,

    @SerializedName("ポジティブ")
    val positiveScore: String,

    @SerializedName("ネガティブ")
    val negativeScore: String
)

/**
 * 会話データ
 * {“コメンテーター”:”user”, “本文”:”こんにちは”}
 */
data class ConversationData(
    @SerializedName("commentator")
    val commentator: String, // "user" または "ai"

    @SerializedName("本文")
    val text: String
)

/**
 * ★追加: 週次お手紙のデータ構造
 */
data class WeeklyLetterData(
    @SerializedName("id")
    val id: String,

    @SerializedName("期間")
    val period: String, // 例: "2025/11/03~2025/11/09"

    @SerializedName("タイトル")
    val title: String,

    @SerializedName("内容")
    val content: String
)

// Message.ktにある定数を利用
// const val MESSAGE_TYPE_USER = 0
// const val MESSAGE_TYPE_AI = 1

/**
 * 保存用データ(String) -> アプリ用データ(Int) へ変換
 */
fun ConversationData.toMessage(): Message {
    val typeInt = when (this.commentator) {
        "user" -> MESSAGE_TYPE_USER
        "ai" -> MESSAGE_TYPE_AI
        else -> MESSAGE_TYPE_AI // 不明な場合はとりあえずAI扱いなどにする
    }
    // 日付などのタイムスタンプが必要なら現在時刻や別途保存した時刻を入れる
    return Message(text = this.text, type = typeInt)
}

/**
 * アプリ用データ(Int) -> 保存用データ(String) へ変換
 */
fun Message.toConversationData(): ConversationData {
    val commentatorString = when (this.type) {
        MESSAGE_TYPE_USER -> "user"
        MESSAGE_TYPE_AI -> "ai"
        else -> "unknown"
    }
    return ConversationData(commentator = commentatorString, text = this.text)
}