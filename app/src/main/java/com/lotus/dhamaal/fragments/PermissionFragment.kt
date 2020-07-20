package com.lotus.dhamaal.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lotus.dhamaal.R
import com.lotus.dhamaal.activities.MainActivity

/**
 * A simple [Fragment] subclass.
 */
class PermissionFragment() : Fragment() {
   companion object{
       private val TAG = PermissionFragment::class.qualifiedName
       private const val REQUEST_CODE_PERMISSIONS = 10
       /** Permissions for using the device hardware*/
       private val REQUIRED_PERMISSIONS = arrayOf(
           Manifest.permission.CAMERA,
           Manifest.permission.RECORD_AUDIO,
           Manifest.permission.WRITE_EXTERNAL_STORAGE,
           Manifest.permission.INTERNET
       )
   }
    private  val videoRecordingFragment = VideoRecordingFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request camera permissions
        if (!allPermissionsGranted()) {
            // Request camera-related permissions
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()
                /*  Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                      PermissionsFragmentDirections.actionPermissionsToCamera())*/
                Log.d(TAG, "Permissions granted successfully")
                activity?.let {  val transaction =it.supportFragmentManager.beginTransaction()
                    transaction.add(R.id.fragment_video_container, videoRecordingFragment)
                    transaction.commit()  }

            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
                val intent: Intent = Intent(context, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }
    /** Convenience method used to check if all permissions required by this app are granted */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        Log.d(TAG, "permission name: $it")
        activity?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it
            )
        } != PackageManager.PERMISSION_GRANTED
    }

}
