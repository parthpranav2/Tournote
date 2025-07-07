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
import com.example.tournote.GlobalClass
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.UserModel

class AddUsers_CreateGroupRecyclerViewAdapter(
    private val context: Context,
    private val fullUserList: List<UserModel>,
    private val viewModel: GroupSelectorActivityViewModel
) : RecyclerView.Adapter<AddUsers_CreateGroupRecyclerViewAdapter.ViewHolder>() {

    // Filter out the current user from the full list
    private val filteredFullUserList: List<UserModel> = fullUserList.filter { user ->
        user.email != GlobalClass.Email
    }

    private var filteredUserList: List<UserModel> = filteredFullUserList.toList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val clickable: ConstraintLayout = itemView.findViewById(R.id.itemBody)
        val tick: ImageView = itemView.findViewById(R.id.imgtick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.searchmembers_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUserList[position]
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

        // Initial state
        holder.tick.setImageResource(R.drawable.untick)
        holder.tick.tag = R.drawable.untick

        holder.clickable.setOnClickListener {
            if (holder.tick.tag == R.drawable.untick) {
                holder.tick.setImageResource(R.drawable.tick)
                holder.tick.tag = R.drawable.tick
                viewModel.addUserToGrp(user) // ✅ Add user
            } else {
                holder.tick.setImageResource(R.drawable.untick)
                holder.tick.tag = R.drawable.untick
                viewModel.removeUserFromGrp(user) // ✅ Remove user
            }
        }
    }

    override fun getItemCount(): Int = filteredUserList.size

    // 🔍 Call this function to filter the list
    fun filter(query: String) {
        filteredUserList = if (query.isBlank()) {
            filteredFullUserList
        } else {
            filteredFullUserList.filter {
                it.name?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }
}