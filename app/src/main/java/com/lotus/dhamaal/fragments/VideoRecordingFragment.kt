package com.lotus.dhamaal.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.impl.CameraCaptureResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.lotus.dhamaal.R
import com.lotus.dhamaal.activities.VideoRecording
import kotlinx.android.synthetic.main.fragment_video_recording.*
import java.util.*

class VideoRecordingFragment : Fragment() {

    companion object {
        private val TAG = VideoRecordingFragment::class.qualifiedName
        @JvmStatic fun newInstance() = VideoRecordingFragment()
        private var isRecording: Boolean = false
        private val MAX_PREVIEW_WIDTH= 1280
        private  val MAX_PREVIEW_HEIGHT = 720
    }
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

    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var cameraDevice: CameraDevice

    /**[Surface] to hold the preview of the camera*/
    private lateinit var surface:Surface

    /**[CameraCaptureSession] to record the video*/
    private lateinit var cameraCaptureSession: CameraCaptureSession
    /** Requests used for preview only in the [CameraCaptureSession] */
    private val previewRequest: CaptureRequest by lazy {
        // Capture request holds references to target surfaces
        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            // Add the preview surface target
            addTarget(surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        }.build()
    }
    private fun previewSession(){
        cameraDevice.createCaptureSession(
            listOf(surface),
            object:CameraCaptureSession.StateCallback(){
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG,"creating capture session failed")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG,"Capture session is configured successfully")
                    session.let {
                        cameraCaptureSession= it
                        it.setRepeatingRequest(previewRequest,null, null)
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
            deviceId = cameraList.filter { lens == cameraCharacteristics(it,CameraCharacteristics.LENS_FACING )}
        }catch(e: CameraAccessException){
            Log.e(TAG," Unable to locate a camera device\n${e.toString()}")
        }
        return deviceId[0]
    }

    @SuppressLint("MissingPermission")
    private  fun connectCamera(){
        /*Initialize surface before using it*/
        previewTextureView.surfaceTexture.apply {
            setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        }
        surface = Surface(previewTextureView.surfaceTexture)
         val cameraId =  cameraId(CameraCharacteristics.LENS_FACING_BACK)
        Log.d(TAG, "camera id for back lens $cameraId")
        try {
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

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
          Log.d(TAG, "surface texture view is available width: $width and  height $height")
            connectCamera()
        }

    }
    override fun onResume() {
        super.onResume()
        startHandler()
        if(previewTextureView.isAvailable){
            connectCamera()
        }else {
            previewTextureView.surfaceTextureListener = surfaceListener
            Log.d(TAG,"on resume surface listner avaialble")
        }
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
}
