package com.example.tournote.GroupSelector.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.UserModel

class SelectedUsers_CreateGroupRecyclerViewAdapter(
    private val context: Context,
    private val fullUserList: List<UserModel> ,
    private val viewModel: GroupSelectorActivityViewModel
) : RecyclerView.Adapter<SelectedUsers_CreateGroupRecyclerViewAdapter.ViewHolder>() {


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        //val clickable : ConstraintLayout = itemView.findViewById(R.id.itemBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.selectedmembers_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = fullUserList[position]
        holder.name.text = user.name ?: "Unknown"

        if (user.profilePic == "null" || user.profilePic.isNullOrBlank()) {
            holder.profilePhoto.setImageResource(R.drawable.profile_photosample)
        } else {
            Glide.with(context)
                .load(user.profilePic)
                .placeholder(R.drawable.profile_photosample)
                .error(R.drawable.profile_photosample)
                .into(holder.profilePhoto)
        }


    }


    override fun getItemCount(): Int = fullUserList.size

}
