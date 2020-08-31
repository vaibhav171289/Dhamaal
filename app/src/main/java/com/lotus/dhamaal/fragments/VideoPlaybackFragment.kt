package com.lotus.dhamaal.fragments

import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.lotus.dhamaal.R

class VideoPlaybackFragment : Fragment() , PlayerControlView.VisibilityListener{
    private val videoRecordingViewModel: VideoRecordingViewModel by activityViewModels()
    private val videoPlaybackViewModel: VideoPlaybackViewModel by activityViewModels()
    companion object {
        private val TAG = VideoPlaybackFragment::class.qualifiedName
        fun newInstance() = VideoPlaybackFragment()
    }

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
    private val videoMediaSource by lazy {
        ProgressiveMediaSource.Factory(dataSource).createMediaSource(uri)
    }

    private fun playRecordedVideo() {
        playerView.player = exoPlayer
        exoPlayer.prepare(videoMediaSource)
        exoPlayer.playWhenReady = true
        videoPlaybackViewModel.getCurrentPosition()?.let { exoPlayer.seekTo(it) }
        playbackState = exoPlayer.playbackState
        exoPlayer.addListener(object: Player.EventListener{

        })

    }

    private fun releaseExoPlayer() {
        videoPlaybackViewModel.onChanged(exoPlayer.contentPosition)
        exoPlayer.playWhenReady = false
        exoPlayer.seekToDefaultPosition()
        exoPlayer.release()
    }
    private fun pauseVideoPlayBack(){
        videoPlaybackViewModel.onChanged(exoPlayer.contentPosition)
        exoPlayer.playWhenReady = false
        playbackState = exoPlayer.playbackState
    }
    private lateinit var uri: Uri
    private lateinit var playerView: PlayerView
    private lateinit var playButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private var playbackState: Int =  PlaybackState.ACTION_PLAY.toInt()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_view_video, container, false)
        playerView = view.findViewById(R.id.video_view)
        playButton= view.findViewById(R.id.exo_play)
        pauseButton  = view.findViewById(R.id.exo_pause)
        playButton.setOnClickListener {
            playButton.onVisibilityAggregated(false)
            pauseButton.onVisibilityAggregated(false)
            playButton.visibility = View.INVISIBLE
            pauseButton.visibility =View.INVISIBLE
            playRecordedVideo()
        }
        pauseButton.setOnClickListener {
            playButton.onVisibilityAggregated(false)
            pauseButton.onVisibilityAggregated(false)
            playButton.visibility = View.INVISIBLE
            pauseButton.visibility =View.INVISIBLE
            pauseVideoPlayBack()
        }
        playerView.keepScreenOn = true
        playerView.setKeepContentOnPlayerReset(true)
//        playerView.findViewById<PlayerControlView>(R.id.exo_controller)
//        val controlView =  inflater.inflate(R.layout.exo_player_control_view,container)
//        playerControlView =  controlView.findViewById(R.id.exo_play)
//        playerControlView = view.findViewById(R.id.player_control_view)
//        playerView.useController = false
        /*playerControlView.setOnClickListener {
            isPlaying = if (isPlaying) {
                stopRecordVideo()
                false
            } else {

                true
            }
        }*/
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "video uri ${videoRecordingViewModel.videoUri}")
        uri = videoRecordingViewModel.videoUri!!
    }

    override fun onVisibilityChange(visibility: Int) {
    }

    override fun onStart() {
        super.onStart()
        playRecordedVideo()
    }
    override fun onStop() {
        super.onStop()
        releaseExoPlayer()
    }

}
