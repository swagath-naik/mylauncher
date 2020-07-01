package com.swagath.mylauncher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
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

    }

    override fun onDestroy() {
        super.onDestroy()
        isContainue = false
        if (this::mHandler.isInitialized) {
            mHandler.removeCallbacks(mRunnable)
        }
    }
}