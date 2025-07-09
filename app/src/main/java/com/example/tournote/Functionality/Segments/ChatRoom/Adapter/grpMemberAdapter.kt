package com.example.tournote.Functionality.Segments.ChatRoom.Adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.GroupMemberData
import com.example.tournote.Functionality.Segments.ChatRoom.activityProfileInfo
import com.example.tournote.R
import com.example.tournote.UserModel

class grpMemberAdapter(val grpList: MutableList<UserModel>, val context: android.content.Context) : RecyclerView.Adapter<grpMemberAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_members, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = grpList[position]
        holder.memberName.text = user.name

        Glide.with(context)
            .load(user.profilePic)
            .placeholder(R.drawable.profile_photosample)
            .error(R.drawable.profile_photosample)
            .into(holder.memberPic)

        holder.itemView.setOnClickListener {
            Log.d("grpadapter", "Member clicked: ${user.name}")
            val intent = Intent(context, activityProfileInfo::class.java)
            intent.putExtra("user", user)
            context.startActivity(intent)
        }
    }


    fun updateList(newList: List<UserModel>) {
        grpList.clear()
        grpList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return grpList.size
    }

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView){
        val memberName = itemView.findViewById<TextView>(R.id.txtMemberName)
        val memberPic = itemView.findViewById<ImageView>(R.id.imgMemberProfile)
    }
}