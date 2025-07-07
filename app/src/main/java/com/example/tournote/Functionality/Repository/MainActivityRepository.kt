package com.example.tournote.Functionality.Repository

import androidx.lifecycle.LiveData
import com.example.tournote.GroupSelector.DataClass.GroupInfoModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class MainActivityRepository {

    val fireStore = Firebase.firestore

    suspend fun groupData(groupId: String): Result<GroupInfoModel> {
        return try {
            val document = fireStore
                .collection("groups")
                .document(groupId)
                .get()
                .await()

            val data = document.toObject(GroupInfoModel::class.java)
            Result.success(data ?: GroupInfoModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



}