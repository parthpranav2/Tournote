package com.example.tournote.Activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.R
import com.example.tournote.databinding.ActivityLogInBinding


class LogInActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    private lateinit var binding: ActivityLogInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)

        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtPass.typeface
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.btnpasswordvisibility.setImageResource(R.drawable.closeeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.btnpasswordvisibility.setImageResource(R.drawable.openeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.txtPass.typeface = currentTypeface

            binding.txtPass.setSelection(binding.txtPass.text?.length ?: 0)
        }

        binding.txtSignup.setOnClickListener {
            redirectToActivity(SignUpActivity::class.java)
        }

    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}