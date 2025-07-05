package com.example.tournote.Functionality

import com.example.tournote.Functionality.data.ChatMessage
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Api_Interface {

    @GET("message/{groupId}")
    suspend fun getAllMessages(
        @Path("groupId") groupId: String
    ): Response<List<ChatMessage>>



}