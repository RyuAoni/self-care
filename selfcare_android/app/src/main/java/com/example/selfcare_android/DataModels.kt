package com.example.selfcare_android

import com.google.gson.annotations.SerializedName

// アプリ全体をまとめるクラス
data class AppData(
    @SerializedName("日記")
    val diaries: MutableList<DiaryEntry> = mutableListOf(),

    @SerializedName("お手紙")
    val letters: List<WeeklyLetter> = emptyList(),

    @SerializedName("設定")
    val settings: UserSettings? = null
)

// 日記データ
data class DiaryEntry(
    @SerializedName("id") val id: String,
    @SerializedName("日付") val date: String,
    @SerializedName("会話データ") val conversations: List<ConversationData> = emptyList(),
    @SerializedName("歩数") val stepCount: String? = null,
    @SerializedName("日記") val diaryContent: String? = null,
    @SerializedName("感情数値") val emotionScore: String? = null,
    @SerializedName("ポジティブ") val positiveScore: String? = null,
    @SerializedName("ネガティブ") val negativeScore: String? = null
)

// 会話データ
data class ConversationData(
    @SerializedName("コメンテーター") val commentator: String, // "user" または "ai"
    @SerializedName("本文") val text: String
)

// お手紙データ
data class WeeklyLetter(
    @SerializedName("id") val id: String,
    @SerializedName("週範囲") val dateRange: String,
    @SerializedName("本文") val content: String
)

// 設定データ
data class UserSettings(
    @SerializedName("名前") val name: String = "未設定",
    @SerializedName("アイコンパス") val iconPath: String? = null,
    @SerializedName("性別") val gender: String? = null,
    @SerializedName("生年月日") val birthday: String? = null,
    @SerializedName("趣味") val hobby: String? = null,
    @SerializedName("すきなもの") val favorite: String? = null
)

/**
 * 保存用データ(String) -> アプリ用データ(Int) へ変換
 */
fun ConversationData.toMessage(): Message {
    val typeInt = when (this.commentator) {
        "user" -> MESSAGE_TYPE_USER
        "ai" -> MESSAGE_TYPE_AI
        else -> MESSAGE_TYPE_AI
    }
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
