package com.example.tournote.Groups.Activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Groups.Adapter.commonGrpAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.GroupMemberData
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.groupViewModel
import com.example.tournote.R
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityProfileInfoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class activityProfileInfo : AppCompatActivity() {
    private lateinit var binding: ActivityProfileInfoBinding
    private val viewModel: groupViewModel by viewModels()
    private var commonGrpList = listOf<String>()
    private var commonGrpNameList = mutableListOf<GroupMemberData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userData = intent.getParcelableExtra<UserModel>("user")

        binding.txtName.text = userData?.name
        binding.txtEmail.text = userData?.email
        binding.txtPhoneNumber.text = userData?.phoneNumber
        binding.createdAtDate.text = viewModel.formatTimestamp(System.currentTimeMillis())
        Glide.with(this)
            .load(userData?.profilePic)
            .placeholder(R.drawable.imageselector)
            .error(R.drawable.imageselector)
            .into(binding.imgProfilePhoto)

        viewModel.loadUserGroupList(userData?.uid.toString())
        viewModel.loadThisUserGroupList()



        viewModel.userGroupList.observe(this) { list1 ->
            viewModel.thiSuserGroupList.observe(this) { list2 ->
                val commonGrpList = list1.intersect(list2).toList()
                val tempList = mutableListOf<GroupMemberData>()

                Log.d("grpadapter", commonGrpList.toString())
                CoroutineScope(Dispatchers.IO).launch {
                    for (groupId in commonGrpList) {
                        val result = viewModel.repo.groupData(groupId)
                        result.onSuccess {
                            Log.d("grpadapter", it.name)
                            tempList.add(GroupMemberData(it.name, it.profilePic))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Log.d("grpadapter", tempList.toString())
                        commonGrpNameList.clear()
                        commonGrpNameList.addAll(tempList)
                        binding.rvMembersList.adapter = commonGrpAdapter(commonGrpNameList, this@activityProfileInfo)
                        binding.rvMembersList.layoutManager = LinearLayoutManager(this@activityProfileInfo)
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }








    }
}