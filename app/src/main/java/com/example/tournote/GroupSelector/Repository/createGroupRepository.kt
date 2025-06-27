package com.example.tournote.GroupSelector.Repository

import com.example.tournote.GlobalClass
import com.example.tournote.GroupInfoModel
import com.example.tournote.Onboarding.Repository.authRepository
import com.example.tournote.UserModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class createGroupRepository {
    private val db = Firebase.firestore
    private val repo = authRepository()

    suspend fun fetchDetailsOfAllUsers(): Result<List<UserModel>> {
        return try {
            val snapshot = db.collection("users").get().await()
            val userList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserModel::class.java)
            }
            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDetailsOfGroups(): Result<List<GroupInfoModel>> {
        val uid = repo.getUid()
        return uid?.let {
            try {
                val userSnapshot = db.collection("users").document(it).get().await()
                val groupIds = userSnapshot.get("groups") as? List<String> ?: emptyList()

                val groupList = mutableListOf<GroupInfoModel>()

                for (groupId in groupIds) {
                    val groupSnapshot = db.collection("groups").document(groupId).get().await()
                    if (groupSnapshot.exists()) {
                        val groupData = groupSnapshot.data
                        val name = groupData?.get("name") as? String ?: "Unnamed Group"
                        val profilePic = groupData?.get("profilePic") as? String ?: "null"

                        groupList.add(
                            GroupInfoModel(groupid = groupId, name = name, profilePic = profilePic)
                        )
                    }
                }

                Result.success(groupList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(IllegalStateException("User UID is null"))

    }


    suspend fun registerGroup(
        name: String,
        description: String,
        members: List<UserModel>,
        groupProfileUrl: String
    ): Result<String> {
        return try {
            val memberEmails = if (members.isNotEmpty()) {
                members.mapNotNull { it.email }
            } else {
                listOf(GlobalClass.Email ?: "unknown@email.com")
            }

            // Create a new group document
            val docRef = db.collection("groups").document()
            val groupId = docRef.id

            // Save group info
            val groupData = hashMapOf(
                "groupId" to groupId,
                "name" to name,
                "description" to description,
                "members" to memberEmails,
                "profilePic" to groupProfileUrl,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(groupData).await()

            // ✅ Add groupId to each user based on email
            addGroupIdToUsersByEmail(memberEmails, groupId)

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun addGroupIdToUsersByEmail(emails: List<String>, groupId: String) {
        for (email in emails) {
            try {
                val snapshot = db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val userDocRef = snapshot.documents.first().reference
                    userDocRef.update("groups", FieldValue.arrayUnion(groupId))
                } else {
                    println("No user found with email: $email")
                }
            } catch (e: Exception) {
                println("Error updating user with email $email: ${e.message}")
            }
        }
    }
}
