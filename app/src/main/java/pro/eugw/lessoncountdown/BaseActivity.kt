package pro.eugw.lessoncountdown

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager

open class BaseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        val instance = LocalBroadcastManager.getInstance(this)
        instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                setTheme()
                recreate()
            }
        }, IntentFilter(baseContext.packageName + ".THEME_UPDATE"))
    }

    private fun setTheme() {
        setTheme(if (getSharedPreferences("newPrefs", Context.MODE_PRIVATE).getBoolean("darkTheme", false)) R.style.AppTheme_Dark else R.style.AppTheme)
    }

}