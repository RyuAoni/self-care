package com.example.selfcare_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // Viewの参照を保持するViewHolder
    abstract class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(message: Message)
    }

    // ユーザーメッセージのViewHolder (item_message_user.xmlに対応)
    class UserMessageViewHolder(view: View) : MessageViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.text_message_body)
        override fun bind(message: Message) {
            messageText.text = message.text
        }
    }

    // AIメッセージのViewHolder (item_message_ai.xmlに対応)
    class AiMessageViewHolder(view: View) : MessageViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.text_message_body)
        override fun bind(message: Message) {
            messageText.text = message.text
            // 必要であればAIアバターの設定などをここに追加できます
        }
    }

    // メッセージタイプに応じて、使用するレイアウトを切り替える
    override fun getItemViewType(position: Int): Int {
        return messages[position].type
    }

    // ViewHolderを作成（レイアウトをインフレートする）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            MESSAGE_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            MESSAGE_TYPE_AI -> {
                val view = inflater.inflate(R.layout.item_message_ai, parent, false)
                AiMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    // データをViewHolderにバインドする
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    // リスト内のアイテム数を返す
    override fun getItemCount() = messages.size

    // 新しいメッセージを追加し、RecyclerViewを更新するヘルパーメソッド
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
        // 最新のメッセージにスクロール
        // この処理はActivity側で行う方がより一般的ですが、簡単のためActivityで実装します。
    }
}