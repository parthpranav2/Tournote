package com.example.tournote.Functionality.Segments.ChatRoom.Repository

import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.groupData
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.userData
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

class firebaseChatRepository {

    val realDb = Firebase.database

    suspend fun groupData(groupId: String): Result<groupData> {
        return try {
            val document = realDb
                .getReference("groups")
                .child(groupId)
                .child("GroupDetails")
                .get()
                .await()

            Result.success(document.getValue(groupData::class.java) ?: groupData())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun groupMemebersData(groupId: String): Result<MutableList<String>> {
        return try {
            val document = realDb
                .getReference("groups")
                .child(groupId)
                .child("Members")
                .get()
                .await()

            val emails = mutableListOf<String>()
            for (child in document.children) {
                child.key?.let {
                    emails.add(it.replace(",", "."))
                }
            }

            Result.success(emails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun userData(userId: String): Result<userData> {
        return try {
            val document = realDb
                .getReference("users")
                .child(userId)
                .child("PersonalDetails")
                .get()
                .await()

            Result.success(document.getValue(userData::class.java)?: userData())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun userGroupData(userId: String): Result<MutableList<String>> {
        return try {
            val document = realDb
                .getReference("users")
                .child(userId)
                .child("Groups")
                .get()
                .await()

            val groups = mutableListOf<String>()
            for (child in document.children) {
                child.key?.let {
                    groups.add(it)
                }
            }

            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}