package com.sabah.bikhushue

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvMessageInner: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cvMessageInner)
        val cvMessageShadow: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cvMessageShadow)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessage.text = msg.text
        
        val themeColors = ThemeHelper.getThemeColors(holder.itemView.context)
        
        // Common colors for the card
        holder.cvMessageInner.setCardBackgroundColor(themeColors.cardBg)
        holder.cvMessageInner.strokeColor = themeColors.bar
        holder.cvMessageShadow.setCardBackgroundColor(themeColors.shadow)
        
        if (msg.isUser) {
            holder.tvMessage.setTextColor(themeColors.txt)
            // You can customize user's text appearance further if needed
        } else {
            holder.tvMessage.setTextColor(themeColors.txt)
        }
    }

    override fun getItemCount() = messages.size
    
    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }
}
