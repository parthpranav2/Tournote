package com.example.tournote.Functionality.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Adapter.ChatAdapter
import com.example.tournote.Functionality.Repository.ChatRepository
import com.example.tournote.Functionality.data.ChatMessage
import io.socket.client.Ack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.AbstractCoroutineContextKey

class ChatViewModel: ViewModel() {

    private val repo: ChatRepository = ChatRepository()

    private val _incomeMessage = MutableLiveData<ChatMessage>()
    val incomeMessage : LiveData<ChatMessage>  get() = _incomeMessage

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> get()  = _messages


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val messageList = mutableListOf<ChatMessage>()

    private val _resendMsg = MutableLiveData<Boolean>(false)
    val resendMsg: LiveData<Boolean> get() = _resendMsg

    var currentRoomJoined: String? = null

    init {

    }


    fun sendMsg(userMessage: ChatMessage){
        Log.d("ChatDebug", "Sending message: $userMessage")
        if (userMessage.message_content?.trim().isNullOrEmpty()) return

        messageList.add(userMessage)
        _messages.value = messageList.toList()

        repo.sendMessage(userMessage, ack = {
            val res = it[0] as JSONObject
            if (res.optString("status") == "error"){
                _resendMsg.value = true
                Log.d("send_msg_db",res.optString("debug"))
            }else{
            }
        })
    }

    fun joinROOM(groupId: String){
        if (currentRoomJoined != groupId) {
            val data = JSONObject().put("id", groupId)
            repo.joinRoom(data,null)
            currentRoomJoined = groupId
        } else {
            Log.d("SocketJoin", "Already in room $groupId, skipping join")
        }
    }

    fun listenMsg() {
        repo.listenMessage { args ->
            val data = args[0] as JSONObject
            val msg = ChatMessage(
                message_id = data.optString("message_id"),
                message_content = data.optString("message_content"),
                user_name = data.optString("user_name"),
                group_id = data.optString("group_id"),
                user_id = data.optString("user_id"),
                timestamp = data.optLong("timestamp"),
                edited = data.optBoolean("edited"),
                isUser = false,
                profile_pic = data.optString("profile_pic")
            )

            // 👇 Switch to main thread for LiveData update
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    messageList.add(msg)
                    _messages.value = messageList.toList()
                }
            }
        }
    }


    fun getAllMsgFromDB(groupId: String, context: Context) {
        Log.d("ChatDebug", "Calling getAllMsgFromDB() for groupId: $groupId")

        viewModelScope.launch {
            try {
                val response = repo.getAllMsgByApi(groupId)
                if (response.isSuccessful) {
                    Log.d("ChatDebug", "✅ API call successful")

                    response.body()?.let { newMessages ->
                        Log.d("ChatDebug", "Received ${newMessages.size} messages from API")

                        withContext(Dispatchers.Main) {
                            // Clear and update the list
                            messageList.clear()
                            messageList.addAll(newMessages)

                            // Create a new list to trigger observer
                            _messages.value = messageList.toList()
                        }

                        Log.d("ChatDebug", "Updated _messages with ${messageList.size} messages")
                    } ?: run {
                        Log.d("ChatDebug", "Response body is null")
                        _error.value = "No data received"
                    }
                } else {
                    Log.d("ChatDebug", "❌ API call failed with code: ${response.code()}")
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.d("ChatDebug", "❗ Exception caught: ${e.message}")
                _error.value = e.message ?: "Failed to get messages"
            }
        }
    }
    fun clearError() {
        _error.value = null
    }

    fun clearResend() {
        _resendMsg.value = false
    }


}