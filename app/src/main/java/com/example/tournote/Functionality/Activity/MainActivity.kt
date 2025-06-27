package com.example.tournote.Functionality.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.tournote.Functionality.FunctionalityPagerAdapter
import com.example.tournote.GroupSelector.Activity.GroupSelectorActivity
import com.example.tournote.GroupSelector.Adapter.GroupSelectorActivityPagerAdapter
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGroupSelectorBinding
import com.example.tournote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val groupId = intent.getStringExtra("GROUP_ID")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPager=binding.viewPager
        viewPager.adapter= FunctionalityPagerAdapter(this)
        viewPager.currentItem=0

        binding.viewPager.setPageTransformer(null)
        binding.viewPager.isUserInputEnabled=false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Triggered when a new page becomes selected
                when(position){
                    0->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplanneractive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)


                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }
                    1->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)


                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }
                    2->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expenseactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)


                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }
                    3->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsnotactive)


                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }
                    4->{
                        binding.imgSmartRoute.setImageResource(R.drawable.smartplannernotactive)
                        binding.imgChats.setImageResource(R.drawable.chatsnotactive)
                        binding.imgExpenses.setImageResource(R.drawable.expensenotactive)
                        binding.imgMemories.setImageResource(R.drawable.memoriesnotactive)
                        binding.imgTrackGroupMates.setImageResource(R.drawable.trackfriendsactive)


                        binding.txtSmartRoute.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtChats.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtExpenses.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtMemories.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        binding.txtTrackGroupMates.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkBluetext))
                    }
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                // Triggered while scrolling (optional)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // SCROLL_STATE_IDLE = 0, DRAGGING = 1, SETTLING = 2
            }
        })

        binding.btnSmartRoute.setOnClickListener {
            viewPager.setCurrentItem(0, false)
        }
        binding.btnMemories.setOnClickListener {
            viewPager.setCurrentItem(1, false)
        }
        binding.btnExpenses.setOnClickListener {
            viewPager.setCurrentItem(2, false)
        }
        binding.btnChats.setOnClickListener {
            viewPager.setCurrentItem(3, false)
        }
        binding.btnTrackGroupMates.setOnClickListener {
            viewPager.setCurrentItem(4, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed(){
        super.onBackPressed()
        redirectToActivity(GroupSelectorActivity::class.java)
    }
}