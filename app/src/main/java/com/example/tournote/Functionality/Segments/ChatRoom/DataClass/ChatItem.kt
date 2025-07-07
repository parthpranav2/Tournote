package com.example.tournote.Functionality.Segments.ChatRoom.DataClass

sealed class ChatItem {
    data class DateHeader(val label: String) : ChatItem()
    data class MessageItem(val message: ChatMessage) : ChatItem()
}

