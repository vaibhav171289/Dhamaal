package com.lotus.dhamaal.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.lotus.dhamaal.R
import com.lotus.dhamaal.fragments.PermissionFragment
import com.lotus.dhamaal.utils.FLAGS_FULLSCREEN
import java.io.File
import java.io.IOException

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class VideoRecording : AppCompatActivity() {
    private val TAG = "VideoRecording"
    private lateinit var container: FrameLayout
    private var mediaRecorder: MediaRecorder? = null
    private val permissionFragment = PermissionFragment()

  @RequiresApi(Build.VERSION_CODES.M)
  private fun prepareMediaRecorder() = MediaRecorder().apply{
          setVideoSource(MediaRecorder.VideoSource.SURFACE)
          Log.d(TAG, "CAMERA check")
          setAudioSource(MediaRecorder.AudioSource.MIC)
          Log.d(TAG, "MIC check")
          setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
          setCaptureRate(60.0)
          Log.d(TAG, "Output format  check")
//          it.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P))
//          Log.d(TAG, "IMAGE Quality check")
          setVideoEncoder(MediaRecorder.VideoEncoder.H264)
          Log.d(TAG, "Video encoder  check")
//          it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//          Log.d(TAG, "Audio encoder  check")
          val fileName : String = "/myvideo.mp4"
           val mediaFile: File =File(filesDir , fileName)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              setOutputFile(mediaFile)
          }
          Log.d(TAG, "Output Path check->$mediaFile")
          videoUri = Uri.fromFile(mediaFile)
      setMaxDuration(60000)
          Log.d(TAG, "MAX duration check")
//          it.setInputSurface(surfaceHolder.surface)
          try {
              prepare()
              Log.d(TAG, "Prepare to launch")
          } catch (e: IllegalStateException) {
              releaseMediaRecorder()
          } catch (e: IOException) {
              releaseMediaRecorder()
          }
      }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_recording)
        container = findViewById(R.id.fragment_video_container)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_video_container, permissionFragment)
        transaction.commit()
    /*

        videoRecordingViewModel =
            ViewModelProviders.of(this).get(VideoRecordingViewModel::class.java)
        videoRecordingViewModel.permissions.observe(this, Observer {
            isRecording = it
        })
        Log.d(TAG, " is recording init  $isRecording")
        //initializing media recorder
        mediaRecorder = MediaRecorder()
        // Request camera permissions
        if (!allPermissionsGranted()) {
            // Request camera-related permissions
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }

        } else {
            cameraSwitchButton = findViewById(R.id.camera_switch_button)
            previewView = findViewById(R.id.previewView)
            previewView.post {
                updateCameraUi()
                initCamera()
            }
            captureButton = findViewById(R.id.camera_capture_button)
            captureButton.setOnClickListener {

                // Setup the listener for take photo button
                startRecording()
            }
            playbackVideoButton = findViewById(R.id.playback_video)
            playbackVideoButton.setOnClickListener {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.fragment_container, videoPlaybackFragment)
                transaction.commit()
            }
            outputDirectory = getOutputDirectory(applicationContext)
        }*/
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun releaseMediaRecorder() {
        mediaRecorder.let {
            it?.reset()
            it?.release()
            val surface: Surface = it!!.surface
            surface.release()
        }

        mediaRecorder = null
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    companion object {
        public lateinit var videoUri: Uri
        private const val VIDEO_CAPTURE = 101
    }
}
