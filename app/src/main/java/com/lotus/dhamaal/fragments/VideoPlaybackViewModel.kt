package com.lotus.dhamaal.fragments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VideoPlaybackViewModel : ViewModel() {
    private var currentPosition = MutableLiveData<Long>(0)
    fun onChanged(pos: Long){
        currentPosition.value =  pos
    }
    fun getCurrentPosition() = currentPosition.value
}
