package com.example.tournote.Functionality.Repository

import com.example.tournote.Functionality.APIClient
import com.example.tournote.Functionality.SocketManager
import com.example.tournote.Functionality.data.ChatMessage
import io.socket.client.Ack
import io.socket.emitter.Emitter
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response

class ChatRepository {


    fun connectSocket(){
        SocketManager.connect()
    }

    fun disconnectSocket(){
        SocketManager.disconnect()
    }

    fun sendMessage(data: ChatMessage,ack: Ack?=null){
        val msg_data = data.toJson()
        SocketManager.emit("new_message",msg_data,ack)
    }

    fun joinRoom(id: JSONObject, ack: Ack?=null){
        SocketManager.emit("join_room",id, ack)
    }

    fun listenMessage(listenMsg: Emitter.Listener){
        SocketManager.on("message_recieved", listenMsg)
    }

    suspend fun getAllMsgByApi(group_id: String): Response<List<ChatMessage>> {
        return APIClient.api_Interface.getAllMessages(group_id)
    }

}

    fun ChatMessage.toJson(): JSONObject {
        return JSONObject().apply {
            put("message_id", message_id)
            put("message_content", message_content)
            put("user_name", user_name)
            put("group_id", group_id)
            put("user_id", user_id)
            put("timestamp", timestamp)
            put("edited", edited)
            put("is_user", isUser)
            put("profile_pic", profile_pic)
        }
    }

