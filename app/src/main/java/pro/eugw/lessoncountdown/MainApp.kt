package pro.eugw.lessoncountdown

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager


class MainApp : Application() {

    var running: Boolean = false

    override fun onCreate() {
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                running = p1!!.getBooleanExtra("isRun", false)
            }
        }, IntentFilter(baseContext.packageName + ".SERVICE_STATE"))
        super.onCreate()
    }

}