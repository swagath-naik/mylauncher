package com.swagath.mylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_show_all_apps.*
import java.util.*
import kotlin.collections.ArrayList

class ShowAllAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all_apps)

        val pm: PackageManager = getPackageManager()
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList: List<ResolveInfo> =
            pm.queryIntentActivities(mainIntent, 0)

        val applicationList = ArrayList<AppInfoModel>()

        for (p in pkgAppsList) {
            applicationList.add(
                AppInfoModel(
                    p.activityInfo.packageName,
                    p.loadLabel(pm).toString()
                )
            )

        }
        applicationList.sortBy { it.package_label.toLowerCase(Locale.getDefault()) }
        //Log.d("sorted",applicationList[0].package_label)
        all_apps_list.layoutManager = LinearLayoutManager(this)
        val appsAdapter = ShowAllAppsAdapter()
        all_apps_list.adapter = appsAdapter
        appsAdapter.submitList(applicationList)

    }

    override fun onStop() {
        super.onStop()
        Log.d("stop", "I m stopped..")
        finish()
    }
}