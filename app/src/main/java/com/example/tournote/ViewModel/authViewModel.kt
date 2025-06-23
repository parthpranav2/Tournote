package com.example.tournote.ViewModel

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Activity.LogInActivity
import com.example.tournote.Repository.authRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class authViewModel:ViewModel() {

    val repo = authRepository()
    val loginError = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()


    private val _roleLoading = MutableLiveData<Boolean>(false)
    val roleLoading : LiveData<Boolean> get() = _roleLoading

    private val _toastmsg = MutableLiveData<String?>(null)
    val toastmsg : LiveData<String?> get() = _toastmsg

    private val _navigateToLogin = MutableLiveData<Boolean>(false)
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private val _navigateToMain = MutableLiveData<Boolean>(false)
    val navigateToMain: LiveData<Boolean> = _navigateToMain


    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account = task.result
            if (account != null) {
                signInWithGoogle(account)
            } else {
                loginError.value = "Account is null"
            }
        } else {
            loginError.value = task.exception?.message
        }
    }

    private fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.firebaseLoginWithGoogle(account)
            if (result != null) {
                user_dataTO_firebase(result.uid, result.displayName ?: "", result.email ?: "", " ")
            } else {
                loginError.value = "Login failed."
            }
        }
    }
     fun cus_login(email:String,pass: String){
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.custom_login(email, pass)
            if (result.isSuccessful){
                _toastmsg.value = "Login Successful"
                isLoading.value= false
                _navigateToMain.value = true
            }else{
                _toastmsg.value = result.exception?.message ?: "Login failed"
                isLoading.value = false
            }
        }
    }

    fun cus_signup (email:String,pass: String,name: String, phone: String){
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.custom_signUp(email, pass)
            if (result.isSuccessful) {
                user_dataTO_firebase(repo.getuser() ?: "", name, email, phone)
            } else {
                isLoading.value = false
                _toastmsg.value = result.exception?.message ?: "Sign Up failed"
            }
        }

    }

    fun user_dataTO_firebase(userId: String, name: String, email: String, phone: String) {
        viewModelScope.launch {
            try {
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "createdAt" to System.currentTimeMillis()
                )
                val result = repo.userDetailsToFirestore(userId, userMap)
                if (result.isSuccess) {
                    isLoading.value = false
                    _toastmsg.value = "sign up successful"
                    _navigateToLogin.value = true
                } else {
                    _toastmsg.value = "Error saving user data: ${result.exceptionOrNull()?.message}"
                    isLoading.value = false

                }

            } catch (e: Exception) {
                isLoading.value = false
                _toastmsg.value = "Error: ${e.message}"
                Log.e("Firebase", "Error in user_dataTO_firebase", e)
            }
        }
    }

    fun forgot (email:String) {
        viewModelScope.launch {
            val result = repo.forgot_pass(email)
            if (result.isSuccessful) {
                _toastmsg.value = "Password reset email sent"
                _navigateToLogin.value= true
            } else {
                _toastmsg.value = result.exception?.message ?: "Failed to send password reset email"
                _navigateToLogin.value = false
            }
        }
    }

    fun clearToast() {
        _toastmsg.value = null
    }

    fun clearNavigationLogin() {
        _navigateToLogin.value = false
    }

    fun clearRoleLoadingMain() {
        _navigateToMain.value = false
    }




}