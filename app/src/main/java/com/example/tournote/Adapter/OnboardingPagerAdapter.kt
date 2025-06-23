package com.example.tournote.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tournote.Fragment.Onboarding1
import com.example.tournote.Fragment.Onboarding2
import com.example.tournote.Fragment.Onboarding3
import com.example.tournote.Fragment.Onboarding4

class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> Onboarding1()
            1 -> Onboarding2()
            2 -> Onboarding3()
            3 -> Onboarding4()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}