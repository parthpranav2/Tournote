package com.example.tournote.Functionality.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.Functionality.Adapter.grpAdminsAdapter
import com.example.tournote.Functionality.Adapter.grpMemberAdapter
import com.example.tournote.Functionality.Adapter.grpOwnerAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.groupViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.GroupSelector.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGroupInfoBinding
import kotlinx.coroutines.launch

class activityGroupInfo : AppCompatActivity() {
    private lateinit var binding: ActivityGroupInfoBinding
    private var MemberList = listOf<String>()
    private val viewModel: groupViewModel by viewModels()

    private val mainRepo = MainActivityRepository()
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
        val currentUser = GlobalClass.Me // Local immutable copy for smart cast

        // Ensure currentUserEmail is derived from the local copy
        val currentUserEmail = currentUser?.email

        val isTracked = (currentUserEmail != null &&
                grpData.trackFriends?.contains(currentUserEmail) == true)

        binding.btnLeaveGroup.setOnClickListener {
            lifecycleScope.launch {
                mainRepo.LeaveCurrentGroup()
                redirectToActivity(GroupSelectorActivity::class.java)
            }
        }

        lifecycleScope.launch {
            if (isTracked && currentUser?.uid != grpData.owner.uid) {
                binding.btnDisableTracking.visibility = View.VISIBLE

                binding.btnDisableTracking.setOnClickListener {
                    lifecycleScope.launch {
                        mainRepo.DisableMyTrackingOnCurrentGroup() // This updates Firebase
                        binding.btnDisableTracking.visibility = View.GONE

                        val currentTrackFriends = grpData.trackFriends?.toMutableList()

                        if (currentUserEmail != null && currentTrackFriends != null) {
                            if (currentTrackFriends.remove(currentUserEmail)) {
                                GlobalClass.GroupDetails_Everything = grpData.copy(
                                    trackFriends = currentTrackFriends
                                )
                            }
                        }
                    }
                }
            } else {
                binding.btnDisableTracking.visibility = View.GONE
            }
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

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}