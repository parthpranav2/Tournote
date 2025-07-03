package com.example.tournote.Onboarding.ViewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.Repository.authRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.io.File

class authViewModel: ViewModel() {

    val repo = authRepository()
    val loginError = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>()


    private val _toastmsg = MutableLiveData<String?>(null)
    val toastmsg : LiveData<String?> get() = _toastmsg

    private val _navigateToLogin = MutableLiveData<Boolean>(false)
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private val _navigateToMain = MutableLiveData<Boolean>(false)
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    private val _googleResponse = MutableLiveData<FirebaseUser?>(null)
    val googleResponse: LiveData<FirebaseUser?> get() = _googleResponse



    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            Log.e("error authViewModel", "Sign in successful")
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
                //GlobalClass.uid=result.uid
                GlobalClass.Email=result.email
                _googleResponse.value = result
            } else {
                _googleResponse.value = null
                loginError.value = "Login failed."
            }
        }
    }
     fun cus_login(email:String,pass: String){
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.custom_login(email, pass)
            if (result.isSuccess){
                GlobalClass.Email=email
                _toastmsg.value = "Login Successful"
                isLoading.value= false
                _navigateToMain.value = true
            }else{
                _toastmsg.value = result.exceptionOrNull()?.message ?: "Login failed"
                isLoading.value = false
            }
        }
    }

    fun cus_signup (email:String,pass: String,name: String, phone: String, profilePicUrl: String){
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.custom_signUp(email, pass)
            if (result.isSuccess) {
                user_dataTO_firebase(repo.getUid() ?: "", name, email, phone, profilePicUrl)
            } else {
                isLoading.value = false
                _toastmsg.value = result.exceptionOrNull()?.message ?: "Sign Up failed"
            }
        }

    }

    fun user_dataTO_firebase(userId: String, name: String, email: String, phone: String,profilePicUrl:String) {
        viewModelScope.launch {
            try {
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "profilePic" to profilePicUrl,
                    "createdAt" to System.currentTimeMillis()
                )
                val result = repo.userDetailsToFirestore(userId, userMap)
                if (result.isSuccess) {
                    isLoading.value = false
                    GlobalClass.Email=email
                    _toastmsg.value = "sign up successful"
                    _navigateToMain.value = true
                } else {
                    _toastmsg.value = "Error saving user data: ${result.exceptionOrNull()?.message}"
                    Log.d("Firebase", "Error saving user data: ${result.exceptionOrNull()?.message}")
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

    fun signOut() {
        viewModelScope.launch {
            val result = repo.signOut()
            if (result.isSuccess){
                _toastmsg.value = "Signed out successfully"
                _navigateToLogin.value= true
            } else {
                _toastmsg.value = result.exceptionOrNull()?.message ?: "Sign out failed"
            }
        }
    }


    private val _imageUrl = MutableLiveData<String?>()
    val imageUrl: LiveData<String?> get() = _imageUrl

    fun uploadImageToCloudinary(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val file = getFileFromUri(uri, context)

                val uploadOptions = hashMapOf<String, Any>(
                    "public_id" to "user_${System.currentTimeMillis()}",
                    "folder" to "android_uploads",
                    "resource_type" to "image"
                )

                MediaManager.get()
                    .upload(file.absolutePath)
                    .options(uploadOptions)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {
                            isLoading.postValue(true)
                        }

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                            // optional progress updates
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            val url = resultData?.get("secure_url") as? String
                            _imageUrl.postValue(url)
                            isLoading.postValue(false)
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            _toastmsg.postValue("Upload Failed: ${error?.description}")
                            isLoading.postValue(false)
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            // not needed here
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                _toastmsg.postValue("Exception: ${e.message}")
                isLoading.postValue(false)
            }
        }
    }

    private fun getFileFromUri(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload_", ".jpg", context.cacheDir)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }



}