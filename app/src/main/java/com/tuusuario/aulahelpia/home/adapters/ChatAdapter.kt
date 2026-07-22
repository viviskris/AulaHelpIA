package com.tuusuario.aulahelpia.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuusuario.aulahelpia.R

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun addMessages(newMessages: List<ChatMessage>) {
        val startPosition = messages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(startPosition, newMessages.size)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text

            // Configurar estilo según quién envía
            val params = tvMessage.layoutParams as ViewGroup.MarginLayoutParams
            if (message.isUser) {
                // Mensaje del usuario (derecha)
                tvMessage.setBackgroundResource(R.drawable.bg_chat_user)
                params.marginStart = 100
                params.marginEnd = 0
            } else {
                // Mensaje del bot (izquierda)
                tvMessage.setBackgroundResource(R.drawable.bg_chat_bot)
                params.marginStart = 0
                params.marginEnd = 100
            }
            tvMessage.layoutParams = params
        }
    }
}