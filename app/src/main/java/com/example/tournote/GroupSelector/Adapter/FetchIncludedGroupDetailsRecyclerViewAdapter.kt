package com.example.tournote.GroupSelector.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Activity.MainActivity
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.GroupSelector.DataClass.GroupInfoModel
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FetchIncludedGroupDetailsRecyclerViewAdapter(
    private val context: Context,
    private val viewModel: GroupSelectorActivityViewModel,
    private val coroutineScope: CoroutineScope  // 🔥 add this
) : RecyclerView.Adapter<FetchIncludedGroupDetailsRecyclerViewAdapter.ViewHolder>() {

    private var groupList: List<GroupInfoModel> = emptyList()
    val repo2 = MainActivityRepository()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val clickable: ConstraintLayout = itemView.findViewById(R.id.itemBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.inclusivegroup_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groupList[position]
        holder.name.text = group.name ?: "Unknown Group"

        // Handle profile picture loading
        if (group.profilePic == "null" || group.profilePic.isNullOrBlank()) {
            holder.profilePhoto.setImageResource(R.drawable.defaultgroupimage)
        } else {
            Glide.with(context)
                .load(group.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(holder.profilePhoto)
        }

        holder.clickable.setOnClickListener {
            coroutineScope.launch {
                val result = repo2.groupData(group.groupid ?: "")
                result.onSuccess { groupData ->
                    GlobalClass.GroupDetails_Everything = groupData
                    val intent = Intent(context, MainActivity::class.java)
                    //intent.putExtra("GROUP_ID", group.groupid ?: "")
                    context.startActivity(intent)
                }.onFailure {
                    // Optional: show error
                }
            }
        }

    }

    override fun getItemCount(): Int = groupList.size

    // Function to update the adapter data
    fun updateGroupList(newGroupList: List<GroupInfoModel>) {
        groupList = newGroupList
        notifyDataSetChanged()
    }
}