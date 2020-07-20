package com.lotus.dhamaal.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.CameraInfoUnavailableException
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.lotus.dhamaal.R

class VideoRecordingFragment : Fragment() {

    companion object {
        private val TAG = VideoRecordingFragment::class.qualifiedName
        @JvmStatic fun newInstance() = VideoRecordingFragment()
        private var isRecording: Boolean = false
        private const val MAX_PREVIEW_WIDTH= 800
        private  const val MAX_PREVIEW_HEIGHT = 600
    }
    private lateinit var cameraSwitchButton: ImageButton
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

            Log.e(TAG," exception in thread handler VideoRecording Fragment ${e.toString()}")
        }
    }

    private var lensCurrentFacing: Int = CameraCharacteristics.LENS_FACING_FRONT
    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var cameraDevice: CameraDevice

    /**[Surface] to hold the preview of the camera*/
    private lateinit var surface:Surface

    /**[CameraCaptureSession] to record the video*/
    private lateinit var cameraCaptureSession: CameraCaptureSession
    /** Requests used for preview only in the [CameraCaptureSession] */
    private lateinit var previewRequest: CaptureRequest.Builder
    private fun createCaptureRequest(): CaptureRequest{
        // Capture request holds references to target surfaces
        return cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview surface target
            addTarget(surface)
            Log.d(TAG, "surface added to the target and tag ")
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            Log.d(TAG, " setting capture request: ${CaptureRequest.CONTROL_AF_MODE} and ${CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO}")
        }.build()

    }
    private fun previewSession(){
        Log.d(TAG, "Is surface valid:  ${surface.isValid}")
        cameraDevice.createCaptureSession(
            listOf(surface),
            object:CameraCaptureSession.StateCallback(){
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG,"creating capture session failed\n${session.inputSurface}")
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onConfigured(session: CameraCaptureSession) {

                    Log.d(TAG,"Capture session is configured successfully ${cameraDevice.id}")
                    session.let {
                        cameraCaptureSession= it
                        it.setRepeatingRequest( createCaptureRequest(),null, null)
                    }
                }
            }, null)
    }
    private fun closeCamera(){
        if(this::cameraCaptureSession.isInitialized)
            cameraCaptureSession.close()
        if(this::cameraDevice.isInitialized)
            cameraDevice.close()
    }
    private val deviceStateCallBack = object: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG,"Camera device is ready to use")
            camera.let {
                cameraDevice = camera
                Log.d(TAG,"Camera device is ready to use : ${cameraDevice.id}")
                previewSession()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG,"Camera device is disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {

            Log.d(TAG,"Camera device OnError")
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
    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T? {

         val characteristics =  cameraManager.getCameraCharacteristics(cameraId)
        return when (key){
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            else -> throw IllegalStateException("Key is not recognised")
        }
    }
    /**[CameraCharacteristics.LENS_FACING]*/
    private fun cameraId(lens: Int): String{
        var deviceId = listOf<String>()
        try{
            val cameraList  = cameraManager.cameraIdList
            Log.d(TAG,"Camera id list-> ${cameraList.asList()}")
            deviceId = cameraList.filter { lens == cameraCharacteristics(it,CameraCharacteristics.LENS_FACING )}
        }catch(e: CameraAccessException){
            Log.e(TAG," Unable to locate a camera device\n${e.toString()}")
        }
        return deviceId[0]
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private  fun connectCamera(){
        /*Initialize surface before using it*/
        previewTextureView.surfaceTexture.apply {
            setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        }
        surface = Surface(previewTextureView.surfaceTexture)
        updateCameraSwitchButton()
        updateCameraUi()
         val cameraId =  cameraId(lensCurrentFacing)
        Log.d(TAG, "Current camera id for lens $cameraId")
        try {
            Log.d(TAG,"handler is alive: ${handler}")
            cameraManager.openCamera(cameraId, deviceStateCallBack, handler)
        }
        catch (e: CameraAccessException){
            Log.e(TAG, e.toString())
        }catch (e: InterruptedException){
            Log.e(TAG," Interrupted While opening the Camera device Id: $cameraId  ${e.toString()}")
        }

    }
    /**[AppCompatImageButton] to record the video*/
    private lateinit var captureButton: AppCompatImageButton
    private lateinit var viewModel: VideoRecordingViewModel
    /**[TextureView] is used to hold the video on the screen*/
    private lateinit var previewTextureView: TextureView

    /**[TextureView.SurfaceTextureListener] */
    private val surfaceListener = object: TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "surface texture changed width: $width and  height $height")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = true

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
          Log.d(TAG, "surface texture view is available width: $width and  height $height")
            connectCamera()
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
    @RequiresApi(Build.VERSION_CODES.M)
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
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setPreviewTextureView(){
        Log.d(TAG,"on resume Texture view is avaialble : ${previewTextureView.isAvailable}")
        if(previewTextureView.isAvailable){
            connectCamera()
        }else {
            previewTextureView.surfaceTextureListener = surfaceListener

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_video_recording, container, false)
        previewTextureView = view.findViewById(R.id.previewTextureView)
        captureButton = view.findViewById(R.id.camera_capture_button)
        captureButton.isClickable =true
        cameraSwitchButton = view.findViewById<ImageButton>(R.id.camera_switch_button)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideoRecordingViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
}
