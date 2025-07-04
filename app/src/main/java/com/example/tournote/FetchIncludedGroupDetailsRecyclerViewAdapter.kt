package com.example.tournote

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
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel

class FetchIncludedGroupDetailsRecyclerViewAdapter(
    private val context: Context,
    private val groupList: List<GroupInfoModel>,
    private val viewModel: GroupSelectorActivityViewModel
) : RecyclerView.Adapter<FetchIncludedGroupDetailsRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val clickable : ConstraintLayout = itemView.findViewById(R.id.itemBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.inclusivegroup_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = groupList[position]
        holder.name.text = user.name ?: "Unknown"

        if (user.profilePic == "null" || user.profilePic.isNullOrBlank()) {
            holder.profilePhoto.setImageResource(R.drawable.defaultgroupimage)
        } else {
            Glide.with(context)
                .load(user.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(holder.profilePhoto)
        }

        holder.clickable.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("GROUP_ID", user.groupid ?: "")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = groupList.size

}