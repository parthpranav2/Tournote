package com.example.serviceinandroid

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Virtualizer
import android.os.IBinder
import android.provider.Settings

class MyService: Service() {
    private lateinit var mediaPlayer: MediaPlayer
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            Actions.START.toString()->{
                startMyServices()
            }
            Actions.STOP.toString()->{
                mediaPlayer.stop()
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startMyServices() {
        mediaPlayer= MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI)
        mediaPlayer.start()
        mediaPlayer.isLooping=true

    }
}
enum class Actions{
    START,STOP
}