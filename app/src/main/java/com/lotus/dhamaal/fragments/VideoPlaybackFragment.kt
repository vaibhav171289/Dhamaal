package com.lotus.dhamaal.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.lotus.dhamaal.R
import com.lotus.dhamaal.activities.VideoRecording

class VideoPlaybackFragment : Fragment() {
    private val videoRecordingViewModel: VideoRecordingViewModel by activityViewModels()
    companion object {
        private val TAG = VideoPlaybackFragment::class.qualifiedName
        fun newInstance() = VideoPlaybackFragment()
    }

    private lateinit var videoView: VideoView
    private  lateinit var uri: Uri;
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_edit_view_video, container, false)
        videoView   = view.findViewById(R.id.video_view)
//        playbackRecordedVideo()
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG,"video uri ${videoRecordingViewModel._videoUri}")
    }
    private fun playbackRecordedVideo() {
        videoView.setVideoURI(VideoRecording.videoUri)
        videoView.setMediaController(MediaController(context))
        videoView.requestFocus()

        videoView.start()
    }
}
