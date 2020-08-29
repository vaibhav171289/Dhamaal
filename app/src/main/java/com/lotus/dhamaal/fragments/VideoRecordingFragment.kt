package com.lotus.dhamaal.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.util.SparseArray
import android.view.*
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.CameraInfoUnavailableException
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.lotus.dhamaal.R
import com.lotus.dhamaal.utils.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VideoRecordingFragment : Fragment() {
    private  val videoRecordingViewModel: VideoRecordingViewModel by activityViewModels()
    companion object {
        private val TAG = VideoRecordingFragment::class.qualifiedName
        @JvmStatic fun newInstance() = VideoRecordingFragment()
        var isRecording: Boolean = false
        private const val SENSOR_DEFAULT_ORIENTATION_DEGREES = 90
        private const val SENSOR_INVERSE_ORIENTATION_DEGREES = 270
        private val DEFAULT_ORIENTATION = SparseArray<Int>().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }
        private val INVERSE_ORIENTATION =  SparseArray<Int>().apply {
            append(Surface.ROTATION_0, 270)
            append(Surface.ROTATION_90, 180)
            append(Surface.ROTATION_180, 90)
            append(Surface.ROTATION_270, 0)
        }


        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
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
    private lateinit var videoFilePath: String
    private fun createVideoFilename():String{
        val timestamp = SimpleDateFormat(FILENAME_FORMAT).format(Date())
        return "VIDEO_${timestamp}.mp4"
    }
    private fun createVideoFile(): File{
        val  videoFile = File(getOutputDirectory(requireContext()), createVideoFilename())
        videoFilePath =  videoFile.absolutePath
        Log.d(TAG, "saved video path : $videoFilePath ")
        return videoFile
    }
    /**[MediaRecorder]*/
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var cameraSwitchButton: ImageButton
    private lateinit var chronometer: Chronometer
    /** [HandlerThread] handler thread to update camera recording to the UI*/
    private lateinit var handlerThread: HandlerThread
    /**[Handler] for mananging [HandlerThread]*/
    private lateinit var handler: Handler
    /** [CameraManager] Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun startHandler(){
        handlerThread = HandlerThread("Video recoding").also { it.start() }
        handler =  Handler(handlerThread.looper)
    }
    private fun stopHandler(){
        handlerThread.quitSafely()
        try {
            handlerThread.join()
        }catch (e: InterruptedException){

            Log.e(TAG, " exception in thread handler VideoRecording Fragment ${e.toString()}")
        }
    }

    private var lensCurrentFacing: Int = CameraCharacteristics.LENS_FACING_FRONT
    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var cameraDevice: CameraDevice

    /**[Surface] to hold the preview of the camera*/
    private lateinit var textureSurface:Surface
    /** Saves the video recording */
    private lateinit var recorderSurface: Surface
    /**[CameraCaptureSession] to record the video*/
    private lateinit var cameraCaptureSession: CameraCaptureSession
    /** Requests used for preview only in the [CameraCaptureSession] */
    private lateinit var previewRequest: CaptureRequest.Builder
    private fun createPreviewCaptureRequest(): CaptureRequest{
        // Capture request holds references to target surfaces
        return cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            // Add the preview surface target
            addTarget(textureSurface)
            Log.d(TAG, "surface added to the target and tag ")
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            Log.d(
                TAG,
                " setting capture request: ${CaptureRequest.CONTROL_AF_MODE} and ${CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE}"
            )
        }.build()

    }
    private fun createRecordCaptureRequest(): CaptureRequest{
        // Capture request holds references to target surfaces
        return cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview surface target
            addTarget(textureSurface)
            addTarget(recorderSurface)
            Log.d(TAG, "2 surfaces are added to the target and tag ")
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            Log.d(
                TAG,
                " setting capture request: ${CaptureRequest.CONTROL_AF_MODE} and ${CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE}"
            )
        }.build()

    }
    private fun previewSession(){
        Log.d(TAG, "Is surface valid:  ${textureSurface.isValid}")
        cameraDevice.createCaptureSession(
            listOf(textureSurface),
            object : CameraCaptureSession.StateCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating capture session failed\n${session.inputSurface}")
                }

                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onConfigured(session: CameraCaptureSession) {

                    Log.d(TAG, "Capture session is configured successfully ${cameraDevice.id}")
                    session.let {
                        cameraCaptureSession = it
                        it.setRepeatingRequest(createPreviewCaptureRequest(), null, null)
                    }
                }
            }, handler
        )
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recordSession(){
        mediaRecorder = MediaRecorder()
        prepareMediaRecorder()
        recorderSurface  =  mediaRecorder!!.surface
        val surfaces = listOf<Surface>(textureSurface, recorderSurface)
        Log.d(TAG, "Is Texture surface valid:  ${textureSurface.isValid}")
        Log.d(TAG, "Is Recorder surface valid:  ${recorderSurface.isValid}")
        cameraDevice.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating record capture session failed\n${session.inputSurface}")
                    releaseMediaRecorder()
                }

                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onConfigured(session: CameraCaptureSession) {

                    Log.d(TAG, "Capture session is configured successfully ${cameraDevice.id}")
                    session.let {
                        cameraCaptureSession = it
                        it.setRepeatingRequest(createRecordCaptureRequest(), null, null)
                        isRecording = true
                        Log.d(TAG, "++++++++++++++++++>starting media recorder")
                    }
                }
            }, handler
        )
    }
    private fun closeCamera(){
        if(this::cameraCaptureSession.isInitialized)
            cameraCaptureSession.close()
        if(this::cameraDevice.isInitialized)
            cameraDevice.close()
    }
    private val deviceStateCallBack = object: CameraDevice.StateCallback(){
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera device is ready to use")
            camera.let {
                cameraDevice = camera
                Log.d(TAG, "Camera device is ready to use : ${cameraDevice.id}")
               previewSession()
                mPreviewSize?.let { it1 ->
                    configureTransform(requireActivity(),
                        it1, surfaceView, surfaceView.width, surfaceView.height)
                }
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "Camera device is disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {

            Log.d(TAG, "Camera device OnError")
            requireActivity().finish()
            val msg = when(error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            val exc = RuntimeException("Camera  error: ($error) $msg")
            Log.e(TAG, exc.message, exc)
        }

    }
    /**[MediaRecorder] to setup the capture  of audio and video formats*/
    private fun releaseMediaRecorder(){
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
                mediaRecorder = null
                Log.d(TAG, "++++++++++++++++++>stopping media recorder")
            }
        }catch (e: IllegalStateException){
            Log.e(TAG, "release media recorder ${Log.getStackTraceString(e)}}")
        }catch (e: java.lang.RuntimeException){
            Log.e(TAG, "runtime media recorder ")
            Log.e(TAG, Log.getStackTraceString(e))
//            e.stackTrace.asList().forEach { Log.d(TAG, it.toString()); }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun prepareMediaRecorder(){
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        try {
            when (cameraCharacteristics(cameraId(lensCurrentFacing), CameraCharacteristics.SENSOR_ORIENTATION)) {
                SENSOR_DEFAULT_ORIENTATION_DEGREES -> mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATION.get(rotation))
                SENSOR_INVERSE_ORIENTATION_DEGREES -> mediaRecorder?.setOrientationHint(INVERSE_ORIENTATION.get(rotation))
            }
        }catch (e: java.lang.IllegalStateException){
            Log.d(TAG, "Sensor orientation : $e")
        }

        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
               setOutputFile(createVideoFile())
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            //if poor video quality change encoding bit rate
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(videoRecordingViewModel.width, videoRecordingViewModel.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() ${Log.getStackTraceString(e)}")
            }
            start()
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startRecording() {
        Log.d(TAG, " is recording started $isRecording")
        isRecording = if (isRecording) {
            stopChronometer()
            releaseMediaRecorder() // release the MediaRecorder object
            previewSession()
            videoRecordingViewModel._videoUri =  Uri.fromFile(File(videoFilePath))
            playbackVideo.isEnabled = true
            Toast.makeText(requireContext(), "Video captured Complete!", Toast.LENGTH_LONG).show()
            false
        } else {
            startChronoMeter()
            recordSession()
            true
        }
        videoRecordingViewModel.onChanged(isRecording)

    }
    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T? {

         val characteristics =  cameraManager.getCameraCharacteristics(cameraId)
        return when (key){
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            CameraCharacteristics.SENSOR_ORIENTATION -> characteristics.get(key)
            else -> throw IllegalStateException("Key is not recognised")
        }
    }
    /**[CameraCharacteristics.LENS_FACING]*/
    private fun cameraId(lens: Int): String{
        var deviceId = listOf<String>()
        try{
            val cameraList  = cameraManager.cameraIdList
            Log.d(TAG, "Camera id list-> ${cameraList.asList()}")
            deviceId = cameraList.filter { lens == cameraCharacteristics(it, CameraCharacteristics.LENS_FACING)}
        }catch (e: CameraAccessException){
            Log.e(TAG, " Unable to locate a camera device\n${e.toString()}")
        }
        return deviceId[0]
    }

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * The [Size] of video recording.
     */
    private var mVideoSize: Size? = null
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private  fun connectCamera(width1: Int, height1: Int) {
        /*Initialize surface before using it*/
        /*previewTextureView.surfaceTexture.apply {
            setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        }
        textureSurface = Surface(previewTextureView.surfaceTexture)*/
      /*  val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(lensCurrentFacing.toString())
        val map = characteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        mVideoSize = chooseVideoSize(
            map!!.getOutputSizes(
                MediaRecorder::class.java
            )
        )
        mPreviewSize = chooseOptimalSize(
            map.getOutputSizes(
                SurfaceTexture::class.java
            ),
            width1, height1, mVideoSize!!
        )

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            surfaceView.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
        } else {
            surfaceView.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
        }
        videoRecordingViewModel.width = mPreviewSize!!.width
        videoRecordingViewModel.height = mPreviewSize!!.height*/
        videoRecordingViewModel.width = width1
        videoRecordingViewModel.height = height1
        /*surfaceView.layoutParams.apply {
            height = MAX_PREVIEW_HEIGHT
            width = MAX_PREVIEW_WIDTH
        }*/
        textureSurface = Surface(surfaceView.surfaceControl)
        updateCameraSwitchButton()
        updateCameraUi()
        val cameraId =  cameraId(lensCurrentFacing)
        Log.d(TAG, "Current camera id for lens $cameraId")
        try {
            Log.d(TAG, "handler is alive: ${handler}")
            cameraManager.openCamera(cameraId, deviceStateCallBack, handler)
        }
        catch (e: CameraAccessException){
            Log.e(TAG, e.toString())
        }catch (e: InterruptedException){
            Log.e(TAG, " Interrupted While opening the Camera device Id: $cameraId  ${e.toString()}")
        }
    }
    /**[AppCompatImageButton] to record the video*/
    private lateinit var captureButton: AppCompatImageButton

    /**[AppCompatImageButton] to record the video*/
    private lateinit var playbackVideo: AppCompatImageButton
    /**[TextureView] is used to hold the video on the screen*/
    private lateinit var previewTextureView: TextureView

    /**[TextureView.SurfaceTextureListener] */
    private val surfaceListener = object: TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "surface texture changed width: $width and  height $height")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            releaseMediaRecorder()
            closeCamera()
            return true
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
          Log.d(TAG, "surface texture view is available width: $width and  height $height")
            connectCamera(width, height)

        }

    }
    private lateinit var surfaceView: AutoFitSurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    /** Overlay on top of the camera preview */
    private lateinit var overlay: View

    private val surfaceHolderCallback = object : SurfaceHolder.Callback{
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) = Unit

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            releaseMediaRecorder()
            closeCamera()
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun surfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, "surface holder created")
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(lensCurrentFacing.toString())
            val previewSize = getPreviewOutputSize(
                surfaceView.display, cameraCharacteristics, SurfaceHolder::class.java)
            Log.d(TAG, "Surface view display size: ${surfaceView.width} x ${surfaceView.height}")
            surfaceView.setAspectRatio(previewSize.width, previewSize.height)

            // To ensure that size is set, initialize camera in the view's thread
            view?.post { connectCamera(surfaceView.width, surfaceView.height) }
        }

    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraManager.cameraIdList.contains(CameraCharacteristics.LENS_FACING_BACK.toString())
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraManager.cameraIdList.contains(CameraCharacteristics.LENS_FACING_FRONT.toString())
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        try {
            cameraSwitchButton.isEnabled = hasBackCamera() && hasFrontCamera()
            Log.d(TAG, "updating camera switch button to ${cameraSwitchButton.isEnabled}")
        } catch (exception: CameraInfoUnavailableException) {
            cameraSwitchButton.isEnabled = false
        }
    }
    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateCameraUi() {
        // Setup for button used to switch cameras
        cameraSwitchButton.let {

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
                lensCurrentFacing = if (CameraCharacteristics.LENS_FACING_FRONT == lensCurrentFacing) {
                    CameraCharacteristics.LENS_FACING_BACK
                } else {
                    CameraCharacteristics.LENS_FACING_FRONT
                }
                Log.d(TAG, "setting lens_facing to $lensCurrentFacing")
                closeCamera()
                // Re-bind use cases to update selected camera
                setPreviewTextureView()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setPreviewTextureView(){
       /* Log.d(TAG,"on resume Texture view is avaialble : ${previewTextureView.isAvailable}")
        if(previewTextureView.isAvailable){
            connectCamera()
        }else {
            previewTextureView.surfaceTextureListener = surfaceListener

        }*/
        Log.d(TAG, "on resume Texture view is avaialble : ${surfaceHolder.surface.isValid}")
        if(videoRecordingViewModel.isSurfaceAvailable  && surfaceHolder.surface.isValid){
            connectCamera(videoRecordingViewModel.width, videoRecordingViewModel.height)
        }else {
            videoRecordingViewModel.isSurfaceAvailable = true;
            surfaceHolder =  surfaceView.holder
            surfaceHolder.addCallback(surfaceHolderCallback)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startChronoMeter(){
        chronometer.base =  SystemClock.elapsedRealtime()
        chronometer.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        chronometer.start()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stopChronometer(){
        chronometer.setTextColor(resources.getColor(android.R.color.white, null))
        chronometer.stop()
    }
    private fun replaceFragment(fragment: Fragment){
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_video_container, fragment)
        transaction.addToBackStack(fragment::class.qualifiedName)
        transaction.setReorderingAllowed(true)
        transaction.commit()
    }

    override fun onDestroyView() {
        val container =  requireActivity().findViewById<ConstraintLayout>(R.id.video_recording_layout)
        super.onDestroyView()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        startHandler()
        setPreviewTextureView()
    }

    override fun onPause() {
        closeCamera()
        stopHandler()
        super.onPause()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_recording, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        videoRecordingViewModel._isRecording.observe(
            viewLifecycleOwner,
            Observer { it ->
                isRecording = it
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

//        previewTextureView = view.findViewById(R.id.previewTextureView)
        overlay = view.findViewById(R.id.overlay)
        surfaceView =  view.findViewById(R.id.previewSurfaceView)
        surfaceHolder =  surfaceView.holder!!
        surfaceHolder.addCallback(surfaceHolderCallback)
        chronometer =  view.findViewById(R.id.chronometer1)
        captureButton = view.findViewById(R.id.camera_capture_button)
        captureButton.isClickable =true
        playbackVideo =  view.findViewById(R.id.process_video)
        playbackVideo.isEnabled = false
        cameraSwitchButton = view.findViewById<ImageButton>(R.id.camera_switch_button)
        captureButton.setOnClickListener {
            Log.d(TAG, "capture button clicked")
            startRecording()
        }
        playbackVideo.setOnClickListener {
            if(playbackVideo.isEnabled) {
                val videoPlaybackFragment = VideoPlaybackFragment.newInstance()
                replaceFragment(videoPlaybackFragment)
            }
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
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration is changed please save the state");
        // Redraw the camera UI controls
        updateCameraUi()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }
    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
    }
}
