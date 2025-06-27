package com.example.tournote.Onboarding.Repository

import android.util.Log
import com.example.tournote.GlobalClass
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class authRepository {
    val firebaseAuth = FirebaseAuth.getInstance()
    val db = Firebase.firestore

    suspend fun custom_login(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email,pass).await()
            GlobalClass.Email=email
            Log.d("authRepository", "Signup success: ${result.user?.email}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Signup failed", e)
            Result.failure(e)
        }
    }

    suspend fun custom_signUp(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, pass)
                .await()
            GlobalClass.Email=email
            Log.d("authRepository", "Signup success: ${result.user?.email}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Signup failed", e)
            Result.failure(e)
        }
    }

    suspend fun forgot_pass(email: String): Task<Void?> {
        return firebaseAuth.sendPasswordResetEmail(email)
    }

    suspend fun signOut(): Result<String> {
        return try {
            firebaseAuth.signOut()
            Result.success("Signed out successfully")
        } catch (e: Exception) {
            Result.failure(e)

        }
    }

    fun getuser(): String? {
        return firebaseAuth.currentUser?.email
    }

    fun getUid():String?{
        return firebaseAuth.currentUser?.uid
    }

    suspend fun firebaseLoginWithGoogle(account: GoogleSignInAccount): FirebaseUser? {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = withContext(Dispatchers.IO) {
                firebaseAuth.signInWithCredential(credential).await()
            }

            result.user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun userDetailsToFirestore(userId: String, userMap: Map<String, Any>):Result<Any> {
        return try {
            val result= db.collection("users").document(userId).set(userMap).await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Error saving user details: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun changePassword(oldPass: String, newPass: String): Result<Any> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential).await()
                user.updatePassword(newPass).await()
                Result.success("Password changed successfully")
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    }
}