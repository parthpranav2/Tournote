package com.example.tournote.Functionality.Segments.ChatRoom.Interface

import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatMessage
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface Api_Interface {

    @GET("message/{groupId}")
    suspend fun getAllMessages(
        @Path("groupId") groupId: String
    ): Response<List<ChatMessage>>

}