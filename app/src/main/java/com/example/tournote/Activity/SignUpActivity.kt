package com.example.tournote.Activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.R
import com.example.tournote.databinding.ActivitySignUpBinding


class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isReEnterPasswordVisible = false


    private lateinit var binding: ActivitySignUpBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnreenterpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtReEnterPass.typeface
            isReEnterPasswordVisible = !isReEnterPasswordVisible

            if (isReEnterPasswordVisible) {
                binding.btnreenterpasswordvisibility.setImageResource(R.drawable.closeeye)
                binding.txtReEnterPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.btnreenterpasswordvisibility.setImageResource(R.drawable.openeye)
                binding.txtReEnterPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.txtReEnterPass.typeface = currentTypeface

            binding.txtReEnterPass.setSelection(binding.txtReEnterPass.text?.length ?: 0)
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

        binding.txtLogIn.setOnClickListener {
            redirectToActivity(LogInActivity::class.java)
        }

        val spinner2 = findViewById<Spinner>(R.id.cmbcountrycode)
        val countryCodes = resources.getStringArray(R.array.country_codes)

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, countryCodes) {
            override fun getItem(position: Int): String {
                // When the spinner is clicked, we can display the full format
                return super.getItem(position) ?: ""
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Customize dropdown items if needed, but use the default
                return super.getDropDownView(position, convertView, parent)
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                // If the item is selected, show only the country code (e.g., +91)
                val selectedItem = countryCodes[position]
                val codeOnly = selectedItem.split(" →")[0] // Extract +91 or other code
                (view as TextView).text = codeOnly // Show just the country code in the spinner
                return view
            }
        }
        spinner2.adapter = adapter


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
}