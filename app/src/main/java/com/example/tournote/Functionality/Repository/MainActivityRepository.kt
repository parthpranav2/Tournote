package com.example.tournote.Functionality.Repository

import android.util.Log
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.UserModel
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

class MainActivityRepository {

    val db = Firebase.database

    suspend fun groupData(groupId: String): Result<GroupData_Detailed_Model> {

        return try {
            val groupRef = db.getReference("groups").child(groupId)

            // 1. Fetch GroupDetails
            val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
            val groupDetails = groupDetailsSnap.value as? Map<*, *> ?: return Result.failure(Exception("Invalid GroupDetails"))

            val name = groupDetails["name"] as? String
            val isGroupValid = groupDetails["isGroupValid"] as? Boolean
            val description = groupDetails["description"] as? String
            val profilePic = groupDetails["profilePic"] as? String
            val ownerID = groupDetails["owner"] as? String
            val createdAt = groupDetails["createdAt"]?.toString()
            val groupID = groupDetails["groupId"] as? String ?: groupId

            // 2. Get raw member/admin email keys (unsanitized, with `,`)
            val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
            val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }

            // 3. Sanitize keys
            val memberEmails = rawMemberKeys.map { it.replace(",", ".") }
            val adminEmails = rawAdminKeys.map { it.replace(",", ".") }
            val ownerEmail = ownerID?.replace(",",".")

            // 4. Fetch all users and match by email
            val usersSnap = db.getReference("users").get().await()
            val membersList = mutableListOf<UserModel>()
            val adminsList = mutableListOf<UserModel>()
            var owner : UserModel?=null

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email != null) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )

                    if (email in memberEmails) membersList.add(user)
                    if (email in adminEmails) adminsList.add(user)
                    if (email == ownerEmail) owner=user
                }
            }

            val group = GroupData_Detailed_Model(
                groupID = groupID,
                name = name,
                description = description,
                profilePic = profilePic,
                isGroupValid = isGroupValid,
                owner = owner!!,
                createdAt = createdAt,
                members = membersList,
                admins = adminsList
            )

            // ✅ Log all group details
            //logGroupInfo(group)

            Result.success(group)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getUserByMailId(emailId: String): Result<UserModel> {
        return try {
            val usersSnap = db.getReference("users").get().await()

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email == emailId) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )
                    return Result.success(user)
                }
            }

            Result.failure(Exception("User with email $emailId not found"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /*private fun logGroupInfo(group: GroupData_Detailed_Model) {
        Log.d("GroupLog", "======= Group Info =======")
        Log.d("GroupLog", "Group ID: ${group.groupID}")
        Log.d("GroupLog", "Name: ${group.name}")
        Log.d("GroupLog", "Description: ${group.description}")
        Log.d("GroupLog", "Created At: ${group.createdAt}")
        Log.d("GroupLog", "Profile Pic: ${group.profilePic}")

        Log.d("GroupLog", "----- Owner -----")
        Log.d("GroupLog", "• ${group.owner.name} | ${group.owner.email} | ${group.owner.phoneNumber} | ${group.owner.profilePic}")

        Log.d("GroupLog", "----- Members -----")
        group.members.forEach {
            Log.d("GroupLog", "• ${it.name} | ${it.email} | ${it.phoneNumber} | ${it.profilePic}")
        }

        Log.d("GroupLog", "----- Admins -----")
        group.admins.forEach {
            Log.d("GroupLog", "• ${it.name} | ${it.email} | ${it.phoneNumber} | ${it.profilePic}")
        }

        Log.d("GroupLog", "==========================")
    }*/


}