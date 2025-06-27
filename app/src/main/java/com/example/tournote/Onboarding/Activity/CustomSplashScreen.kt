package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.GlobalClass
import com.example.tournote.GroupSelector.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel

class CustomSplashScreen : AppCompatActivity() {
    private val viewModel : authViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (viewModel.repo.getuser()!=null){
            GlobalClass.Email=viewModel.repo.getuser()
            redirectToActivity(GroupSelectorActivity::class.java)
        } else {
            // If user is not logged in, redirect to GettingStartedActivity
            redirectToActivity(GettingStartedActivity::class.java)
        }

    }


    private fun redirectToActivity(activityClass: Class<*>) {
        Handler(mainLooper).postDelayed({
            val intent = Intent(this, activityClass)
            startActivity(intent)
            finish() // Close MainActivity
        }, 2000)
    }



}