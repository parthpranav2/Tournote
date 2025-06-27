package com.example.tournote.Onboarding.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
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
import com.example.tournote.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val viewModel: authViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var isPasswordVisible = false
    private var isReEnterPasswordVisible = false

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            }
        }

    private val ImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                uri?.let {
                    selectedImageUri = it
                    binding.imgProfilePhoto.setImageURI(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

            binding.btnreenterpasswordvisibility.setImageResource(
                if (isReEnterPasswordVisible) R.drawable.closeeye else R.drawable.openeye
            )
            binding.txtReEnterPass.inputType = if (isReEnterPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.txtReEnterPass.typeface = currentTypeface
            binding.txtReEnterPass.setSelection(binding.txtReEnterPass.text?.length ?: 0)
        }

        binding.btnpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtPass.typeface
            isPasswordVisible = !isPasswordVisible

            binding.btnpasswordvisibility.setImageResource(
                if (isPasswordVisible) R.drawable.closeeye else R.drawable.openeye
            )
            binding.txtPass.inputType = if (isPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.txtPass.typeface = currentTypeface
            binding.txtPass.setSelection(binding.txtPass.text?.length ?: 0)
        }

        binding.txtLogIn.setOnClickListener {
            redirectToActivity(LogInActivity::class.java)
        }

        val spinner2 = findViewById<Spinner>(R.id.cmbcountrycode)
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, countryCodes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val codeOnly = getItem(position)?.split(" →")?.get(0) ?: ""
                (view as TextView).text = codeOnly
                return view
            }
        }
        spinner2.adapter = adapter

        binding.imgProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"
            }
            ImagePickerLauncher.launch(intent)
        }

        binding.btnGoogle.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            googleSignInClient.signOut()
            val intent = googleSignInClient.signInIntent
            launcher.launch(intent)
        }

        binding.btnSignUp.setOnClickListener {
            val mail = binding.txtEmail.text.toString()
            val name = binding.txtName.text.toString()
            val phone = binding.txtPhone.text.toString()
            val pass = binding.txtPass.text.toString()
            val cnf_pass = binding.txtReEnterPass.text.toString()

            if (validateInputs(mail, pass, cnf_pass, name, phone)) {
                if (selectedImageUri != null) {
                    viewModel.uploadImageToCloudinary(selectedImageUri!!, this)
                    viewModel.imageUrl.observe(this) { url ->
                        if (!url.isNullOrBlank()) {
                            viewModel.cus_signup(mail, pass, name, phone,url)
                        }
                    }
                } else {
                    viewModel.cus_signup(mail, pass, name, phone,"null")
                }
            }

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateInputs(email: String, password: String, cnfPass: String, name: String, phone: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            binding.txtEmail.error = "Email is required"
            isValid = false
        }
        if (name.isEmpty()) {
            binding.txtName.error = "Name is required"
            isValid = false
        }
        if (phone.isEmpty()) {
            binding.txtPhone.error = "Phone is required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.txtPass.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.txtPass.error = "Password should be at least 6 characters"
            isValid = false
        } else if (cnfPass.length < 6) {
            binding.txtReEnterPass.error = "Password should be at least 6 characters"
            isValid = false
        } else if (cnfPass != password) {
            Toast.makeText(this, "Both password should be matched.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        return isValid
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

    private fun observeModel() {
        viewModel.loginError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.isLoading.observe(this) { loading ->
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
        viewModel.navigateToMain.observe(this) {
            if (it) {
                val intent = Intent(this, GroupSelectorActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                viewModel.clearRoleLoadingMain()
            }
        }
    }
}
