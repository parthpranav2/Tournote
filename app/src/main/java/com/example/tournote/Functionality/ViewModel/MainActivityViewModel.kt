package com.example.tournote.Functionality.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Repository.ChatRepository
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GroupInfoModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivityViewModel: ViewModel() {

    val repo = MainActivityRepository()
    val chatRepo = ChatRepository()
    val chatView = ChatViewModel()



    private val _groupInfo = MutableLiveData<Result<GroupInfoModel>>()
    val groupInfo: LiveData<Result<GroupInfoModel>> = _groupInfo

    private val _groupId = MutableLiveData<String>(null)
    val groupId: LiveData<String> = _groupId

    init {
        chatRepo.connectSocket()
    }


    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _groupId.value=groupId
            val result = repo.groupData(groupId)
            _groupInfo.value = result
        }
    }

    fun loadChatRoom() {
        viewModelScope.launch {
            chatView.joinROOM(groupId.toString())
        }
    }

}