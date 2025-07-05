package com.example.tournote.Functionality

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


private val BASE_URL = "https://tournote-chat-backend.onrender.com"

object APIClient {

    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val api_Interface = retrofit.create(Api_Interface::class.java)

}