package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tournote.GroupSelector.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.databinding.ActivityLogInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


class LogInActivity : AppCompatActivity() {
    private var isPasswordVisible = false

    private lateinit var binding: ActivityLogInBinding


    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.e("authViewModel", "Google Sign In Result Received")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            }
        }

    private val viewModel: authViewModel by viewModels()
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

        observeModel()

        binding.btnsignin.setOnClickListener {
            val mail = binding.txtEmail.text.toString()
            val pass = binding.txtPass.text.toString()
            if (validateInputs(mail,pass)){
                viewModel.cus_login(mail,pass)
            }
        }

        binding.btnGoogle.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            googleSignInClient.signOut()
            Log.e("authViewModel", "Google Sign In Button Clicked")
            val intent = googleSignInClient.signInIntent
            launcher.launch(intent)
        }


    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            binding.progressBar.visibility = View.GONE
            isValid = false
        }

        if (password.isEmpty()) {
            binding.txtPass.error = "Password is required"
            binding.progressBar.visibility = View.GONE
            isValid = false
        } else if (password.length < 6) {
            binding.txtPass.error = "Password should be at least 6 characters"
            isValid = false
            binding.progressBar.visibility = View.GONE
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
            binding.progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
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
                val intent = Intent(this, GroupSelectorActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                viewModel.clearRoleLoadingMain()
            }
        }
    }

}