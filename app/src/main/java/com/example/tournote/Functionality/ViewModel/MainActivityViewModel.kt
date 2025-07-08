package com.example.tournote.Functionality.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Segments.ChatRoom.Repository.ChatRepository
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.ChatViewModel
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private val repo = MainActivityRepository()
    val chatRepo = ChatRepository()
    val chatView = ChatViewModel()

    private val _groupInfo = MutableLiveData<Result<GroupData_Detailed_Model>>()
    val groupInfo: LiveData<Result<GroupData_Detailed_Model>> = _groupInfo

    private val _groupId = MutableLiveData<String?>(null)
    val groupId: LiveData<String?> = _groupId

    fun loadGroup() {
        viewModelScope.launch {
            val group = GlobalClass.GroupDetails_Everything

            _groupId.value = group.groupID
            _groupInfo.value = Result.success(group)

            // 🔐 Now safe to call after data is ready
            chatRepo.connectSocket(group.groupID!!)
        }
    }

    fun loadChatRoom() {
        viewModelScope.launch {
            chatView.joinROOM(GlobalClass.GroupDetails_Everything.groupID!!)
        }
    }
}
