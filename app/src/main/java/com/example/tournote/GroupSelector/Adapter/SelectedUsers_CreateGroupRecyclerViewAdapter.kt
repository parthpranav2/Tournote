package com.example.tournote.GroupSelector.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.manager.Lifecycle
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.UserModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class SelectedUsers_CreateGroupRecyclerViewAdapter(
    private val context: Context,
    private val fullUserList: List<UserModel>,
    private val viewModel: GroupSelectorActivityViewModel
) : RecyclerView.Adapter<SelectedUsers_CreateGroupRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val body: ConstraintLayout = itemView.findViewById(R.id.itemBody)
        val ownerTag: TextView = itemView.findViewById(R.id.txtOwner)
        val adminTag: TextView = itemView.findViewById(R.id.txtAdmin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.selectedmembers_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = fullUserList[position]

        // Set user name
        holder.name.text = user.name ?: "Unknown"

        // Owner tag should be visible from the start if it's "(You)"
        holder.ownerTag.visibility = if (user.name == "(You)") View.VISIBLE else View.INVISIBLE

        // Set Admin tag visibility based on ViewModel
        val isAdmin = viewModel.checkForPresence_AdminList(user)
        holder.adminTag.visibility = if (isAdmin) View.VISIBLE else View.GONE

        // Load profile photo
        if (user.profilePic.isNullOrBlank() || user.profilePic == "null") {
            holder.profilePhoto.setImageResource(R.drawable.profile_photosample)
        } else {
            Glide.with(context)
                .load(user.profilePic)
                .placeholder(R.drawable.profile_photosample)
                .error(R.drawable.profile_photosample)
                .into(holder.profilePhoto)
        }

        // Long click to show admin options
        holder.body.setOnLongClickListener {
            if(holder.name.text!="(You)"){
                showAdminBottomSheet(user, position)
            }
            true
        }
    }

    private fun showAdminBottomSheet(user: UserModel, position: Int) {
        val dialog = BottomSheetDialog(context).apply {
            setContentView(R.layout.bsfragment_admin)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val txtAdminAction = dialog.findViewById<TextView>(R.id.txtbtnAdmin)
        val btnAdmin = dialog.findViewById<RelativeLayout>(R.id.btnAdmin)
        val btnInfo = dialog.findViewById<RelativeLayout>(R.id.btnInfo)

        val isAdmin = viewModel.checkForPresence_AdminList(user)
        txtAdminAction?.text = if (isAdmin) "Remove from group admin" else "Make group admin"

        btnAdmin?.setOnClickListener {
            if (isAdmin) {
                viewModel.removeUserFromAdminList(user)
            } else {
                viewModel.addUserToAdminList(user)
            }

            notifyItemChanged(position) // ⬅️ This will rebind the item and update admin tag
            dialog.dismiss()
        }

        btnInfo?.setOnClickListener {
            // TODO: Navigate to user info
        }

        dialog.show()
    }

    override fun getItemCount(): Int = fullUserList.size
}
