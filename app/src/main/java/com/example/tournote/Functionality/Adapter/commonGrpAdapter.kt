package com.example.tournote.Functionality.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.GroupMemberData
import com.example.tournote.R

class commonGrpAdapter(val grpMemberList: List<GroupMemberData>,val context: Context) : RecyclerView.Adapter<commonGrpAdapter.GroupMemberViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GroupMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_common_groups, parent, false)
        return GroupMemberViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: GroupMemberViewHolder,
        position: Int
    ) {
        val grp = grpMemberList[position]
        holder.name.text = grp.name
        Glide.with(context)
            .load(grp.image)
            .placeholder(R.drawable.defaultgroupimage)
            .error(R.drawable.defaultgroupimage)
            .into(holder.image)
    }

    override fun getItemCount(): Int {
        return grpMemberList.size
    }

    class GroupMemberViewHolder(val grpMemberView: View) : RecyclerView.ViewHolder(grpMemberView){
        val name = grpMemberView.findViewById<TextView>(R.id.txtGroupName)
        val image = grpMemberView.findViewById<ImageView>(R.id.imgGroupProfile)
    }
}