package com.lotus.dhamaal.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.lotus.dhamaal.R
import kotlinx.android.synthetic.main.fragment_edit_view_video.*

class VideoPlaybackFragment : Fragment() {
    private val videoRecordingViewModel: VideoRecordingViewModel by activityViewModels()

    companion object {
        private val TAG = VideoPlaybackFragment::class.qualifiedName
        fun newInstance() = VideoPlaybackFragment()
    }

    private val bandwidthMeter by lazy { DefaultBandwidthMeter.Builder(requireContext()) }

    //    private  val trackSelectionFactory by lazy { AdaptiveTrackSelection.Factory(bandwidthMeter) }
    private val exoPlayer by lazy {
        SimpleExoPlayer.Builder(requireContext()).apply {
        }.build()
    }
    private val applicationName by lazy {
        requireContext().applicationInfo.loadLabel(requireContext().packageManager).toString()
    }
    private val dataSource by lazy {
        DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), applicationName))
    }
    private  val videoMediaSource by lazy {
        ProgressiveMediaSource.Factory(dataSource).createMediaSource(uri)
    }
    private fun playRecordedVideo(){
        video_view.controllerAutoShow = false
        video_view.player = exoPlayer
        exoPlayer.prepare(videoMediaSource)
        exoPlayer.playWhenReady =true
    }
    private fun stopRecordVideo(){
        exoPlayer.stop()
        exoPlayer.release()
    }
    private lateinit var uri: Uri;
    private  lateinit var play_pause: ImageButton
    private  var isPlaying: Boolean  =false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_view_video, container, false)
        play_pause =  view.findViewById(R.id.play_pause)
        play_pause.setOnClickListener{
           isPlaying= if(isPlaying){
               stopRecordVideo()
               false
           }else{
               playRecordedVideo()
               true
           }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "video uri ${videoRecordingViewModel._videoUri}")
        uri =  videoRecordingViewModel._videoUri!!
    }

}
