package com.example.tournote.Functionality.Fragments

import android.R.attr.clipToPadding
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.Functionality.Adapter.ChatAdapter
import com.example.tournote.Functionality.ViewModel.ChatViewModel
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.Functionality.data.ChatItem
import com.example.tournote.Functionality.data.ChatMessage
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.text.clear

class ChatsFragment : Fragment() {

    private val mainViewModel: MainActivityViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    lateinit var recyclerViewChat : RecyclerView
    lateinit var chatAdapter : ChatAdapter

    private val authViewmodel: authViewModel by viewModels()
    lateinit var editTextMessage: EditText
    lateinit var buttonSend : FloatingActionButton
    private var grp_id: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        recyclerViewChat = view.findViewById<RecyclerView>(R.id.recyclerViewChat)
        editTextMessage = view.findViewById<EditText>(R.id.editTextMessage)
        buttonSend = view.findViewById<FloatingActionButton>(R.id.buttonSend)

        chatAdapter = ChatAdapter(requireContext())
        mainViewModel.loadChatRoom()


        recyclerViewChat.adapter =chatAdapter
        recyclerViewChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        chatViewModel.getAllMsgFromDB(GlobalClass.group_id?:"",requireContext())
        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            val updated_msg = processMessages(messages)
            val itemList = groupMessagesByDate(updated_msg)
            Log.d("ChatDebug","lets see:${messages}")
            chatAdapter.submitList(itemList) {
                recyclerViewChat.scrollToPosition(messages.size - 1)
            }
        }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        chatViewModel.listenMsg()
        chatViewModel.listenMsgDelete()
        chatViewModel.listenMsgUpdate()

        return view
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val toolbar = view.findViewById<View>(R.id.toolbar)
//        val rootLayout = view.findViewById<View>(R.id.rootLayout)
//
//        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
//            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//            toolbar.setPadding(
//                toolbar.paddingLeft,
//                12,
//                toolbar.paddingRight,
//                toolbar.paddingBottom
//            )
//            insets
//        }
//    }




    private fun setupRecyclerView() {
//        recyclerViewChat.apply {
//            chatAdapter = ChatAdapter()
//            adapter = chatAdapter
//            layoutManager = LinearLayoutManager(requireContext()).apply {
//                stackFromEnd = true
//            }
//            clipToPadding = false
//        }


    }

    private fun processMessages(messages: List<ChatMessage>): List<ChatMessage> {
        val userID = authViewmodel.repo.getUid()
        for (message in messages) {
            if (message.user_id == userID) {
                message.isUser = true  // update field
            }else{
                message.isUser = false
            }
        }
        return messages
    }

    private fun setupClickListeners() {
        buttonSend.setOnClickListener {
            sendMsgTOView()
        }
            editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            // use when user presses "Send" on keyboard
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMsgTOView()
                true
            } else false
        }

        // Handle focus changes to scroll to bottom
        editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && ::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
                recyclerViewChat.post {
                    recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun observeViewModel() {

        chatViewModel.resendMsg.observe(viewLifecycleOwner) { it ->
            if (it == true){
                Toast.makeText(requireContext(), "Error: Message is not saved", Toast.LENGTH_SHORT).show()
                chatViewModel.clearResend()
            }
        }

        mainViewModel.groupId.observe(viewLifecycleOwner) {
            grp_id = it
        }

        chatViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            buttonSend.isEnabled = !isLoading
        }

        chatViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                chatViewModel.clearError()
            }
        }
    }
    fun generateShortMessageId(): String {
        return UUID.randomUUID().toString().take(8)
    }



    fun sendMsgTOView(){

        val msg_content = editTextMessage.text.toString()

        if (msg_content.trim().isNotEmpty()) {

            CoroutineScope(Dispatchers.Main).launch {
                val userID = authViewmodel.repo.getUid()
                if (userID != null) {
                    val user_data = authViewmodel.repo.userDetailGetLogin(userID)
                    if (user_data != null && GlobalClass.group_id != null) {
                        val user_name = user_data.get("name").toString()
                        val profile_url: String? = user_data.get("profilePic").toString()
                        val group_id = GlobalClass.group_id
                        val msg_id = generateShortMessageId()
                        val msg = ChatMessage(
                            msg_content, user_name, group_id, userID, profile_pic = profile_url, message_id = msg_id,isUser = true
                        )
                        chatViewModel.sendMsg(msg)
                    }else{
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(requireContext(), "UserID not found", Toast.LENGTH_SHORT).show()
                }
            }

            editTextMessage.text?.clear()

        }

    }

    fun groupMessagesByDate(messages: List<ChatMessage>): List<ChatItem> {
        val result = mutableListOf<ChatItem>()
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val labelFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        var lastDateKey: String? = null

        for (msg in messages.sortedBy { it.timestamp }) {
            val msgDate = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
            val msgKey = dateFormatter.format(msgDate.time)

            if (msgKey != lastDateKey) {
                val label = when (msgKey) {
                    dateFormatter.format(today.time) -> "Today"
                    dateFormatter.format(yesterday.time) -> "Yesterday"
                    else -> labelFormatter.format(msgDate.time)
                }
                result.add(ChatItem.DateHeader(label))
                lastDateKey = msgKey
            }

            result.add(ChatItem.MessageItem(msg))
        }

        return result
    }


}