package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.R
import com.example.tournote.databinding.ActivityGettingStartedBinding


class GettingStartedActivity : AppCompatActivity() {
    private lateinit var binding : ActivityGettingStartedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_getting_started)

        binding = ActivityGettingStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGettingStrated.setOnClickListener{
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}