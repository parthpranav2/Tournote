package com.example.tournote.Groups.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tournote.Groups.Fragment.CreateGroupFragment
import com.example.tournote.Groups.Fragment.HomeFragment
import com.example.tournote.Groups.Fragment.ProfileFragment

class GroupSelectorActivityPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1-> CreateGroupFragment()
            2 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}