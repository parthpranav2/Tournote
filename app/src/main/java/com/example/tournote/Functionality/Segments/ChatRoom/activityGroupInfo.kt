package com.example.tournote.Functionality.Segments.ChatRoom

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.ChatRoom.Adapter.grpAdminsAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.Adapter.grpMemberAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.Adapter.grpOwnerAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.groupViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityGroupInfoBinding

class activityGroupInfo : AppCompatActivity() {
    private lateinit var binding: ActivityGroupInfoBinding
    private var MemberList = listOf<String>()
    private val viewModel: groupViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGroupInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userId = viewModel.authrepo.getUid()

        val grpData = GlobalClass.GroupDetails_Everything

        if (userId == grpData.owner.uid) {
            binding.btnDeleteGroup.visibility = View.VISIBLE
        }

        binding.txtGroupName.text = grpData.name
        binding.txtGroupDescription.text = grpData.description
        binding.createdAtDate.text = viewModel.formatTimestamp(grpData.createdAt!!)
        Glide.with(this)
            .load(grpData.profilePic)
            .placeholder(R.drawable.defaultgroupimage)
            .error(R.drawable.defaultgroupimage)
            .into(binding.grpProfileImage)

        val memberList = grpData.members
        val adminList = grpData.admins
        val owner = grpData.owner

        val ownerList = listOf(owner) // assuming owner is UserModel
        val allExcludedIds = (ownerList + adminList).map { it.uid }.toSet()
        val filteredMemberList = memberList.filterNot { it.uid in allExcludedIds }

        val ownerAdapter = grpOwnerAdapter(ownerList.toMutableList(), this)
        val adminAdapter = grpAdminsAdapter(adminList.toMutableList(), this)
        val memberAdapter = grpMemberAdapter(filteredMemberList.toMutableList(), this)

        val concatAdapter = ConcatAdapter(ownerAdapter, adminAdapter, memberAdapter)
        binding.rvMembersList.adapter = concatAdapter
        binding.rvMembersList.layoutManager = LinearLayoutManager(this)

        binding.rvMembersList.post {
            binding.txtMemberCount.text = concatAdapter.itemCount.toString()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }


    }
}