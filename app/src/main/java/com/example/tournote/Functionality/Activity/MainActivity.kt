package com.example.tournote.Functionality.Activity

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tournote.Functionality.Adapter.FunctionalityPagerAdapter
import com.example.tournote.Functionality.MenuActionHandler
import com.example.tournote.Functionality.SocketManager
import com.example.tournote.Functionality.ViewModel.ChatViewModel
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.GroupSelector.Activity.GroupSelectorActivity
import com.example.tournote.GroupSelector.Adapter.GroupSelectorActivityPagerAdapter
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGroupSelectorBinding
import com.example.tournote.databinding.ActivityMainBinding
import com.google.firebase.logger.Logger

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    lateinit var moreOptions: ImageButton
    private val viewModel: MainActivityViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val isChatsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        val groupId = intent.getStringExtra("GROUP_ID")
        GlobalClass.group_id = groupId
        viewModel.loadGroup(groupId.toString())

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val group_logo = binding.grpLogo
        val group_name = binding.grpName
        moreOptions = findViewById(R.id.more_options)

        moreOptions.setOnClickListener {
            showPopupMenu(it)
        }

        viewModel.groupInfo.observe(this){
            it.onSuccess {
                group_name.text = it.name ?: "Unknown Group"
                Log.d("grpname", "grpname: ${it.name}")
                if (it.profilePic == "null" || it.profilePic.isNullOrBlank()) {
                    group_logo.setImageResource(R.drawable.defaultgroupimage)
                } else {
                    // Load the image using Glide or any other image loading library
                    com.bumptech.glide.Glide.with(this)
                        .load(it.profilePic)
                        .placeholder(R.drawable.defaultgroupimage)
                        .error(R.drawable.defaultgroupimage)
                        .into(group_logo)
                }
            }
            it.onFailure {
                group_name.text = "Error loading group name"
                group_logo.setImageResource(R.drawable.defaultgroupimage)
            }
        }

        viewPager=binding.viewPager
        viewPager.adapter= FunctionalityPagerAdapter(this)
        viewPager.currentItem=0

        handleKeyboardVisibility()

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

                        binding.bottomButtons.visibility = View.GONE

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

    private fun handleKeyboardVisibility() {
        val rootView = findViewById<View>(R.id.main)
        val bottomNav = findViewById<View>(R.id.bottomButtons)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open
                bottomNav.visibility = View.GONE
            } else {
                // Keyboard is closed
                if(viewPager.currentItem!=3){
                    bottomNav.visibility = View.VISIBLE
                }else{
                    bottomNav.visibility = View.GONE
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(viewPager.currentItem==3){
            viewPager.currentItem=0
        }
        else{
            redirectToActivity(GroupSelectorActivity::class.java)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("ChatDebug","socket disconnected")
        SocketManager.disconnect()
    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            val currentFragment = getCurrentFragment()
            if (currentFragment is MenuActionHandler) {
                currentFragment.onMenuActionSelected(item.itemId)
                true
            } else {
                false
            }
        }

        popup.show()
    }

    fun getCurrentFragment(): Fragment? {
        val fragmentTag = "f" + binding.viewPager.currentItem
        return supportFragmentManager.findFragmentByTag(fragmentTag)
    }



}