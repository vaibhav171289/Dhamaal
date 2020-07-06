package com.lotus.dhamaal.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VideoRecordingViewModel : ViewModel() {

    private val _text = MutableLiveData<Boolean>().apply {
        value = false
    }
    val permissions: LiveData<Boolean> = _text
}