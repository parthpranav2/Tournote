package com.example.tournote.Functionality.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.ViewModel.ChatViewModel
import com.example.tournote.Functionality.data.ChatMessage
import com.example.tournote.R
import com.google.api.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(val context: android.content.Context): ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }

            VIEW_TYPE_OTHER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_other, parent, false)
                OtherMessageViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message)
        }
    }

    // Moved formatTime function inside the class
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    inner class UserMessageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView = itemView.findViewById<TextView>(R.id.messageText)
        private val timestampTextView = itemView.findViewById<TextView>(R.id.timeText)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.message_content
            timestampTextView.text = formatTime(message.timestamp)
        }
    }

    inner class OtherMessageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView = itemView.findViewById<TextView>(R.id.messageText)
        private val timestampTextView = itemView.findViewById<TextView>(R.id.timeText)
        private val userNameTextView = itemView.findViewById<TextView>(R.id.userNameText)
        private val profilePicImageView = itemView.findViewById<android.widget.ImageView>(R.id.user_avatar)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.message_content
            timestampTextView.text = formatTime(message.timestamp)
            userNameTextView.text = message.user_name

            // Simplified Glide usage - placeholder and error handle null/empty URLs
            Glide.with(context)
                .load(message.profile_pic)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .into(profilePicImageView)
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.message_id == newItem.message_id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}