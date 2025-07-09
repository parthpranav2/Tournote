package com.example.tournote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserModel(
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null,
    val phoneNumber: String? = null,
    val profilePic: String? = "null"
):Parcelable
