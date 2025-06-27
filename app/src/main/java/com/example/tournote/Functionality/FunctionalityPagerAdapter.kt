package com.example.tournote.Functionality

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tournote.Functionality.Fragments.ChatsFragment
import com.example.tournote.Functionality.Fragments.ExpensesFragment
import com.example.tournote.Functionality.Fragments.MemoriesFragment
import com.example.tournote.Functionality.Fragments.SmartRoutePlannerFragment
import com.example.tournote.Functionality.Fragments.TrackFriendsFragment
import com.example.tournote.GroupSelector.Fragment.CreateGroupFragment
import com.example.tournote.GroupSelector.Fragment.HomeFragment
import com.example.tournote.GroupSelector.Fragment.ProfileFragment

class FunctionalityPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0-> SmartRoutePlannerFragment()
            1-> MemoriesFragment()
            2-> ExpensesFragment()
            3-> ChatsFragment()
            4-> TrackFriendsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}