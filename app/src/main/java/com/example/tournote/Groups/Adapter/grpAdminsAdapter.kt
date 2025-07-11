package com.example.tournote.Groups.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Groups.Activity.activityProfileInfo
import com.example.tournote.R
import com.example.tournote.UserModel

class grpAdminsAdapter(val adminList: MutableList<UserModel>, val context: Context) : RecyclerView.Adapter<grpAdminsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_members, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val grp = adminList[position]
        holder.memberName.text = grp.name
        holder.adminBadge.visibility =  View.VISIBLE
        holder.itemView.setOnClickListener {
            setOnItemClickListener(grp)
        }
        Glide.with(context)
            .load(grp.profilePic)
            .placeholder(R.drawable.profile_photosample)
            .error(R.drawable.profile_photosample)
            .into(holder.memberPic)
    }

    fun updateList(newList: List<UserModel>) {
        adminList.clear()
        adminList.addAll(newList)
        notifyDataSetChanged()
    }
    fun setOnItemClickListener(data: UserModel) {
        val intent = Intent(context, activityProfileInfo::class.java)
        intent.putExtra("user", data)
        startActivity(context, intent, null)
    }

    override fun getItemCount(): Int {
        return adminList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val memberName = itemView.findViewById<TextView>(R.id.txtMemberName)
        val memberPic = itemView.findViewById<ImageView>(R.id.imgMemberProfile)
        val adminBadge = itemView.findViewById<TextView>(R.id.txtAdminBadge)
    }
}