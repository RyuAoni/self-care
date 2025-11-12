package com.example.selfcare_android

import com.google.gson.annotations.SerializedName

// アプリ全体をまとめるクラス
data class AppData(
    @SerializedName("日記")
    val diaries: List<DiaryEntry> = emptyList(),

    @SerializedName("お手紙")
    val letters: List<WeeklyLetter> = emptyList(),

    @SerializedName("設定")
    val settings: UserSettings? = null
)

// 日記データ
data class DiaryEntry(
    @SerializedName("id") val id: String,
    @SerializedName("日記") val date: String,
    @SerializedName("会話データ") val conversation: List<ConversationEntry> = emptyList(),
    @SerializedName("歩数") val steps: String? = null,
    @SerializedName("日記") val diaryText: String? = null,
    @SerializedName("感情数値") val emotionScore: Double? = null,
    @SerializedName("ポジティブ") val positiveScore: Double? = null,
    @SerializedName("ネガティブ") val negativeScore: Double? = null
)

data class ConversationEntry(
    @SerializedName("コメンテーター") val speaker: String,
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