package com.lotus.dhamaal.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.AppCompatImageButton
import com.lotus.dhamaal.R
import java.io.File
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.common.util.concurrent.ListenableFuture
import com.lotus.dhamaal.fragments.PermissionFragment
import com.lotus.dhamaal.fragments.VideoPlaybackFragment
import com.lotus.dhamaal.fragments.VideoRecordingFragment
import com.lotus.dhamaal.utils.FLAGS_FULLSCREEN
import java.io.IOException

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L
const val REQUEST_VIDEO_CAPTURE = 1

class VideoRecording : AppCompatActivity(), CameraXConfig.Provider {
    private val TAG = "VideoRecording"
    private lateinit var captureButton: AppCompatImageButton
    private lateinit var playbackVideoButton: AppCompatImageButton
    private lateinit var cameraSwitchButton: ImageButton
    private lateinit var preview: Preview
    private lateinit var preview1: Preview
    private lateinit var previewView: PreviewView
    private lateinit var cameraSelector: CameraSelector
    private lateinit var camera: Camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var outputDirectory: File
    private lateinit var container: FrameLayout
    private lateinit var videoRecordingViewModel: VideoRecordingViewModel
    private var mediaRecorder: MediaRecorder? = null
    private var lensCurrentFacing: Int = CameraSelector.LENS_FACING_FRONT
    private val videoPlaybackFragment by lazy {  VideoPlaybackFragment() }
    private val permissionFragment = PermissionFragment()
    private lateinit var  surface: Surface
    private lateinit var cameraDevice: CameraDevice
    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
  /*  val c1 = object: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            camera.let {
                cameraDevice= it
            }

        }
    }*/
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
          videoUri = Uri.fromFile(mediaFile);
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

    private fun startRecording() {
        Log.d(TAG, " is recording started $isRecording")
        isRecording = if (isRecording) {
            mediaRecorder!!.stop() // stop the recording
            releaseMediaRecorder() // release the MediaRecorder object
            Toast.makeText(this, "Video captured!", Toast.LENGTH_LONG).show()
            false
        } else {
//            val recordVideo = RecordVideo(this, mediaRecorder!!)
//            setContentView(previewView)
            mediaRecorder = prepareMediaRecorder()
            runOnUiThread {
                mediaRecorder!!.start()
            }
            true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VIDEO_CAPTURE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Toast.makeText(this, "Video has been saved to:\n" + data?.data, Toast.LENGTH_LONG).show()
//                    playbackRecordedVideo()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Video recording cancelled.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(this, "Failed to record video", Toast.LENGTH_LONG).show()
                }
            }
        }
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



    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }


    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {
        Log.d(TAG, "binding preview")
        //Create a Preview
        preview = Preview.Builder().build()
        //Specify the desired camera LensFacing option.
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensCurrentFacing)
            .build()
        //Bind the selected camera and any use cases to the lifecycle.
        preview.setSurfaceProvider(previewView.createSurfaceProvider())
        cameraProvider?.unbindAll()
        //Connect the Preview to the PreviewView.
        camera = cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)!!
        Log.d(TAG, "binding preview ${camera.cameraInfo.sensorRotationDegrees}")
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun initCamera() {
        Log.d(TAG, "initializing camera")
        //initializing camera provider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Enable or disable switching between cameras

        //verify initialization
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            //calling the preview method
            lensCurrentFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            updateCameraSwitchButton()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                /*  Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                      PermissionsFragmentDirections.actionPermissionsToCamera())*/
                Log.d(TAG, "Permissions granted successfully")
                val intent: Intent = Intent(this, VideoRecording::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        // Setup for button used to switch cameras
        cameraSwitchButton.let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
                lensCurrentFacing = if (CameraSelector.LENS_FACING_FRONT == lensCurrentFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                Log.d(TAG, "setting lens_facing to $lensCurrentFacing")
                // Re-bind use cases to update selected camera
                bindPreview(cameraProvider)
            }
        }
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = container.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
            Log.d(TAG, "updating camera switch button to ${switchCamerasButton.isEnabled}")
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton.isEnabled = false
        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Redraw the camera UI controls
        updateCameraUi()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        )
        private var isRecording: Boolean = false
        public lateinit var videoUri: Uri
        private const val VIDEO_CAPTURE = 101
        /**
        Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}
