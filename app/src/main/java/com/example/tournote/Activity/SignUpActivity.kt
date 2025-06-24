package com.example.tournote.Activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.R
import com.example.tournote.ViewModel.authViewModel
import com.example.tournote.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isReEnterPasswordVisible = false


    private lateinit var binding: ActivitySignUpBinding

    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            }
        }

    private val viewModel: authViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        observeModel()

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

        binding.btnSignUp.setOnClickListener {
            val mail = binding.txtEmail.text.toString()
            val name = binding.txtName.text.toString()
            val phone = binding.txtPhone.text.toString()
            val pass = binding.txtPass.text.toString()
            val cnf_pass = binding.txtReEnterPass.text.toString()
            if (validateInputs(mail,pass,cnf_pass,name,phone)){
                viewModel.cus_signup(mail,pass,name,phone)
            }

        }

        binding.btnGoogle.setOnClickListener {
            binding.bar.visibility = View.VISIBLE
            googleSignInClient.signOut()
            val intent = googleSignInClient.signInIntent
            launcher.launch(intent)
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

    private fun validateInputs(email: String, password: String,cnfPass: String,name:String,phone:String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            binding.bar.visibility = View.GONE
            isValid = false
        }
        if (name.isEmpty()) {
            binding.txtName.error = "Name is required"
            binding.bar.visibility = View.GONE
            isValid = false
        }
        if (phone.isEmpty()) {
            binding.txtPhone.error = "Phone is required"
            binding.bar.visibility = View.GONE
            isValid = false
        }

        if (password.isEmpty()) {
            binding.txtPass.error = "Password is required"
            binding.bar.visibility = View.GONE
            isValid = false
        } else if (password.length < 6) {
            binding.txtPass.error = "Password should be at least 6 characters"
            isValid = false
            binding.bar.visibility = View.GONE
        }else if (cnfPass.length < 6) {
            binding.txtReEnterPass.error = "Password should be at least 6 characters"
            isValid = false
            binding.bar.visibility = View.GONE
        }else if (cnfPass != password) {
            Toast.makeText(this, "Both password should be matched.", Toast.LENGTH_SHORT).show()
            isValid = false
            binding.bar.visibility = View.GONE
        }

        return isValid
    }

    private fun observeModel(){

        viewModel.loginError.observe(this)
        { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this)
        { loading ->
            // Show/hide progress bar based on `loading`
            binding.bar.visibility = if (loading == true) View.VISIBLE else View.GONE
        }

        viewModel.toastmsg.observe(this) {
            it?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.navigateToLogin.observe(this) {
            if (it) {
                startActivity(Intent(this, LogInActivity::class.java))
                viewModel.clearNavigationLogin()
            }
        }

        viewModel.navigateToMain.observe(this){
            if (it) {
                Toast.makeText(this, "main activity will open", Toast.LENGTH_SHORT).show()
                viewModel.clearRoleLoadingMain()
            }
        }
    }


}