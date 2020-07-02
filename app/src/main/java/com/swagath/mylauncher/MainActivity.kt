package com.swagath.mylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    val tag = "MainActivity"
    var gestureDetector: GestureDetector? = null
    var isContainue = true
    lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private fun updateDateTime() {
        mRunnable = object : Runnable {
            override fun run() {
                if (isContainue) {
                    mHandler.postDelayed(this, 10000)
                }
                runOnUiThread {
                    val sdf = SimpleDateFormat("EEEE, dd MMM ", Locale.getDefault())
                    val currentDateandTime: String = sdf.format(Date())
                    if (main_date_time_widget.text != currentDateandTime) {
                        main_date_time_widget.text = currentDateandTime
                    }

                }
            }
        }
        mHandler = Handler(Looper.getMainLooper())
        if (isContainue) {
            mHandler.postDelayed(mRunnable, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val w: Window = window

        w.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        gestureDetector = GestureDetector(this@MainActivity, this@MainActivity)

        val sdf = SimpleDateFormat("EEEE, dd MMM ", Locale.getDefault())
        val currentDateandTime: String = sdf.format(Date())

        main_date_time_widget.text = currentDateandTime
        main_date_time_widget.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Please install Google Clock App from PlayStore",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        updateDateTime()

        main_up_button.setOnClickListener {
            val intent = Intent(this, ShowAllAppsActivity::class.java)
            startActivity(intent)
        }


        dock_icon_01.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.dialer")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Please install Dialer App",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        dock_icon_02.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Please install Messages App",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        dock_icon_04.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.android.chrome")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Please install Google Chrome App",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        dock_icon_05.setOnClickListener {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.GoogleCamera")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Please install Google Camera App",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setIcon("com.google.android.dialer", dock_icon_01)
        setIcon("com.google.android.apps.messaging", dock_icon_02)
        setIcon("com.android.chrome", dock_icon_04)
        setIcon("com.google.android.GoogleCamera", dock_icon_05)

    }

    override fun onDestroy() {
        super.onDestroy()
        isContainue = false
        if (this::mHandler.isInitialized) {
            mHandler.removeCallbacks(mRunnable)
        }
    }

    private fun setIcon(pkg_name: String, image_view: ImageView) {
        try {
            val icon: Drawable? = packageManager.getApplicationIcon(pkg_name)
            image_view.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector!!.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true

    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val defVal = 150
        Log.d("fling", "->> fling")
        if (e1!!.getY() - e2!!.getY() > defVal) {
            Log.d("fling", "->> Swipe Up")

            val intent = Intent(this, ShowAllAppsActivity::class.java)
            Log.d(tag, "Launch LauncherActivity Class")
            startActivity(intent)


            return true
        }

        if (e2.getY() - e1.getY() > defVal) {

            Log.d("fling", "->> Swipe Down")
            return true
        }

        if (e1.getX() - e2.getX() > defVal) {

            Log.d("fling", "->> Swipe Left ")

            return true
        }

        if (e2.getX() - e1.getX() > defVal) {

            Log.d("fling", "->> Swipe Right ")

            return true
        } else {

            return true
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {

    }


}