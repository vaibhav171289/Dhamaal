package com.lotus.dhamaal.utils

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.content.Context
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import com.lotus.dhamaal.activities.VideoRecording
import java.io.File
import java.io.IOException

class RecordVideo(context: Context, mediaRecorder: MediaRecorder) : SurfaceView( context), SurfaceHolder.Callback {
    private val TAG = "RecordVideo"
    private var surfaceHolder: SurfaceHolder = holder
    private var recorder: MediaRecorder? = mediaRecorder
    private var mediaPrepared = false
    init {
        surfaceHolder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun surfaceCreated(holder: SurfaceHolder?) {
       mediaPrepared = prepareMediaRecorder()

    }
    fun isPrepareMediaRecorder() = mediaPrepared
    @RequiresApi(Build.VERSION_CODES.M)
    private fun prepareMediaRecorder(): Boolean {
        recorder.let {
            it!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
            Log.d(TAG, "CAMERA check")
            it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            it.setCaptureRate(60.0)
            Log.d(TAG, "Output format  check")
            it.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P))
            Log.d(TAG, "IMAGE Quality check")
            it.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            Log.d(TAG, "Video encoder  check")
            it.setAudioSource(MediaRecorder.AudioSource.MIC)
            Log.d(TAG, "MIC check")
            it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            Log.d(TAG, "Audio encoder  check")
            val mediaFile: File = File(Environment.getExternalStorageDirectory().absolutePath + "/myvideo.mp4")
//            it.setOutputFile(mediaFile)
            Log.d(TAG, "Output Path check")
            VideoRecording.videoUri = Uri.fromFile(mediaFile);
            it.setMaxDuration(60000)
            Log.d(TAG, "MAX duration check")
            it.setInputSurface(surfaceHolder.surface)
            try {
                it.prepare()
                Log.d(TAG, "Prepare to launch")
            } catch (e: IllegalStateException) {
                releaseMediaRecorder()
                return false;
            } catch (e: IOException) {
                releaseMediaRecorder()
                return false;
            }
        }
        return true
    }
    private fun releaseMediaRecorder() {
        recorder.let {
            it?.reset()
            it?.release()
            val surface: Surface = it!!.surface
            surface.release()
        }

        recorder = null
    }
}