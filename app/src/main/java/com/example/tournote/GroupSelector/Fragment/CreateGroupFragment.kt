package com.example.tournote.GroupSelector.Fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.GroupSelector.Adapter.AddUsers_CreateGroupRecyclerViewAdapter
import com.example.tournote.GroupSelector.Adapter.SelectedUsers_CreateGroupRecyclerViewAdapter
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.UserModel
import com.example.tournote.databinding.FragmentCreateGroupBinding

class CreateGroupFragment : Fragment() {

    private lateinit var binding: FragmentCreateGroupBinding
    private val viewModel: GroupSelectorActivityViewModel by activityViewModels()
    private lateinit var adapter: AddUsers_CreateGroupRecyclerViewAdapter
    private lateinit var adapter2: SelectedUsers_CreateGroupRecyclerViewAdapter

    private var selectedImageUri: Uri? = null
    private var usersIn: List<UserModel> = listOf()

    private var isGroupCreationPending = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            viewModel.navToHomeSwitch()
        }

        binding.btnAddMembers.setOnClickListener {
            binding.btnAddMembers.visibility = View.GONE
            binding.relLayoutMemberSelector.visibility = View.VISIBLE
        }

        // Observe image upload result once
        viewModel.imageUrl.observe(viewLifecycleOwner) { url ->
            if (isGroupCreationPending && !url.isNullOrBlank()) {
                val groupName = binding.txtGroupName.text.toString()
                val groupDescription = binding.txtGroupDescription.text.toString()

                viewModel.createGroup(groupName, groupDescription, usersIn, url)
                isGroupCreationPending = false
            }
        }

        viewModel.resetUi.observe(viewLifecycleOwner){status->
            if(status){
                resetUI()
            }
        }

        binding.btnCreateGrp.setOnClickListener {
            viewModel.fetchGroupDetails()
            if (binding.txtGroupName.text.isNullOrEmpty()) {
                binding.txtGroupNameAlert.visibility = View.VISIBLE
            } else {
                val groupName = binding.txtGroupName.text.toString()
                val groupDescription = binding.txtGroupDescription.text.toString()

                if (selectedImageUri != null) {
                    isGroupCreationPending = true
                    viewModel.uploadImageToCloudinary(selectedImageUri!!, requireContext())
                } else {
                    viewModel.createGroup(groupName, groupDescription, usersIn, "null")
                }
            }
        }

        // Setup user lists
        binding.rvActiveUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSelectedUserList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        viewModel.fetchAllActiveUsers()

        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter = AddUsers_CreateGroupRecyclerViewAdapter(requireContext(), users, viewModel)
            binding.rvActiveUserList.adapter = adapter

            binding.txtSearch.addTextChangedListener { editable ->
                val query = editable?.toString() ?: ""
                adapter.filter(query)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner){status->
            if(status){
                binding.progressBar.visibility=View.VISIBLE
            }else{
                binding.progressBar.visibility=View.GONE
            }
        }
        viewModel.usersIn.observe(viewLifecycleOwner) { users ->
            usersIn = users
            adapter2 = SelectedUsers_CreateGroupRecyclerViewAdapter(requireContext(), users, viewModel)
            binding.rvSelectedUserList.adapter = adapter2

            binding.relLayoutSelectedMembers.visibility =
                if (users.size<=1) View.GONE else View.VISIBLE
        }

        binding.grpProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                uri?.let {
                    selectedImageUri = it
                    binding.grpProfileImage.setImageURI(it)
                }
            }
        }


    private fun resetUI() {
        // Clear input fields
        binding.txtGroupName.text?.clear()
        binding.txtGroupDescription.text?.clear()

        // Reset error visibility
        binding.txtGroupNameAlert.visibility = View.GONE

        // Reset selected image
        selectedImageUri = null
        binding.grpProfileImage.setImageResource(android.R.color.transparent)

        // Hide member selector layout and show "Add Members" button
        binding.relLayoutMemberSelector.visibility = View.GONE
        binding.btnAddMembers.visibility = View.VISIBLE

        // Reset selected users list in ViewModel
        viewModel.removeAllUsersFromGrp()

        // Optionally scroll list to top
        binding.rvActiveUserList.scrollToPosition(0)
        binding.rvSelectedUserList.scrollToPosition(0)

        viewModel.resetUiToggleSwitch()
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateGroupFragment().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}

