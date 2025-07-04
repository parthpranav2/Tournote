package com.example.tournote.Functionality.data

sealed class ChatItem {
    data class DateHeader(val label: String) : ChatItem()
    data class MessageItem(val message: ChatMessage) : ChatItem()
}

