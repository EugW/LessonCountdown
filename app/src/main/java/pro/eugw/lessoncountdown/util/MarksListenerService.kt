package pro.eugw.lessoncountdown.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import pro.eugw.lessoncountdown.util.network.KundelikMarks
import kotlin.concurrent.thread

class MarksListenerService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        thread(true) {
            while (true) {
                KundelikMarks.check(applicationContext)
                Thread.sleep(prefs.getInt(LOCAL_SERVICE_DELAY, 15).toLong()*1000*60)
            }
        }
    }

}
