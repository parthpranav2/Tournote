package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel
import kotlinx.coroutines.launch

class CustomSplashScreen : AppCompatActivity() {
    private val authViewModel : authViewModel by viewModels()
    private lateinit var groupViewModel: GroupSelectorActivityViewModel

    val repo = MainActivityRepository()

    private val minSplashDuration = 2000L // Minimum 2 seconds
    private val maxSplashDuration = 8000L // Maximum 8 seconds (fallback)
    private var splashStartTime = 0L
    private var dataFetched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        splashStartTime = System.currentTimeMillis()

        if (authViewModel.repo.getuser() != null) {
            val email = authViewModel.repo.getuser()
            if (email != null) {
                lifecycleScope.launch {
                    val result = repo.getUserByMailId(email)
                    result.onSuccess { user ->
                        GlobalClass.Me = user
                        initializeGroupViewModel()
                    }.onFailure {
                        // Handle failure (log or fallback)
                        initializeGroupViewModel() // still continue even if user load failed
                    }
                }
            } else {
                redirectToActivityWithDelay(GettingStartedActivity::class.java, minSplashDuration)
            }
        } else {
            redirectToActivityWithDelay(GettingStartedActivity::class.java, minSplashDuration)
        }

    }

    private fun initializeGroupViewModel() {
        groupViewModel = ViewModelProvider(this)[GroupSelectorActivityViewModel::class.java]

        // Set up observers for group data
        observeGroupData()

        // Set up fallback timer
        setupFallbackTimer()

        // Start fetching group data
        groupViewModel.fetchGroupDetails()
    }

    private fun observeGroupData() {
        // Observe groups data
        groupViewModel.groups.observe(this) { groups ->
            // Data is fetched (even if empty)
            dataFetched = true
            checkAndRedirect()
        }

        // Observe error state
        groupViewModel.error.observe(this) { error ->
            error?.let {
                // Even if there's an error, we should proceed
                dataFetched = true
                checkAndRedirect()
            }
        }
    }

    private fun setupFallbackTimer() {
        // Fallback timer to prevent indefinite waiting
        Handler(mainLooper).postDelayed({
            if (!dataFetched) {
                dataFetched = true
                checkAndRedirect()
            }
        }, maxSplashDuration)
    }

    private fun checkAndRedirect() {
        if (!dataFetched) return

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - splashStartTime

        if (elapsedTime >= minSplashDuration) {
            // Minimum duration has passed, redirect immediately
            redirectToActivity(GroupSelectorActivity::class.java)
        } else {
            // Wait for remaining minimum duration
            val remainingTime = minSplashDuration - elapsedTime
            Handler(mainLooper).postDelayed({
                redirectToActivity(GroupSelectorActivity::class.java)
            }, remainingTime)
        }
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun redirectToActivityWithDelay(activityClass: Class<*>, delay: Long) {
        Handler(mainLooper).postDelayed({
            redirectToActivity(activityClass)
        }, delay)
    }
}