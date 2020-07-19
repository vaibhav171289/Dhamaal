package com.lotus.dhamaal.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.lotus.dhamaal.R
import com.lotus.dhamaal.ui.dashboard.DashboardFragment
import com.lotus.dhamaal.ui.home.HomeFragment
import com.lotus.dhamaal.ui.notifications.NotificationsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val homeFragment: HomeFragment = HomeFragment()
    private val dashboardFragment: DashboardFragment = DashboardFragment()
    private val notificationsFragment: NotificationsFragment = NotificationsFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        actionBar?.setDisplayShowHomeEnabled(false)
        actionBar?.title = "vaibhav"
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        nav_view.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, homeFragment)
        transaction.commit()
    }
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        //supportFragmentManager is use and FragmentManager is Deprciated in androidx
        val transaction = supportFragmentManager.beginTransaction()
        //custom animation for each fragment so when we switch between fragments how should they behave
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        when (item.itemId) {
            R.id.navigation_home -> transaction.replace(R.id.fragment_container, homeFragment)
            R.id.navigation_dashboard -> transaction.replace(R.id.fragment_container, dashboardFragment)
            R.id.navigation_notifications -> transaction.replace(R.id.fragment_container, notificationsFragment)
        }

        transaction.commit()
        true;
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }
    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.optionSignOut -> {
                //todo: correct the activity to start
//                signout()
                val intent: Intent = Intent(this, VideoRecording::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }
            //todo: correct the activity to start
            R.id.optionAccountSettings -> {
                val intent: Intent = Intent(this, VideoRecording::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }
            R.id.startVideo -> {
                if(!checkCameraHardware(this)) {
                    Toast.makeText(this,"Device Doesn't Support Camera ", Toast.LENGTH_SHORT).show()
                    return false
                }
                val intent: Intent = Intent(this, VideoRecording::class.java)
               /* intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK*/
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
