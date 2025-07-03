package com.example.tournote.Functionality.data

import java.util.UUID

data class ChatMessage(
    var message_content: String? = null,
    var user_name: String? = null,
    var group_id: String? = null,
    var user_id: String? = null,
    var timestamp: Long = System.currentTimeMillis(),
    var edited: Boolean = false,
    var isUser: Boolean = false,
    var message_id: String? = " ",
    var profile_pic: String? = null
){

    companion object{
        fun createUserMessage(message: String, userName: String, groupId: String, userId: String, profilePic: String?,edited: Boolean): ChatMessage {
            return ChatMessage(
                message_content = message,
                user_name = userName,
                group_id = groupId,
                user_id = userId,
                profile_pic = profilePic,
                edited = edited,
                isUser = true
            )
        }

        fun createOtherMessage(message: String,userName: String,groupId: String, userId: String, profilePic: String?,edited: Boolean): ChatMessage {
            return ChatMessage(
                message_content = message,
                user_name = userName,
                group_id = groupId,
                user_id = userId,
                profile_pic = profilePic,
                edited = edited,
                isUser = false
            )
        }
    }
}

