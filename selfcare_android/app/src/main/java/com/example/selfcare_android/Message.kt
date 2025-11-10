package com.example.selfcare_android

// メッセージの送信者を識別するための定数
const val MESSAGE_TYPE_USER = 0
const val MESSAGE_TYPE_AI = 1

data class Message(
    val text: String,        // メッセージの本文
    val type: Int,           // 送信者 (MESSAGE_TYPE_USER または MESSAGE_TYPE_AI)
    val timestamp: Long = System.currentTimeMillis() // 送信時刻
)