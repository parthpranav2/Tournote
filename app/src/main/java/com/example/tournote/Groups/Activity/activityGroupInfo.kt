package com.example.tournote.Groups.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.Groups.Adapter.grpAdminsAdapter
import com.example.tournote.Groups.Adapter.grpMemberAdapter
import com.example.tournote.Groups.Adapter.grpOwnerAdapter
import com.example.tournote.Functionality.AddUsersToGroup_GroupInfoRecyclerViewAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.groupViewModel
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityGroupInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.getValue

class activityGroupInfo : AppCompatActivity() {
    private lateinit var binding: ActivityGroupInfoBinding
    private var MemberList = listOf<String>()
    private val viewModel: groupViewModel by viewModels()

    private val mainRepo = MainActivityRepository()
    private lateinit var adapter: AddUsersToGroup_GroupInfoRecyclerViewAdapter

    private var usersIn: List<UserModel> = listOf()

    private var initialAddMembersClick =false

    private val viewModel1: GroupSelectorActivityViewModel by viewModels()

    private val viewModel2: MainActivityViewModel by viewModels()

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

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        if((GlobalClass.GroupDetails_Everything.owner== GlobalClass.Me)||(GlobalClass.GroupDetails_Everything.admins.contains(
                GlobalClass.Me))){
            binding.btnAddMembers.visibility=View.VISIBLE
        }else{
         binding.btnAddMembers.visibility=View.GONE
        }

        val userId = viewModel.authrepo.getUid()

        val grpData = GlobalClass.GroupDetails_Everything
        val currentUser = GlobalClass.Me // Local immutable copy for smart cast

        if(grpData.owner.uid==currentUser?.uid){
            binding.btnDeleteGroup.visibility=View.VISIBLE
            binding.btnLeaveGroup.visibility=View.GONE
            if(GlobalClass.GroupDetails_Everything.isGroupValid?:false){
                binding.btnEndTrip.visibility=View.VISIBLE
            }else{
                binding.btnEndTrip.visibility=View.GONE
            }

            binding.btnDeleteGroup.setOnClickListener {
                showDeleteGrpConfirmationBsFragment {res->
                    if(res){
                        lifecycleScope.launch {
                            binding.progressBar.visibility=View.VISIBLE
                            mainRepo.DeleteCurrentGroupFromRoot()
                            redirectToActivity(GroupSelectorActivity::class.java)
                            binding.progressBar.visibility=View.GONE
                        }
                    }
                }
            }
        }else{
            binding.btnDeleteGroup.visibility=View.GONE
            binding.btnLeaveGroup.visibility=View.VISIBLE
            binding.btnEndTrip.visibility=View.GONE
        }


        // Ensure currentUserEmail is derived from the local copy
        val currentUserEmail = currentUser?.email

        val isTracked = (currentUserEmail != null &&
                grpData.trackFriends.any { it.email == currentUser.email } == true)


        binding.btnAddMembers.setOnClickListener {
            if(!initialAddMembersClick){
                binding.relLayoutMemberSelector.visibility=View.VISIBLE
                initialAddMembersClick=true
            }else{
                //final click before adding members
                binding.relLayoutMemberSelector.visibility=View.GONE
                if(usersIn.isNotEmpty()){
                    lifecycleScope.launch { // Launch a single coroutine for the entire process
                        binding.progressBar.visibility = View.VISIBLE // Show progress bar

                        val addMemberJobs = usersIn.map { user ->
                            async { // Use async to run additions concurrently and get a Deferred object
                                mainRepo.AddMemberToGroup(user)
                            }
                        }
                        addMemberJobs.awaitAll() // Wait for all member additions to complete

                        setupMembersList(GlobalClass.GroupDetails_Everything)

                        binding.progressBar.visibility = View.GONE // Hide progress bar

                        usersIn = listOf() // Clear the list

                        initialAddMembersClick=false

                        viewModel1.fetchAllActiveUsers()
                    }
                }
            }
        }


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

                        val currentTrackFriends = grpData.trackFriends.toMutableList() // .trackFriends is now non-nullable List<UserModel>

                        if (currentUserEmail != null) {
                            // Find the UserModel to remove based on email
                            val userToRemove = currentTrackFriends.find { it.email == currentUserEmail }
                            if (userToRemove != null) {
                                if (currentTrackFriends.remove(userToRemove)) {
                                    GlobalClass.GroupDetails_Everything = grpData.copy(
                                        trackFriends = currentTrackFriends
                                    )
                                }
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

        setupMembersList(grpData)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEndTrip.setOnClickListener {
            showEndTripConfirmationBsFragment { res->
                if(res) {
                    lifecycleScope.launch {
                        binding.btnEndTrip.visibility=View.GONE
                        mainRepo.EndTour()
                        viewModel2.turnOffGroupValidity()
                    }
                }
            }
        }



// Setup user lists
        binding.rvActiveUserList.layoutManager = LinearLayoutManager(this)

        viewModel1.fetchAllActiveUsers()

        viewModel1.users.observe(this) { allUsers ->
            // Assuming both your User and Member objects have a unique 'id' or 'userId' property.
            // Replace 'it.id' and 'user.id' with the actual property names if different (e.g., 'it.userId').

            // 1. Create a Set of unique IDs from the existing members.
            val currentMemberIds = GlobalClass.GroupDetails_Everything.members.map { it.uid }.toSet()

            // 2. Filter 'allUsers' by checking if their ID is NOT in the 'currentMemberIds' Set.
            val nonMemberUsers = allUsers.filter { user ->
                !currentMemberIds.contains(user.uid)
            }

            // Pass the filtered list to the adapter
            adapter = AddUsersToGroup_GroupInfoRecyclerViewAdapter(this, nonMemberUsers, viewModel1)
            binding.rvActiveUserList.adapter = adapter

            binding.txtSearch.addTextChangedListener { editable ->
                val query = editable?.toString() ?: ""
                // Ensure your adapter's filter method filters the 'nonMemberUsers' list internally.
                adapter.filter(query)
            }
        }

        viewModel1.usersIn.observe(this) { users ->
            usersIn = users
        }
    }

    private fun showEndTripConfirmationBsFragment(onResult: (Boolean) -> Unit) {
        val dialog = BottomSheetDialog(this).apply {
            setContentView(R.layout.bsfragment_endtrack_confirmation)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnConfirm)

        btnConfirm?.setOnClickListener {
            dialog.dismiss()
            onResult(true) // ✅ Button was clicked
        }

        dialog.setOnDismissListener {
            onResult(false) // ❌ Dialog dismissed without button click
        }

        dialog.show()
    }

    private fun showDeleteGrpConfirmationBsFragment(onResult: (Boolean) -> Unit) {
        val dialog = BottomSheetDialog(this).apply {
            setContentView(R.layout.bsfragment_deletegrp_confirmation)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnConfirm)

        btnConfirm?.setOnClickListener {
            dialog.dismiss()
            onResult(true) // ✅ Button was clicked
        }

        dialog.setOnDismissListener {
            onResult(false) // ❌ Dialog dismissed without button click
        }

        dialog.show()
    }


    private fun setupMembersList(grpData: GroupData_Detailed_Model){
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
    }


    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}