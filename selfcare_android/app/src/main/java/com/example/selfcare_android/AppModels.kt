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
    @SerializedName("お手紙リスト")
    val weeklyLetters: MutableList<WeeklyLetterData> = mutableListOf()
)

/**
 * 日記1件あたりのデータ構造
 */
data class DiaryEntry(
    @SerializedName("id") val id: String,
    @SerializedName("日付") val date: String,
    @SerializedName("会話データ") val conversations: List<ConversationData>,
    @SerializedName("歩数") val stepCount: String,
    @SerializedName("日記") val diaryContent: String, // ★
    // ★追加: 画像の保存先パス (nullなら画像なし)
    @SerializedName("画像パス") val imagePath: String? = null,
    @SerializedName("感情数値") val emotionScore: String,
    @SerializedName("ポジティブ") val positiveScore: String,
    @SerializedName("ネガティブ") val negativeScore: String
)

/**
 * 会話データ
 * {“コメンテーター”:”user”, “本文”:”こんにちは”}
 */
data class ConversationData(
    @SerializedName("commentator") val commentator: String,
    @SerializedName("本文") val text: String
)

/**
 * ★追加: 週次お手紙のデータ構造
 */
data class WeeklyLetterData(
    @SerializedName("id") val id: String,
    @SerializedName("期間") val period: String,
    @SerializedName("タイトル") val title: String,
    @SerializedName("内容") val content: String
)

// Message.ktにある定数を利用
// const val MESSAGE_TYPE_USER = 0
// const val MESSAGE_TYPE_AI = 1

/**
 * 保存用データ(String) -> アプリ用データ(Int) へ変換
 */
fun ConversationData.toMessage(): Message {
    val typeInt = if (this.commentator == "user") MESSAGE_TYPE_USER else MESSAGE_TYPE_AI
    return Message(text = this.text, type = typeInt)
}

/**
 * アプリ用データ(Int) -> 保存用データ(String) へ変換
 */
fun Message.toConversationData(): ConversationData {
    val commentatorString = if (this.type == MESSAGE_TYPE_USER) "user" else "ai"
    return ConversationData(commentator = commentatorString, text = this.text)
}