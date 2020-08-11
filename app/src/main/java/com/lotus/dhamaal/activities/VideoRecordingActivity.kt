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
    private val permissionFragment = PermissionFragment.newInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_recording)
        container = findViewById(R.id.fragment_video_container)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_video_container, permissionFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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
        private const val VIDEO_CAPTURE = 101
    }
}
