package com.lotus.dhamaal.fragments

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VideoRecordingViewModel : ViewModel() {
    private val _text = MutableLiveData<Boolean>()
    fun onChanged(value:Boolean){
          _text.value  = value
    }
    var isRecording: LiveData<Boolean> = _text

    var videoUri: Uri? =null
    var width: Int = 1920
    var height: Int = 1080
    fun updateWindow(w: Int, h: Int){
        width  =w
        height = h
    }
}
