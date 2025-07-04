package com.example.tournote.Functionality

import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

object SocketManager {

    private const val SOCKET_URL = "https://b659-103-178-126-232.ngrok-free.app"

    private lateinit var socket : Socket

    fun initSocket(){
        socket = IO.socket(SOCKET_URL)
    }

    fun connect() {
        if (!::socket.isInitialized) {
            initSocket()
        }
        if (!socket.connected()) {
            socket.connect()
        } else {
            Log.d("Socket", "Already connected 🚫")
        }
    }

    fun disconnect() {
        if (::socket.isInitialized) {
            socket.disconnect()
        }
    }

    fun on(event : String , listener: Emitter.Listener){
        if (::socket.isInitialized) socket.on(event, listener)
        else socket.connect()
    }

    fun emit(event: String, data:Any ,ack: Ack?=null) {
        if(::socket.isInitialized) {
            if (ack != null) socket.emit(event,data,ack)
            else socket.emit(event,data)
        }
    }



}