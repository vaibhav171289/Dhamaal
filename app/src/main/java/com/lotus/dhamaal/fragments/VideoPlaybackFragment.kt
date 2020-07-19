package com.lotus.dhamaal.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView

import com.lotus.dhamaal.R
import com.lotus.dhamaal.activities.VideoRecording

class VideoPlaybackFragment : Fragment() {

    companion object {
//        fun newInstance() = VideoPlaybackFragment()
    }

    private lateinit var viewModel: VideoPlaybackViewModel
    private lateinit var videoView: VideoView
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
        viewModel = ViewModelProviders.of(this).get(VideoPlaybackViewModel::class.java)
        // TODO: Use the ViewModel
    }
    private fun playbackRecordedVideo() {
        videoView.setVideoURI(VideoRecording.videoUri)
        videoView.setMediaController(MediaController(context))
        videoView.requestFocus()

        videoView.start()
    }
}
