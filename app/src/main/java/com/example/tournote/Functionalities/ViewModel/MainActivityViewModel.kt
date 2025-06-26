package com.example.tournote.Functionalities.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    private val _navigateToHome = MutableLiveData<Boolean>(true)
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    fun navToHomeSwitch(){
        if(_navigateToHome.value==true){
            _navigateToHome.value=false
        }else{
            _navigateToHome.value=true
        }
    }
}