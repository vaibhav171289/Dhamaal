package com.lotus.dhamaal.fragments

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

class VideoRecordingViewModel : ViewModel() {
    private val _text = MutableLiveData<Boolean>()
    fun onChanged(value:Boolean){
          _text.value  = value;
    }
    var _isRecording: LiveData<Boolean> = _text

    var _videoUri: Uri? =null
    var width: Int = 1080
    var height: Int = 1920
    var isSurfaceAvailable = false
}
